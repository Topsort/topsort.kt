package com.topsort.analytics.service

import android.util.Log
import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.core.ServiceSettings.baseApiUrl
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse

private const val AUCTION_ENDPOINT = "/v2/auctions"

internal object TopsortAuctionsHttpService {

    private lateinit var httpClient: HttpClient

    fun runAuctions(auctionRequest: AuctionRequest): AuctionResponse? {
        val response = executeRunAuctions(auctionRequest)
        if (response.isSuccessful()) {
            return AuctionResponse.fromJson(response.body)
        } else {
            Log.w("TopsortAuctionsHttpService", "Auction message: " + response.message)
            Log.w("TopsortAuctionsHttpService", "Auction response: " + response.body.toString())
        }
        return null
    }

    private fun executeRunAuctions(auctionRequest: AuctionRequest): HttpResponse {
        if (!this::httpClient.isInitialized) {
            httpClient = HttpClient("${baseApiUrl}${AUCTION_ENDPOINT}")
        }
        val json = auctionRequest.toJsonObject().toString()
        return httpClient.post(json, Cache.token.ifEmpty { null })
    }
}
