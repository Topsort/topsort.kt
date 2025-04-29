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
import org.mockito.Mockito.thenAnswer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        `when`(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any()
        )).thenAnswer {
            // Simulate no winners
            val callback = mockBannerView.onNoWinnersCallback
            callback?.invoke()
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location"
        ) { _, _ -> }
        
        // Then
        assertTrue(callbackInvoked, "onNoWinners callback should have been invoked")
    }

    @Test
    fun `test onError callback is invoked when an error occurs`() = runTest {
        // Given
        var callbackInvoked = false
        val testException = Exception("Test exception")
        mockBannerView.onError { callbackInvoked = true }
        
        // When
        `when`(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any()
        )).thenAnswer {
            // Simulate error
            val callback = mockBannerView.onErrorCallback
            callback?.invoke(testException)
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location"
        ) { _, _ -> }
        
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
        `when`(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any()
        )).thenAnswer {
            // Simulate auction error
            val callback = mockBannerView.onAuctionErrorCallback
            callback?.invoke(testError)
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location"
        ) { _, _ -> }
        
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
        `when`(mockBannerView.setup(
            any(),
            anyString(),
            anyString(),
            any()
        )).thenAnswer {
            // Simulate successful image load
            val callback = mockBannerView.onImageLoadCallback
            callback?.invoke()
            Unit
        }
        
        // Simulate setup call
        mockBannerView.setup(
            BannerConfig.LandingPage("test-slot", listOf("id1", "id2")),
            "test-path",
            "test-location"
        ) { _, _ -> }
        
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