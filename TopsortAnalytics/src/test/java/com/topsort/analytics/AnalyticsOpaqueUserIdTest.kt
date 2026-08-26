package com.topsort.analytics

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The public [Analytics.opaqueUserId] getter.
 *
 * It exists so an integrator can read back the id their events are actually reported under, which
 * is not always the one they passed to [Analytics.setup]: a blank argument falls back to a stored
 * id, or to a generated placeholder when there is nothing to fall back on. A placeholder will not
 * audience-match, so being able to detect that one is in effect is the point of the getter.
 *
 * These tests pin the contract at the Analytics boundary, stubbing [Cache] so the resolved id is
 * controlled. The resolution rules themselves need the real encrypted store and are covered by the
 * instrumented tests.
 */
class AnalyticsOpaqueUserIdTest {

    private val application = mockk<Application>()

    @Before
    fun setUp() {
        every { application.applicationContext } returns mockk<Context>()
        // getInstance is a companion method in work 2.11.2, not a Java static, so mockkStatic
        // would not intercept it - the real WorkManagerImpl would then call getApplicationContext
        // on a stubbed android.jar Context and blow up.
        mockkObject(WorkManager.Companion)
        // Relaxed: setup() schedules work, and which work it schedules is not this test's business.
        // A strict mock would make these tests fail whenever setup() gains another enqueue.
        every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
        mockkObject(Cache)
        resetAnalytics()
    }

    @After
    fun tearDown() {
        resetAnalytics()
        unmockkAll()
    }

    /**
     * [Analytics] is an object, so what one test leaves behind is what the next one starts from.
     * Reset it directly: otherwise whether "null before setup" holds would depend on execution
     * order, and the mocked WorkManager would outlive this class. Renaming a field makes this throw
     * rather than quietly stop resetting.
     */
    private fun resetAnalytics() {
        listOf("session", "applicationContext", "workManager").forEach { name ->
            Analytics::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(Analytics, null)
            }
        }
    }

    @Test
    fun `opaqueUserId is null before setup runs`() {
        assertThat(Analytics.opaqueUserId).isNull()
    }

    @Test
    fun `opaqueUserId reports the resolved id, not the blank one that was passed in`() {
        every { Cache.setup(any(), any(), any()) } returns "id-from-the-cache"

        Analytics.setup(application, "", "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("id-from-the-cache")
    }

    @Test
    fun `opaqueUserId reports the supplied id when it is the one in effect`() {
        every { Cache.setup(any(), any(), any()) } returns "marketplace-id"

        Analytics.setup(application, "marketplace-id", "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    /**
     * The identity overload is the supported entry point, so its contract is pinned here and not
     * only in the instrumented tests - the deprecated String overload delegates to it, so without
     * these the new public API would be exercised by no unit test at all.
     */
    @Test
    fun `opaqueUserId reports the resolved id for a marketplace identity`() {
        every { Cache.setup(any(), any(), any()) } returns "marketplace-id"

        Analytics.setup(application, UserIdentity.of("marketplace-id"), "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    @Test
    fun `opaqueUserId reports the minted id for an unidentified user`() {
        every { Cache.setup(any(), any(), any()) } returns "generated-placeholder"

        Analytics.setup(application, UserIdentity.Unidentified, "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("generated-placeholder")
    }

    /** The deprecated overload must reach the same place, so blank maps to Unidentified. */
    @Test
    fun `the deprecated overload routes a blank id through the identity overload`() {
        every { Cache.setup(any(), UserIdentity.Unidentified, any()) } returns "generated-placeholder"

        @Suppress("DEPRECATION")
        Analytics.setup(application, "", "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("generated-placeholder")
    }

    @Test
    fun `calling setup again with a real id replaces a placeholder`() {
        every { Cache.setup(any(), any(), any()) } returns "generated-placeholder"
        Analytics.setup(application, "", "token")

        every { Cache.setup(any(), any(), any()) } returns "marketplace-id"
        Analytics.setup(application, "marketplace-id", "token")

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }
}
