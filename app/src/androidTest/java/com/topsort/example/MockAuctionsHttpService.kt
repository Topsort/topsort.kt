package com.topsort.example

import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.model.auctions.EntityType
import com.topsort.analytics.service.AuctionsHttpService
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mock implementation of the AuctionsHttpService for testing purposes.
 * Returns predefined responses instead of making real HTTP calls.
 */
class MockAuctionsHttpService : AuctionsHttpService {
    
    /**
     * Standard version that matches the main synchronous method signature
     */
    override fun runAuctionsSync(request: AuctionRequest): AuctionResponse? {
        return createMockResponse()
    }
    
    /**
     * Suspend version that matches the suspend method signature in TopsortAuctionsHttpService
     */
    override suspend fun runAuctions(request: AuctionRequest): AuctionResponse {
        return createMockResponse()
    }
    
    /**
     * Creates a mock auction response with a single result and winner
     */
    private fun createMockResponse(): AuctionResponse {
        // Create a mock response JSON structure
        val responseJson = JSONObject().apply {
            put("results", JSONArray().apply {
                put(JSONObject().apply {
                    put("winners", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", "product-1")
                            put("type", EntityType.PRODUCT.name)
                            put("resolvedBidId", "mock-bid-123456")
                            put("asset", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("url", "https://example.com/test-banner.jpg")
                                    put("width", 300)
                                    put("height", 250)
                                })
                            })
                        })
                    })
                })
            })
        }
        
        // Return the mock response
        return AuctionResponse.fromJson(responseJson.toString())
    }
} 