package com.topsort.analytics.model.auctions

import com.topsort.analytics.core.getStringOrNull
import org.json.JSONObject

/**
 * Response from the Topsort auction API containing the results of one or more auctions.
 *
 * @property results List of auction results, one per auction in the request.
 */
data class AuctionResponse private constructor(
    val results: List<AuctionResponseItem>,
) {
    companion object {
        /**
         * Parses an [AuctionResponse] from a JSON string.
         *
         * @param json The JSON string to parse.
         * @return The parsed [AuctionResponse].
         * @throws AuctionError.EmptyResponse if [json] is null.
         * @throws AuctionError.DeserializationError if the JSON is malformed or missing required fields.
         */
        fun fromJson(json: String?): AuctionResponse {
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

                return AuctionResponse(results = results)
            } catch (e: AuctionError) {
                throw e
            } catch (e: Exception) {
                throw AuctionError.DeserializationError(e, json.toByteArray())
            }
        }
    }

    /**
     * A single auction result containing the winning bids.
     *
     * @property resultType The type of auction result (e.g., "listings", "banners").
     * @property winners List of winning bids for this auction, ordered by rank.
     * @property error Whether an error occurred processing this auction.
     */
    data class AuctionResponseItem(
        val resultType: String,
        val winners: List<AuctionWinnerItem>,
        val error: Boolean,
    ) {
        companion object {
            /**
             * Parses an [AuctionResponseItem] from a [JSONObject].
             *
             * @throws AuctionError.DeserializationError if required fields are missing.
             */
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

    /**
     * A winning bid in an auction result.
     *
     * @property rank The rank of this winner (1 = highest).
     * @property type The entity type of the winner (product, vendor, etc.).
     * @property id The marketplace ID of the winning entity.
     * @property resolvedBidId The unique identifier for this winning bid, used for attribution.
     * @property asset Optional list of creative assets associated with this winner.
     * @property campaignId The campaign ID associated with this winning bid.
     */
    data class AuctionWinnerItem(
        val rank: Int,
        val type: EntityType,
        val id: String,
        val resolvedBidId: String,
        val asset: List<Asset>? = null,
        val campaignId: String? = null,
    ) {
        companion object {
            /**
             * Parses an [AuctionWinnerItem] from a [JSONObject].
             *
             * @throws AuctionError.DeserializationError if required fields are missing or invalid.
             */
            fun fromJsonObject(json: JSONObject): AuctionWinnerItem {
                try {
                    val assetArray = json.optJSONArray("asset")
                    val assets = assetArray?.let { array ->
                        (0 until array.length()).map {
                            Asset.fromJsonObject(array.getJSONObject(it))
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

    /**
     * A creative asset associated with a winning bid.
     *
     * @property url The URL of the asset (e.g., image, video).
     * @property content Optional content associated with the asset (e.g., HTML or text content).
     */
    data class Asset(
        val url: String,
        val content: String? = null,
    ) {
        companion object {
            /**
             * Parses an [Asset] from a [JSONObject].
             *
             * @throws AuctionError.DeserializationError if the URL field is missing.
             */
            fun fromJsonObject(json: JSONObject): Asset {
                try {
                    return Asset(
                        url = json.getString("url"),
                        content = json.getStringOrNull("content"),
                    )
                } catch (e: Exception) {
                    throw AuctionError.DeserializationError(e, json.toString().toByteArray())
                }
            }
        }
    }
}

