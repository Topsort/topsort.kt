package com.topsort.analytics.model

import androidx.annotation.IntRange
import com.topsort.analytics.core.getIntOrNull
import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.auctions.Device
import org.json.JSONArray

import org.json.JSONObject

data class PurchaseEvent(
    val purchases: List<Purchase>
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        purchases.indices.map {
            array.put(it, purchases[it].toJsonObject())
        }
        return JSONObject().put("purchases", array)
    }

    companion object{
        fun fromJson(json : String?) : PurchaseEvent? {
            if(json == null) return null
            val array = JSONObject(json).getJSONArray("purchases")
            val purchases = Purchase.fromJsonArray(array)

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

    /**
     * The device type where the purchase occurred.
     */
    val deviceType: Device? = null,

    /**
     * The channel where the purchase occurred.
     */
    val channel: Channel? = null,

    /**
     * Not sent. The events API has no page on a purchase, so the SDK does not serialize it.
     */
    @Deprecated("Purchases have no page in the events API, so this is not sent.")
    val page: Page? = null,
)  : JsonSerializable {
    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .put("items", JSONArray(items.map { it.toJsonObject() }))
            .apply {
                deviceType?.let { put("deviceType", it.value) }
                channel?.let { put("channel", it.value) }
            }
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
                deviceType = Device.fromValue(json.getStringOrNull("deviceType")),
                channel = Channel.fromValue(json.getStringOrNull("channel")),
            )
        }

        fun fromJsonArray(array: JSONArray): List<Purchase> =
            getListFromJsonArray(
                array
            ) {
                fromJsonObject(it)
            }
    }
}

data class PurchasedItem(

    /**
     * The marketplace ID of the product being purchased
     */
    val productId: String,

    /**
     * Units purchased, at least 1. A purchase with any item below 1 is not reported.
     */
    @IntRange(from = 1) val quantity: Int,

    /**
     * Price of a single item in the marketplace currency's major unit, not in cents.
     * Example: an item priced at $25 is sent as 25, not 2500.
     * This field is an Int, so fractional prices such as 25.35 cannot be sent exactly.
     * Nullable in this SDK, but the API requires it. Items at exactly 0, such as gifts, are left
     * out of the purchase; a purchase whose items are all at 0, or with any negative or missing
     * price, is not reported.
     */
    @IntRange(from = 0) val unitPrice: Int? = null,

    /**
     * Not sent. Topsort attributes a purchase from the shopper's earlier clicks and impressions,
     * so a purchase item carries no bid id and the SDK does not serialize this.
     */
    @Deprecated("Purchases are attributed from earlier clicks and impressions, so this is not sent.")
    val resolvedBidId: String? = null,

    /**
     * The vendor ID for halo attribution.
     * Used to attribute purchases to vendors when applicable.
     */
    val vendorId: String? = null,
) {

    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("productId", productId)
            .put("quantity", quantity)
            .apply {
                // Consistent null handling: omit keys when value is null
                unitPrice?.let { put("unitPrice", it) }
                vendorId?.let { put("vendorId", it) }
            }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PurchasedItem {
            return PurchasedItem(
                productId = json.getString("productId"),
                quantity = json.getInt("quantity"),
                unitPrice = json.getIntOrNull("unitPrice"),
                vendorId = json.getStringOrNull("vendorId"),
            )
        }
    }
}

/**
 * Every rule in [items] the events API enforces with a 400, one message per broken field, or an
 * empty list when the purchase can be sent. A single bad item fails the whole purchase server-side,
 * so the caller drops the purchase rather than sending it. Items at unitPrice 0 (gifts) are not
 * checked here: the caller leaves them out of the purchase, which is only rejected when nothing
 * with a price is left.
 */
internal fun purchaseItemErrors(items: List<PurchasedItem>): List<String> {
    if (items.isEmpty()) return listOf("items must not be empty")
    if (items.all { it.unitPrice == 0 }) return listOf("total price is zero: every item has unitPrice 0")
    return items.withIndex().filter { it.value.unitPrice != 0 }.flatMap { (index, item) ->
        val prefix = "items[$index] (${item.productId})"
        listOfNotNull(
            "$prefix: productId must not be empty".takeIf { item.productId.isEmpty() },
            "$prefix: quantity must be at least 1, was ${item.quantity}".takeIf { item.quantity < 1 },
            when {
                item.unitPrice == null -> "$prefix: unitPrice is required"
                item.unitPrice <= 0 -> "$prefix: unitPrice must be greater than 0, was ${item.unitPrice}"
                else -> null
            },
        )
    }
}

internal data class PurchaseEventResponse(
    val purchaseId: String,
    val id: String
)
