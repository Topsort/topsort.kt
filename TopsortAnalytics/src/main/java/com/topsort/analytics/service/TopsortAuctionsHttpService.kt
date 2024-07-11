package com.topsort.analytics.service

import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.core.ServiceSettings
import com.topsort.analytics.core.ServiceSettings.baseApiUrl
import com.topsort.analytics.core.ServiceSettings.bearerToken
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import org.json.JSONObject

private const val AUCTION_ENDPOINT = "/v2/auctions"

internal object TopsortAuctionsHttpService {

    private lateinit var httpClient: HttpClient

    fun runAuctions(auctionRequest: AuctionRequest): AuctionResponse? {
        val response = executeRunAuctions(auctionRequest)
        if (response.isSuccessful()) {
            return AuctionResponse.fromJson(response.body)
        }
        return null
    }

    private fun executeRunAuctions(auctionRequest: AuctionRequest): HttpResponse {
        if(!this::httpClient.isInitialized){
            assert(ServiceSettings.isSetup())
            httpClient = HttpClient("${baseApiUrl}${AUCTION_ENDPOINT}")
        }
        val json = JSONObject.wrap(auctionRequest)!!.toString()
        return httpClient.post(json, bearerToken)
    }
}
