package com.topsort.analytics.service

import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse

/**
 * Interface for the auctions service to enable dependency injection and testing
 */
interface AuctionsHttpService {
    /**
     * Executes an auction request and returns the response, blocking the calling thread. Never
     * call this from the main thread; prefer [runAuctions] from a coroutine.
     * 
     * @param request The auction request to execute
     * @return The auction response, or null if the request failed
     */
    fun runAuctionsSync(request: AuctionRequest): AuctionResponse?
    
    /**
     * Executes an auction request and returns the response. Implementations must run the request
     * off the caller's thread, so that this is safe to call from any dispatcher.
     * 
     * @param request The auction request to execute
     * @return The auction response
     */
    suspend fun runAuctions(request: AuctionRequest): AuctionResponse
} 