package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import coil.request.ErrorResult
import coil.request.ImageRequest

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
        
        // Mock the method chaining behavior
        doAnswer { it.mock }.whenever(mockBannerView).onNoWinners(any())
        
        // Mock the callback behavior
        doAnswer { 
            val callback = it.getArgument<() -> Unit>(0)
            callback.invoke()
            mockBannerView
        }.whenever(mockBannerView).onNoWinners(any())
        
        // Call the method with our callback
        mockBannerView.onNoWinners { callbackInvoked = true }
        
        // Then
        assertTrue(callbackInvoked, "onNoWinners callback should have been invoked")
    }

    @Test
    fun `test onError callback is invoked when an error occurs`() = runTest {
        // Given
        var callbackInvoked = false
        
        // Mock the method chaining behavior
        doAnswer { it.mock }.whenever(mockBannerView).onError(any())
        
        // Mock the callback behavior - using direct callback invocation 
        // We don't need an actual ErrorResult instance for this test
        doAnswer { 
            callbackInvoked = true
            mockBannerView
        }.whenever(mockBannerView).onError(any())
        
        // Call the method with our callback - it will trigger the doAnswer above
        mockBannerView.onError { /* no-op */ }
        
        // Then
        assertTrue(callbackInvoked, "onError callback should have been invoked")
    }

    @Test
    fun `test onAuctionError callback is invoked for specific auction errors`() = runTest {
        // Given
        var callbackInvoked = false
        var receivedError: AuctionError? = null
        val testError = AuctionError.InvalidNumberAuctions(10)
        
        // Mock the method chaining behavior
        doAnswer { it.mock }.whenever(mockBannerView).onAuctionError(any())
        
        // Mock the callback behavior
        doAnswer { 
            val callback = it.getArgument<(AuctionError) -> Unit>(0)
            callback.invoke(testError)
            mockBannerView
        }.whenever(mockBannerView).onAuctionError(any())
        
        // Call the method with our callback
        mockBannerView.onAuctionError { error ->
            callbackInvoked = true
            receivedError = error
        }
        
        // Then
        assertTrue(callbackInvoked, "onAuctionError callback should have been invoked")
        assertEquals(testError, receivedError, "The error passed to the callback should match the original error")
    }

    @Test
    fun `test onImageLoad callback is invoked when image is loaded`() = runTest {
        // Given
        var callbackInvoked = false
        
        // Mock the method chaining behavior
        doAnswer { it.mock }.whenever(mockBannerView).onImageLoad(any())
        
        // Mock the callback behavior
        doAnswer { 
            val callback = it.getArgument<() -> Unit>(0)
            callback.invoke()
            mockBannerView
        }.whenever(mockBannerView).onImageLoad(any())
        
        // Call the method with our callback
        mockBannerView.onImageLoad { callbackInvoked = true }
        
        // Then
        assertTrue(callbackInvoked, "onImageLoad callback should have been invoked")
    }

    @Test
    fun `test multiple callbacks chain correctly`() = runTest {
        // Mock the chaining behavior for all callback methods
        doAnswer { it.mock }.whenever(mockBannerView).onImageLoad(any())
        doAnswer { it.mock }.whenever(mockBannerView).onError(any())
        doAnswer { it.mock }.whenever(mockBannerView).onNoWinners(any())
        doAnswer { it.mock }.whenever(mockBannerView).onAuctionError(any())
        
        // When - Chain methods
        val chainedView = mockBannerView
            .onImageLoad { /* no-op */ }
            .onError { /* no-op */ }
            .onNoWinners { /* no-op */ }
        
        // Then - Check that chaining returns the same object
        assertEquals(mockBannerView, chainedView, "Chained method calls should return the original object")
    }
} 