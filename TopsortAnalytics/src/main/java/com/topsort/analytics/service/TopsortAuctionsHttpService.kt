package com.topsort.analytics.service

import android.util.Log
import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.auctions.ApiConstants
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import java.io.IOException
import androidx.annotation.VisibleForTesting

/**
 * Implementation of AuctionsHttpService that performs real HTTP calls
 */
@VisibleForTesting
object TopsortAuctionsHttpService : AuctionsHttpService {

    private lateinit var httpClient: HttpClient
    
    /**
     * The current service instance for auctions, can be replaced for testing
     */
    @VisibleForTesting
    var serviceInstance: AuctionsHttpService = this

    /**
     * Executes an auction request and returns the response
     */
    override fun runAuctionsSync(auctionRequest: AuctionRequest): AuctionResponse? {
        try {
            val response = executeRunAuctions(auctionRequest)
            if (response.isSuccessful()) {
                return AuctionResponse.fromJson(response.body)
            } else {
                Log.w("TopsortAuctionsHttpService", "Auction message: " + response.message)
                Log.w("TopsortAuctionsHttpService", "Auction response: " + response.body.toString())
                throw AuctionError.HttpError(IOException("HTTP Error: ${response.code} - ${response.message}"))
            }
        } catch (e: AuctionError) {
            throw e
        } catch (e: Exception) {
            throw AuctionError.HttpError(e)
        }
    }

    private fun executeRunAuctions(auctionRequest: AuctionRequest): HttpResponse {
        try {
            if (!this::httpClient.isInitialized) {
                httpClient = HttpClient("${ApiConstants.BASE_API_URL}${ApiConstants.AUCTION_ENDPOINT}")
            }
            val json = auctionRequest.toJsonObject().toString()
            return httpClient.post(json, Cache.token.ifEmpty { null })
        } catch (e: Exception) {
            throw AuctionError.HttpError(e)
        }
    }

    /**
     * Executes an auction request asynchronously, delegating to the current service instance
     */
    override suspend fun runAuctions(request: AuctionRequest): AuctionResponse {
        // For compatibility with the old method signature
        val response = serviceInstance.runAuctionsSync(request)
        return response ?: throw AuctionError.EmptyResponse
    }
    
    /**
     * Sets a mock implementation for testing purposes
     */
    @VisibleForTesting
    fun setMockService(mockService: AuctionsHttpService) {
        serviceInstance = mockService
    }
    
    /**
     * Resets to the default implementation
     */
    @VisibleForTesting
    fun resetToDefaultService() {
        serviceInstance = this
    }
}
