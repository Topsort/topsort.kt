package com.topsort.analytics.service

import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse

/**
 * Interface for the auctions service to enable dependency injection and testing
 */
interface AuctionsHttpService {
    /**
     * Executes an auction request and returns the response.
     * 
     * @param request The auction request to execute
     * @return The auction response, or null if the request failed
     */
    fun runAuctionsSync(request: AuctionRequest): AuctionResponse?
    
    /**
     * Executes an auction request asynchronously and returns the response.
     * 
     * @param request The auction request to execute
     * @return The auction response
     */
    suspend fun runAuctions(request: AuctionRequest): AuctionResponse
} 