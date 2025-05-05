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
        return createMockResponse() ?: throw RuntimeException("Failed to create mock response")
    }
    
    /**
     * Creates a mock auction response with a single result and winner
     */
    private fun createMockResponse(): AuctionResponse? {
        try {
            // Create a mock response JSON structure that matches the expected format
            val responseJson = JSONObject().apply {
                put("results", JSONArray().apply {
                    put(JSONObject().apply {
                        put("resultType", "banner")
                        put("error", false)
                        put("winners", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", "product-1")
                                put("type", "url") 
                                put("rank", 1)
                                put("resolvedBidId", "mock-bid-123456")
                                put("asset", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("url", "https://example.com/test-banner.jpg")
                                    })
                                })
                            })
                        })
                    })
                })
            }
            
            // Parse and return the response
            return AuctionResponse.fromJson(responseJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
} 