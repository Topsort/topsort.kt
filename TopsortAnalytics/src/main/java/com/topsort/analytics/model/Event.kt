package com.topsort.analytics.model

import org.json.JSONArray
import org.json.JSONObject

data class Event(
    val clicks: List<Click>? = null,
    val impressions: List<Impression>? = null,
    val purchases: List<Purchase>? = null,
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()

        if (clicks != null) {
            val array = JSONArray()
            clicks.indices.map {
                array.put(it, clicks[it].toJsonObject())
            }
            json.put("clicks", array)
        }

        if (impressions != null) {
            val array = JSONArray()
            impressions.indices.map {
                array.put(it, impressions[it].toJsonObject())
            }
            json.put("impressions", array)
        }

        if (purchases != null) {
            val array = JSONArray()
            purchases.indices.map {
                array.put(it, purchases[it].toJsonObject())
            }
            json.put("purchases", array)
        }
        return json
    }

    companion object {
        fun fromJson(json: String?): Event? {
            if (json == null) return null
            val jsonObject = JSONObject(json)

            val clicks = if (jsonObject.has("clicks")) {
                val array = JSONObject(json).getJSONArray("clicks")
                val clicks = (0 until array.length()).map {
                    Click.Factory.fromJsonObject(array.getJSONObject(it))
                }

                clicks
            } else null

            val impressions = if (jsonObject.has("impressions")) {
                val array = JSONObject(json).getJSONArray("impressions")
                val impressions = (0 until array.length()).map {
                    Impression.Factory.fromJsonObject(array.getJSONObject(it))
                }

                impressions
            } else null

            val purchases = if (jsonObject.has("purchases")) {
                val array = JSONObject(json).getJSONArray("purchases")
                val purchases = (0 until array.length()).map {
                    Purchase.fromJsonObject(array.getJSONObject(it))
                }

                purchases
            } else null

            return Event(clicks = clicks, impressions = impressions, purchases = purchases)
        }
    }
}
