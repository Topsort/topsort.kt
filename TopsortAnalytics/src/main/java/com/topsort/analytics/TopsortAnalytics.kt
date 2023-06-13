package com.topsort.analytics

import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchasedItem

interface TopsortAnalytics {

    /**
     * Report a list of impressions
     */
    fun reportImpression(
        impressions: List<Impression>,
    )

    /**
     * Reports a single impression with the provided resolvedBidId
     */
    fun reportImpressionWithResolvedBidId(
        resolvedBidId: String,
        placement: Placement
    )

    /**
     * Reports a click event
     *
     * @param productId The product that was clicked.
     * @param auctionId Required for promoted products. Must be the ID for the auction the product won
     * @param id The marketplace's unique ID for the click
     */
    fun reportClick(
        placement: Placement,
        productId: String? = null,
        auctionId: String? = null,
        id: String? = null,
        resolvedBidId: String? = null,
    )

    /**
     * Reports a click event given the provided resolvedBidId
     */
    fun reportClickWithResolvedBidId(
        resolvedBidId: String,
        placement: Placement
    )

    /**
     * Reports a purchase event.
     *  @param items the list of purchased items
     *  @param id The marketplace assigned ID for the order
     */
    fun reportPurchase(
        items: List<PurchasedItem>,
        id: String? = null,
    )
}
