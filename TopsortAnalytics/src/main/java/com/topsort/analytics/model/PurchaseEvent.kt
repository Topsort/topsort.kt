package com.topsort.analytics.model

import androidx.annotation.IntRange

data class PurchaseEvent(
    private val eventType: EventType = EventType.Purchase,
    val session: Session,
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
     * Items purchased
     */
    val items: List<PurchasedItem>,

    /**
     * The marketplace assigned ID for the order
     */
    val id: String
)

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
)

internal data class PurchaseEventResponse(
    val purchaseId: String,
    val id: String
)
