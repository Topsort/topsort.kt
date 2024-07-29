package com.topsort.analytics.model.auctions

import org.json.JSONObject

data class AuctionResponse private constructor(
    val results: List<AuctionResponseItem>,
) {
    companion object {
        fun fromJson(json: String?): AuctionResponse? {
            if (json == null) return null
            val array = JSONObject(json).getJSONArray("results")
            val results = (0 until array.length()).map {
                AuctionResponseItem.fromJsonObject(array.getJSONObject(it))
            }

            return AuctionResponse(
                results = results,
            )
        }
    }

    data class AuctionResponseItem(
        val resultType: String,
        val winners: List<AuctionWinnerItem>,
        val error: Boolean,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionResponseItem {
                val array = json.getJSONArray("winners")
                val winners = (0 until array.length()).map {
                    AuctionWinnerItem.fromJsonObject(array.getJSONObject(it))
                }
                return AuctionResponseItem(
                    resultType = json.getString("resultType"),
                    error = json.getBoolean("error"),
                    winners = winners,
                )
            }
        }
    }

    data class AuctionWinnerItem(
        val rank: Int,
        val type: String,
        val id: String,
        val resolvedBidId: String,
        val asset: Asset? = null,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionWinnerItem {
                return AuctionWinnerItem(
                    rank = json.getInt("rank"),
                    type = json.getString("type"),
                    id = json.getString("id"),
                    resolvedBidId = json.getString("resolvedBidId"),
                    asset = Asset.fromJsonObject(json),
                )
            }
        }
    }

    data class Asset(val url: String) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionResponse.Asset? {
                val asset = json.optJSONObject("asset")
                if (asset == null) {
                    return null
                }
                val url = asset.getString("url")
                return Asset(url = url)
            }
        }
    }
}
