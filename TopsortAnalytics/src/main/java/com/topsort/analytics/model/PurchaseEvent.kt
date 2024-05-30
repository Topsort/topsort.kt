package com.topsort.analytics.model

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseEvent(
    private val eventType: EventType = EventType.Purchase,
    val session: Session,
    val purchasedAt: String,

    /**
     * Items purchased
     */
    val items: List<PurchasedItem>,

    /**
     * The marketplace assigned ID for the order
     */
    val id: String?
)

@Serializable
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
    val auctionId: String? = null,

    val resolvedBidId: String? = null
)

internal data class PurchaseEventResponse(
    val purchaseId: String,
    val id: String
)
