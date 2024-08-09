package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.EntityType

/**
 * Response for a single slot banner auction
 *
 * @property id id of the winning entity
 * @property type type of the winning entity
 * @property url url of the banner to show
 * @property resolvedBidId id for tracking the auction result on events
 */
data class BannerResponse(
    val id: String,
    val type: EntityType,
    val url: String,
    val resolvedBidId: String,
)