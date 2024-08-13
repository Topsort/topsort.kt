package com.topsort.analytics.banners

import android.content.Context
import android.widget.ImageView
import coil.load
import com.topsort.analytics.Analytics
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.auctions.EntityType

class BannerView(
    context: Context,
    config: BannerConfig,
    path: String,
    location: String,
    clickCallback: (String, EntityType) -> Unit
) : ImageView(context) {
    init {
        val result = runBannerAuction(config)
        if (result != null) {
            this.load(result.url)
            this.viewTreeObserver.addOnGlobalLayoutListener {
                Analytics.reportImpressionPromoted(
                    resolvedBidId = result.resolvedBidId,
                    placement = Placement(path = path, location = location)
                )
            }
            this.setOnClickListener {
                Analytics.reportClickPromoted(
                    resolvedBidId = result.resolvedBidId,
                    placement = Placement(path = path, location = location)
                )
                clickCallback(result.id, result.type)
            }
        }
    }
}