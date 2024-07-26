package com.topsort.analytics.model

import androidx.annotation.IntRange
import com.topsort.analytics.core.getIntOrNull
import com.topsort.analytics.core.getStringOrNull
import org.json.JSONArray

import org.json.JSONObject

data class PurchaseEvent(
    val purchases: List<Purchase>
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().put("purchases", purchases)
    }

    companion object{
        fun fromJson(json : String?) : PurchaseEvent? {
            if(json == null) return null
            val array = JSONObject(json).getJSONArray("purchases")
            val purchases = (0 until array.length()).map {
                Purchase.fromJsonObject(array.getJSONObject(it))
            }

            return PurchaseEvent(purchases = purchases)
        }
    }
}

data class Purchase(

    /**
     * RFC3339 formatted timestamp including UTC offset.
     */
    val occurredAt: String,

    /**
     * The opaque user ID which allows correlating user activity.
     */
    val opaqueUserId: String,

    /**
     * The marketplace assigned ID for the order
     */
    val id: String,

    /**
     * Items purchased
     */
    val items: List<PurchasedItem>,
)  : JsonSerializable {
    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .put("items", JSONArray(items.map { it.toJsonObject() }))
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Purchase {
            val itemsArray = json.getJSONArray("items")
            return Purchase(
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                items = (0 until itemsArray.length()).map {
                    PurchasedItem.fromJsonObject(itemsArray.getJSONObject(it))
                },
            )
        }
    }
}

data class PurchasedItem(

    /**
     * The marketplace ID of the product being purchased
     */
    val productId: String,

    @IntRange(from = 1) val quantity: Int,
    @IntRange(from = 1) val unitPrice: Int? = null,

    /**
     * If known, the product's auction ID if the consumer clicked on a promoted link before purchasing
     */
    val resolvedBidId: String? = null
) {

    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("productId", productId)
            .put("quantity", quantity)
            .put("unitPrice", unitPrice)
            .put("resolvedBidId", resolvedBidId)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PurchasedItem {
            return PurchasedItem(
                productId = json.getString("productId"),
                quantity = json.getInt("quantity"),
                unitPrice = json.getIntOrNull("unitPrice"),
                resolvedBidId = json.getStringOrNull("resolvedBidId"),
            )
        }
    }
}

internal data class PurchaseEventResponse(
    val purchaseId: String,
    val id: String
)
