package com.topsort.example

import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.AuctionsHttpService
import java.io.IOException

/**
 * Mock implementation of AuctionsHttpService that always throws errors for testing error handling
 */
class MockErrorAuctionsHttpService : AuctionsHttpService {
    
    /**
     * Always throws an HTTP error for testing error handling
     */
    override fun runAuctionsSync(request: AuctionRequest): AuctionResponse? {
        throw AuctionError.HttpError(
            IOException("HTTP Error: 401 - Unauthorized (Mock for testing)")
        )
    }
    
    /**
     * Always throws an HTTP error for testing error handling (suspend version)
     */
    override suspend fun runAuctions(request: AuctionRequest): AuctionResponse {
        throw AuctionError.HttpError(
            IOException("HTTP Error: 401 - Unauthorized (Mock for testing)")
        )
    }
} 