package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import coil.load
import com.topsort.analytics.Analytics
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.auctions.EntityType


class BannerView(
    context: Context,
    attrs: AttributeSet
) : ImageView(context, attrs) {
    lateinit var config: BannerConfig
    lateinit var location: String
    lateinit var path: String
    lateinit var onClick: (String, EntityType) -> Unit

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
                onClick(result.id, result.type)
            }
        }
    }
}