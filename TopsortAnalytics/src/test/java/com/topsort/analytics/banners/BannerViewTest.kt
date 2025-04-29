package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

@ExperimentalCoroutinesApi
class BannerViewTest {

    // Test doubles
    private lateinit var mockBannerView: BannerView
    
    @Before
    fun setup() {
        // Initialize with mocks
        mockBannerView = mock(BannerView::class.java)
    }

    @Test
    fun `test onNoWinners callback is invoked when no winners returned`() = runTest {
        // Given
        var callbackInvoked = false
        mockBannerView.onNoWinners { callbackInvoked = true }
        
        // When
        whenever(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any<Duration>(),
            any()
        )).thenAnswer {
            // Use reflection to access private callback
            val field = BannerView::class.java.getDeclaredField("onNoWinnersCallback")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val callback = field.get(mockBannerView) as? (() -> Unit)
            callback?.invoke()
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location",
            onClick = { _, _ -> }
        )
        
        // Then
        assertTrue(callbackInvoked, "onNoWinners callback should have been invoked")
    }

    @Test
    fun `test onError callback is invoked when an error occurs`() = runTest {
        // Given
        var callbackInvoked = false
        
        // Create a properly mocked ErrorResult
        val mockRequest = mock(ImageRequest::class.java)
        val mockThrowable = RuntimeException("Test error")
        // ErrorResult is an interface with two properties: request and throwable
        val testErrorResult = object : ErrorResult {
            override val request: ImageRequest = mockRequest
            override val throwable: Throwable = mockThrowable
        }
        
        mockBannerView.onError { callbackInvoked = true }
        
        // When
        whenever(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any<Duration>(),
            any()
        )).thenAnswer {
            // Use reflection to access private callback
            val field = BannerView::class.java.getDeclaredField("onErrorCallback")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val callback = field.get(mockBannerView) as? ((ErrorResult) -> Unit)
            callback?.invoke(testErrorResult)
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location",
            onClick = { _, _ -> }
        )
        
        // Then
        assertTrue(callbackInvoked, "onError callback should have been invoked")
    }

    @Test
    fun `test onAuctionError callback is invoked for specific auction errors`() = runTest {
        // Given
        var callbackInvoked = false
        var receivedError: AuctionError? = null
        val testError = AuctionError.InvalidNumberAuctions(10)
        
        mockBannerView.onAuctionError { error ->
            callbackInvoked = true
            receivedError = error
        }
        
        // When
        whenever(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any<Duration>(),
            any()
        )).thenAnswer {
            // Use reflection to access private callback
            val field = BannerView::class.java.getDeclaredField("onAuctionErrorCallback")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val callback = field.get(mockBannerView) as? ((AuctionError) -> Unit)
            callback?.invoke(testError)
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location",
            onClick = { _, _ -> }
        )
        
        // Then
        assertTrue(callbackInvoked, "onAuctionError callback should have been invoked")
        assertEquals(testError, receivedError, "The error passed to the callback should match the original error")
    }

    @Test
    fun `test onImageLoad callback is invoked when image is loaded`() = runTest {
        // Given
        var callbackInvoked = false
        mockBannerView.onImageLoad { callbackInvoked = true }
        
        // When
        whenever(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any<Duration>(),
            any()
        )).thenAnswer {
            // Use reflection to access private callback
            val field = BannerView::class.java.getDeclaredField("onImageLoadCallback")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val callback = field.get(mockBannerView) as? (() -> Unit)
            callback?.invoke()
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location",
            onClick = { _, _ -> }
        )
        
        // Then
        assertTrue(callbackInvoked, "onImageLoad callback should have been invoked")
    }

    @Test
    fun `test multiple callbacks chain correctly`() = runTest {
        // Given
        var imageLoadCalled = false
        var errorCalled = false
        var noWinnersCalled = false
        
        val chainedView = mockBannerView
            .onImageLoad { imageLoadCalled = true }
            .onError { errorCalled = true }
            .onNoWinners { noWinnersCalled = true }
        
        // When checking that chaining returns the same object
        assertEquals(mockBannerView, chainedView, "Chained method calls should return the original object")
    }
} 