package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import coil.load
import com.topsort.analytics.Analytics
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.auctions.EntityType


/**
 * View for displaying banners powered by auctions.
 *
 * @constructor The constructor is meant to be called automatically from XML inflation.
 * You can add this view to your layout by using a `com.topsort.analytics.banners.BannerView` element.
 *
 * @param context
 * @param attrs AttributeSet for the view. Since this view inherits from `ImageView`
 * you can set attributes as you would with a regular `ImageView`.
 */
class BannerView(
    context: Context,
    attrs: AttributeSet
) : ImageView(context, attrs) {

    /**
     * Setup the banner in the view by running an auction in the background.
     *
     * @param config a BannerConfig object that specifies the parameters for the auction
     * @param path identifier for the activity where the banner is displayed. It's recommended to be the deeplink for the view.
     * @param location optional name for the location within the view where the banner is displayed.
     * @param onClick callback for when the banner is clicked. Usually this should navigate to an activity related to the banner (e.g. the product page for the product shown in the banner).
     * @receiver
     */
    suspend fun setup(
        config: BannerConfig,
        path: String,
        location: String?,
        onClick: (String, EntityType) -> Unit
    ) {
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