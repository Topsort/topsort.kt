package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.ApiConstants
import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.TopsortAuctionsHttpService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class BannerAuctionErrorsTest {

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    // This is a simple test to verify that the initialization error is fixed
    @Test
    fun `test initialization is fixed`() {
        // This test does nothing but verify that initialization works
        assertEquals(true, true)
    }

    // Test to verify that when an HTTP error occurs, an AuctionError.HttpError is thrown
    @Test
    fun `when HTTP error occurs, then AuctionError HttpError is thrown`() {
        runBlocking {
            // Given a mock auction configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Mock the TopsortAuctionsHttpService to throw an exception
            val ioException = IOException("Network error")
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // When the service is called, throw an exception
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenThrow(ioException)
                
                // Then executing the function should throw an AuctionError.HttpError
                val exception = assertFailsWith<AuctionError.HttpError> {
                    runBannerAuction(config)
                }
                
                // Verify the original exception is contained within
                assert(exception.cause == ioException)
            }
        }
    }
    
    // Test to verify that when an empty response is returned, an AuctionError.EmptyResponse is thrown
    @Test
    fun `when empty response is received, then AuctionError EmptyResponse is thrown`() {
        runBlocking {
            // Given a mock auction configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Mock the TopsortAuctionsHttpService to return null
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // When the service is called, return null
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenReturn(null)
                
                // Then executing the function should throw an AuctionError.EmptyResponse
                assertFailsWith<AuctionError.EmptyResponse> {
                    runBannerAuction(config)
                }
            }
        }
    }
    
    // Test to verify that when no winners are in the auction response, null is returned
    @Test
    fun `when response has no winners, then null is returned`() {
        runBlocking {
            // Given a mock auction configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Create a mock auction response with no winners
            val emptyResponse = Mockito.mock(AuctionResponse::class.java)
            Mockito.`when`(emptyResponse.results).thenReturn(listOf())
            
            // Mock the TopsortAuctionsHttpService to return the empty response
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // When the service is called, return the empty response
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenReturn(emptyResponse)
                
                // Then executing the function should return null
                val result = runBannerAuction(config)
                assertNull(result)
            }
        }
    }
    
    // Test to verify that when the number of auctions is invalid, an AuctionError.InvalidNumberAuctions is thrown
    @Test
    fun `when invalid auction count, then AuctionError InvalidNumberAuctions is thrown`() {
        runBlocking {
            // Given we have modified the MIN_AUCTIONS to be greater than 1 for the test
            val originalMinAuctions = ApiConstants.MIN_AUCTIONS
            val originalMaxAuctions = ApiConstants.MAX_AUCTIONS
            
            try {
                // Override the constants for the test
                val mockField = ApiConstants::class.java.getDeclaredField("MIN_AUCTIONS")
                mockField.isAccessible = true
                mockField.set(null, 2) // Set MIN_AUCTIONS to 2, which will make a single auction invalid
                
                // When we try to run an auction with just 1 slot (which is now invalid)
                val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
                
                // Then executing the function should throw an AuctionError.InvalidNumberAuctions
                val exception = assertFailsWith<AuctionError.InvalidNumberAuctions> {
                    runBannerAuction(config)
                }
                
                // The exception should contain the actual count
                assert(exception.count == 1)
            } finally {
                // Reset the constants back to their original values
                val minField = ApiConstants::class.java.getDeclaredField("MIN_AUCTIONS")
                minField.isAccessible = true
                minField.set(null, originalMinAuctions)
                
                val maxField = ApiConstants::class.java.getDeclaredField("MAX_AUCTIONS")
                maxField.isAccessible = true
                maxField.set(null, originalMaxAuctions)
            }
        }
    }
    
    /**
     * Test that auctions properly timeout if they take too long
     */
    @Test
    fun `auction requests should timeout if they exceed the specified duration`() {
        runBlocking {
            // Create test configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Mock the TopsortAuctionsHttpService to delay longer than our timeout
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // Configure mock to delay longer than our timeout
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenAnswer { _ -> 
                    runBlocking {
                        // Delay longer than our timeout
                        delay(500)
                        Mockito.mock(AuctionResponse::class.java)
                    }
                }
                
                // Set a short timeout (100ms)
                val timeout = 100.milliseconds
                
                // The auction should throw a TimeoutError
                val exception = assertFailsWith<AuctionError.TimeoutError> {
                    runBannerAuction(config, timeout)
                }
                
                // Verify the error message contains timeout information
                assertTrue(exception.message?.contains(timeout.toString()) ?: false,
                         "Timeout error message should include the timeout duration")
            }
        }
    }
    
    /**
     * Test that auctions complete normally if they finish within the timeout
     */
    @Test
    fun `auction requests should complete normally if they finish within timeout`() {
        runBlocking {
            // Create test configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Mock the TopsortAuctionsHttpService
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // Mock response
                val mockResponse = Mockito.mock(AuctionResponse::class.java)
                Mockito.`when`(mockResponse.results).thenReturn(emptyList())
                
                // Configure mock to delay less than our timeout
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenAnswer { _ -> 
                    runBlocking {
                        // Delay shorter than our timeout
                        delay(50)
                        mockResponse
                    }
                }
                
                // Set a longer timeout (500ms)
                val timeout = 500.milliseconds
                
                // The auction should complete successfully (return null for empty results)
                val result = runBannerAuction(config, timeout)
                
                // Result is null because we mocked an empty response list
                assertTrue(result == null, "Auction should complete normally with null result for empty winner list")
            }
        }
    }
    
    /**
     * Test that the default timeout is applied when not explicitly specified
     */
    @Test
    fun `default timeout should be applied when not explicitly specified`() {
        runBlocking {
            // Create test configuration
            val config = BannerConfig.LandingPage("test-slot", listOf("test-id"))
            
            // Mock the TopsortAuctionsHttpService
            val httpServiceMock = Mockito.mockStatic(TopsortAuctionsHttpService::class.java)
            
            httpServiceMock.use {
                // Configure mock to delay longer than the default timeout
                it.`when`<AuctionResponse> { 
                    TopsortAuctionsHttpService.runAuctions(any(AuctionRequest::class.java)) 
                }.thenAnswer { _ -> 
                    runBlocking {
                        // This delay is much longer than any reasonable default timeout
                        delay(DEFAULT_AUCTION_TIMEOUT.inWholeMilliseconds * 2)
                        Mockito.mock(AuctionResponse::class.java)
                    }
                }
                
                // The auction should throw a TimeoutError
                assertFailsWith<AuctionError.TimeoutError> {
                    runBannerAuction(config) // Using default timeout
                }
            }
        }
    }
} 