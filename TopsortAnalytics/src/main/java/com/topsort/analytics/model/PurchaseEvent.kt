package com.topsort.analytics.model

import androidx.annotation.IntRange
import org.json.JSONObject

data class PurchaseEvent(
    private val eventType: EventType = EventType.Purchase,
    val purchases: List<Purchase>
)

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
) {
    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .put("items", items)
    }

    companion object{
        fun fromJsonObject(json : JSONObject) : Purchase{
            return Purchase(
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                items = listOf(),
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
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PurchasedItem {
            return PurchasedItem(
                productId = json.getString("productId"),
                quantity = json.getInt("quantity"),
                unitPrice = json.getInt("unitPrice"),
            )
        }
    }

}

internal data class PurchaseEventResponse(
    val purchaseId: String,
    val id: String
)
