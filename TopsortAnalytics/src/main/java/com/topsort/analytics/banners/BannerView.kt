package com.topsort.analytics.banners

import android.content.Context
import android.widget.ImageView
import coil.load
import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.service.TopsortAuctionsHttpService

class BannerView(context: Context, config: BannerConfig) : ImageView(context) {
    init {
        val result = runBannerAuction(config)
        if (result != null) {
            val url = result.url;
            this.load(url);
            // TODO: Sets OnClickListeners for reporting the click and to go to the banner's destination
        }
    }
}

fun runBannerAuction(config: BannerConfig): BannerResponse? {
    val auction = buildBannerAuction(config)
    val request = AuctionRequest(listOf(auction))
    val response = TopsortAuctionsHttpService.runAuctions(request)
    if ((response?.results?.isNotEmpty() == true)) {
        if (response.results[0].winners.isNotEmpty()) {
            val winner = response.results[0].winners[0]
            return BannerResponse(
                id = winner.id,
                url = winner.asset!!.url,
                resolvedBidId = winner.resolvedBidId
            )
        }
    }
    return null
}

fun buildBannerAuction(config: BannerConfig): Auction {
    when (config) {
        is BannerConfig.LandingPage -> {
            return Auction.Factory.buildBannerAuctionLandingPage(
                1,
                config.slotId,
                config.ids,
                config.device,
                config.geoTargeting
            )
        }

        is BannerConfig.CategorySingle -> {
            return Auction.Factory.buildBannerAuctionCategorySingle(
                1,
                config.slotId,
                config.category,
                config.device,
                config.geoTargeting
            )
        }

        is BannerConfig.CategoryMultiple -> {
            return Auction.Factory.buildBannerAuctionCategoryMultiple(
                1,
                config.slotId,
                config.categories,
                config.device,
                config.geoTargeting
            )
        }

        is BannerConfig.CategoryDisjunctions -> {
            return Auction.Factory.buildBannerAuctionCategoryDisjunctions(
                1, config.slotId, config.disjunctions, config.device, config.geoTargeting
            )
        }

        is BannerConfig.Keyword -> {
            return Auction.Factory.buildBannerAuctionKeywords(
                1,
                config.slotId,
                config.keyword,
                config.device,
                config.geoTargeting
            )
        }
    }
}
