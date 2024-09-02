package com.topsort.analytics.model.auctions

import org.json.JSONArray
import org.json.JSONObject

data class AuctionRequest(
    val auctions : List<Auction>
){
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        auctions.indices.map {
            array.put(it, auctions[it].toJsonObject())
        }
        return JSONObject().put("auctions", array)
    }
}
