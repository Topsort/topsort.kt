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

internal object TopsortAuctionsHttpService {

    private lateinit var httpClient: HttpClient

    fun runAuctions(auctionRequest: AuctionRequest): AuctionResponse? {
        try {
            val response = executeRunAuctions(auctionRequest)
            if (response.isSuccessful()) {
                return AuctionResponse.fromJson(response.body)
            } else {
                Log.w("TopsortAuctionsHttpService", "Auction message: " + response.message)
                Log.w("TopsortAuctionsHttpService", "Auction response: " + response.body.toString())
                throw AuctionError.HttpError(IOException("HTTP Error: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            if (e is AuctionError) {
                throw e
            }
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
}
