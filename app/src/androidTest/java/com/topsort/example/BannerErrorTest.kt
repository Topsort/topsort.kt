package com.topsort.example

import android.content.Context
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.Analytics
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.EntityType
import com.topsort.analytics.service.TopsortAuctionsHttpService
import com.topsort.example.testutil.TestAttributeSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BannerErrorTest {

    private lateinit var context: Context
    private lateinit var parentView: FrameLayout
    private lateinit var bannerView: BannerView
    private val errorMockService = MockErrorAuctionsHttpService()
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        parentView = FrameLayout(context)
        
        val attrs = TestAttributeSet.create(context)
        bannerView = BannerView(context, attrs)
        parentView.addView(bannerView)
        
        Analytics.setup(
            application = context.applicationContext as android.app.Application,
            opaqueUserId = "test-user-id",
            token = "invalid-token"
        )
        
        // Use dependency injection to set the mock service
        TopsortAuctionsHttpService.setMockService(errorMockService)
    }
    
    @After
    fun tearDown() {
        // Reset to the default service implementation
        TopsortAuctionsHttpService.resetToDefaultService()
        
        parentView.removeAllViews()
    }

    @Test
    fun testErrorHandling() = runBlocking {
        // Create a latch to wait for the error
        val errorLatch = CountDownLatch(1)
        
        // Create a configuration with invalid data to trigger an error
        val config = BannerConfig.LandingPage(
            slotId = "invalid-slot-id",
            ids = listOf("product-1")
        )
        
        bannerView.onError { throwable: Throwable ->
            // Count down the latch when an error is received
            errorLatch.countDown()
        }
        
        bannerView.setup(
            config = config,
            path = "test/path",
            location = "test-location"
        ) { _: String, _: EntityType ->
            // This click handler should not be called in an error scenario
        }
        
        // Wait for the error callback to be triggered
        val errorReceived = errorLatch.await(5, TimeUnit.SECONDS)
        
        // Verify that the error callback was triggered
        assertTrue("Error should be detected", errorReceived)
    }
} 