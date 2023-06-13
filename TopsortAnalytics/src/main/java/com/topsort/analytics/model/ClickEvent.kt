package com.topsort.analytics.model

data class ClickEvent(
    private val eventType: EventType = EventType.Click,
    val session: Session,
    val placement: Placement,

    /**
     * The product that was clicked.
     */
    val productId: String? = null,

    /**
     * Required for promoted products. Must be the ID for the auction the product won
     */
    val auctionId: String? = null,

    /**
     * The marketplace's unique ID for the click
     */
    val id: String? = null,
    val resolvedBidId: String? = null,
    val occurredAt: String? = null
)

internal data class ClickEventResponse(
    val clickId: String
)
