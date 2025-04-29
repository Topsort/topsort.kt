package com.topsort.analytics.model.auctions

import org.json.JSONArray
import org.json.JSONObject

data class AuctionRequest(
    val auctions : List<Auction>
){
    init {
        if (auctions.isEmpty() || auctions.size < ApiConstants.MIN_AUCTIONS) {
            throw AuctionError.InvalidNumberAuctions(auctions.size)
        }
        if (auctions.size > ApiConstants.MAX_AUCTIONS) {
            throw AuctionError.InvalidNumberAuctions(auctions.size)
        }
    }
    
    fun toJsonObject(): JSONObject {
        try {
            val array = JSONArray()
            auctions.indices.map {
                array.put(it, auctions[it].toJsonObject())
            }
            return JSONObject().put("auctions", array)
        } catch (e: Exception) {
            throw AuctionError.SerializationError
        }
    }
}
