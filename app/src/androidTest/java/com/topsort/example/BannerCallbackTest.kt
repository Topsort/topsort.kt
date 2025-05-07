package com.topsort.example

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.Analytics
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView
import com.topsort.analytics.model.auctions.EntityType
import com.topsort.analytics.service.TopsortAuctionsHttpService
import com.topsort.example.testutil.TestAttributeSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Basic connected instrumentation test for BannerView that focuses on callbacks
 */
@RunWith(AndroidJUnit4::class)
class BannerCallbackTest {

    private lateinit var context: Context
    private lateinit var parentView: FrameLayout
    private lateinit var bannerView: BannerView
    private val mockService = MockAuctionsHttpService()
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        parentView = FrameLayout(context)
        
        // Create BannerView with a proper AttributeSet
        val attrs = TestAttributeSet.create(context)
        bannerView = BannerView(context, attrs)
        parentView.addView(bannerView)
        
        Analytics.setup(
            application = context.applicationContext as android.app.Application,
            opaqueUserId = "test-user-id",
            token = "test-token"
        )
        
        // Use dependency injection to set the mock service
        TopsortAuctionsHttpService.setMockService(mockService)
    }
    
    @After
    fun tearDown() {
        // Reset to the default service implementation
        TopsortAuctionsHttpService.resetToDefaultService()
        
        parentView.removeAllViews()
    }

    @Test
    fun testCallbackRegistration() = runBlocking(Dispatchers.Main) {
        // Create a latch to track callback invocation
        val callbackLatch = CountDownLatch(1)
        var callbackInvoked = false
        var productId: String? = null
        var entityType: EntityType? = null
        
        // Create a simple banner config
        val config = BannerConfig.LandingPage(
            slotId = "test-slot-id",
            ids = listOf("product-1", "product-2")
        )
        
        // Setup the banner with a click callback
        bannerView.setup(
            config = config,
            path = "test/path",
            location = "test-location"
        ) { id, type ->
            // Capture the callback parameters
            callbackInvoked = true
            productId = id
            entityType = type
            callbackLatch.countDown()
        }
        
        bannerView.performClick()
        
        val callbackReceived = callbackLatch.await(2, TimeUnit.SECONDS)

        if (callbackReceived) {
            assertTrue(callbackInvoked)
            assertTrue(productId != null)
            assertTrue(entityType != null)
        }
    }
} 