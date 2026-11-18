package com.topsort.analytics

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.topsort.analytics.model.PurchasedItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [Analytics.reportPurchase] must drop a purchase the API would reject with a 400 instead of
 * caching it, and must tell the host why. The validation rules themselves are covered by
 * PurchaseValidationTest; this pins what happens at the public boundary.
 */
class AnalyticsReportPurchaseTest {

    private val application = mockk<Application>()
    private val listener = mockk<EventDiscardListener>(relaxed = true)

    @Before
    fun setUp() {
        every { application.applicationContext } returns mockk<Context>()
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
        mockkObject(Cache)
        every { Cache.setup(any(), any(), any()) } returns "user-1"
        every { Cache.storePurchase(any()) } returns 1L
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        Analytics.setup(application, UserIdentity.of("user-1"), "token")
        Analytics.eventDiscardListener = listener
    }

    @After
    fun tearDown() {
        Analytics.eventDiscardListener = null
        listOf("session", "applicationContext", "workManager").forEach { name ->
            Analytics::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(Analytics, null)
            }
        }
        unmockkAll()
    }

    @Test
    fun `a valid purchase is cached`() {
        Analytics.reportPurchase(
            items = listOf(PurchasedItem(productId = "p1", quantity = 1, unitPrice = 25)),
            id = "order-1",
        )

        verify(exactly = 1) { Cache.storePurchase(any()) }
        verify(exactly = 0) { listener.onEventsDiscarded(any(), any()) }
    }

    @Test
    fun `a purchase with an invalid item is not cached and is reported as invalid`() {
        Analytics.reportPurchase(
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1, unitPrice = 25),
                PurchasedItem(productId = "p2", quantity = 0, unitPrice = 25),
            ),
            id = "order-1",
        )

        verify(exactly = 0) { Cache.storePurchase(any()) }
        verify(exactly = 1) { listener.onEventsDiscarded(DiscardReason.INVALID_EVENT, 1) }
        verify {
            Log.e(any<String>(), match { it.contains("order-1") && it.contains("items[1] (p2): quantity") })
        }
    }

    @Test
    fun `a purchase with a missing unitPrice is not cached`() {
        Analytics.reportPurchase(
            items = listOf(PurchasedItem(productId = "p1", quantity = 1)),
            id = "order-1",
        )

        verify(exactly = 0) { Cache.storePurchase(any()) }
        verify(exactly = 1) { listener.onEventsDiscarded(DiscardReason.INVALID_EVENT, 1) }
    }
}
