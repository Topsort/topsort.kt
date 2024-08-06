package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.EntityType

data class BannerResponse(
    val id: String,
    val type: EntityType,
    val url: String,
    val resolvedBidId: String,
) {}
