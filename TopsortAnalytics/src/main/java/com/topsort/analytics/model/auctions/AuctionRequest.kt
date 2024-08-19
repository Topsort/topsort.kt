package com.topsort.analytics.model.auctions

import org.json.JSONObject

data class AuctionRequest(
    val auctions : List<Auction>
){
    fun toJsonObject(): JSONObject {
        return JSONObject().put("auctions", auctions)
    }
}
