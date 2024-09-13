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
        val type: EntityType,
        val id: String,
        val resolvedBidId: String,
        val asset: List<Asset>? = null,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionWinnerItem {
                val assetArray = json.optJSONArray("asset")
                var assets: List<Asset>? = null
                if (assetArray != null) {
                    assets = (0 until assetArray.length()).map {
                        Asset.fromJsonObject(assetArray.getJSONObject(it))
                    }
                }
                return AuctionWinnerItem(
                    rank = json.getInt("rank"),
                    type = EntityType.fromValue(json.getString("type")),
                    id = json.getString("id"),
                    resolvedBidId = json.getString("resolvedBidId"),
                    asset = assets,
                )
            }
        }
    }


    data class Asset(val url: String) {
        companion object {
            fun fromJsonObject(json: JSONObject): Asset {
                val url = json.getString("url")
                return Asset(url = url)
            }
        }
    }
}

