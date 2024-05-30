package com.topsort.analytics.model

import kotlinx.serialization.Serializable

@Serializable
data class ImpressionEvent(
    private val eventType: EventType = EventType.Impression,
    val session: Session,
    val impressions: List<Impression>,
    val occurredAt: String? = null
)

@Serializable
data class Impression(
    val placement: Placement,

    /**
     * The product that was rendered
     */
    val productId: String? = null,

    /**
     * Required for promoted products. Must be the ID for the auction the product won.
     */
    val auctionId: String? = null,

    /**
     * The marketplace's ID for the impression
     */
    val id: String? = null,

    val resolvedBidId: String? = null
)

internal data class ImpressionEventResponse(
    val impressions: List<Impression>
)
