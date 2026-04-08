package com.topsort.analytics.model.auctions

import com.topsort.analytics.core.getStringOrNull
import org.json.JSONObject

data class AuctionResponse private constructor(
    val results: List<AuctionResponseItem>,
) {
    companion object {
        fun fromJson(json: String?): AuctionResponse? {
            if (json == null) {
                throw AuctionError.EmptyResponse
            }
            
            try {
                val jsonObject = JSONObject(json)
                if (!jsonObject.has("results")) {
                    throw AuctionError.DeserializationError(
                        IllegalArgumentException("Missing 'results' field"),
                        json.toByteArray()
                    )
                }
                
                val array = jsonObject.getJSONArray("results")
                val results = (0 until array.length()).map {
                    AuctionResponseItem.fromJsonObject(array.getJSONObject(it))
                }

                return AuctionResponse(
                    results = results,
                )
            } catch (e: AuctionError) {
                throw e
            } catch (e: Exception) {
                throw AuctionError.DeserializationError(e, json.toByteArray())
            }
        }
    }

    data class AuctionResponseItem(
        val resultType: String,
        val winners: List<AuctionWinnerItem>,
        val error: Boolean,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionResponseItem {
                try {
                    if (!json.has("winners")) {
                        throw AuctionError.DeserializationError(
                            IllegalArgumentException("Missing 'winners' field"),
                            json.toString().toByteArray()
                        )
                    }
                    
                    val array = json.getJSONArray("winners")
                    val winners = (0 until array.length()).map {
                        AuctionWinnerItem.fromJsonObject(array.getJSONObject(it))
                    }
                    return AuctionResponseItem(
                        resultType = json.getString("resultType"),
                        error = json.getBoolean("error"),
                        winners = winners,
                    )
                } catch (e: AuctionError) {
                    throw e
                } catch (e: Exception) {
                    throw AuctionError.DeserializationError(e, json.toString().toByteArray())
                }
            }
        }
    }

    data class AuctionWinnerItem(
        val rank: Int,
        val type: EntityType,
        val id: String,
        val resolvedBidId: String,
        val asset: List<Asset>? = null,
        /**
         * The campaign ID associated with this winner.
         */
        val campaignId: String? = null,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): AuctionWinnerItem {
                try {
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
                        campaignId = json.getStringOrNull("campaignId"),
                    )
                } catch (e: AuctionError) {
                    throw e
                } catch (e: Exception) {
                    throw AuctionError.DeserializationError(e, json.toString().toByteArray())
                }
            }
        }
    }

    data class Asset(
        val url: String,
        /**
         * Content map for banner templates.
         * Contains key-value pairs for template customization.
         */
        val content: Map<String, String>? = null,
    ) {
        companion object {
            fun fromJsonObject(json: JSONObject): Asset {
                try {
                    val url = json.getString("url")
                    val content = parseContentMap(json.optJSONObject("content"))
                    return Asset(url = url, content = content)
                } catch (e: Exception) {
                    throw AuctionError.DeserializationError(e, json.toString().toByteArray())
                }
            }

            private fun parseContentMap(contentObj: JSONObject?): Map<String, String>? {
                if (contentObj == null) return null
                val map = mutableMapOf<String, String>()
                contentObj.keys().forEach { key ->
                    if (!contentObj.isNull(key)) {
                        map[key] = contentObj.getString(key)
                    }
                }
                return map.toMap()
            }
        }
    }
}

