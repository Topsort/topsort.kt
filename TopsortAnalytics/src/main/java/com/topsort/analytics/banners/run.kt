package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.TopsortAuctionsHttpService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Run a banner auction with a single slot
 *
 * @param config the banner configuration that specifies which kind of banner auction to run
 * @return A BannerResponse if the auction successfully returned a winner or null if not.
 */
suspend fun runBannerAuction(config: BannerConfig): BannerResponse? {
    val auction = buildBannerAuction(config)
    val request = AuctionRequest(listOf(auction))
    var response: AuctionResponse? = null;
    val auctionJob = CoroutineScope(Dispatchers.IO).launch {
        response = TopsortAuctionsHttpService.runAuctions(request)
    }
    auctionJob.join()
    if ((response?.results?.isNotEmpty() == true)) {
        if (response!!.results[0].winners.isNotEmpty()) {
            val winner = response!!.results[0].winners[0]
            return BannerResponse(
                id = winner.id,
                url = winner.asset!![0].url,
                type = winner.type,
                resolvedBidId = winner.resolvedBidId
            )
        }
    }
    return null
}

/**
 * Builds a low-level Auction object to be run with TopsortAuctionHttpService.
 *
 * Generally, you shouldn't be calling this function yourself and you should use runBannerAuction instead.
 *
 * @param config the banner configuration that specifies which kind of banner auction to run
 * @return an Auction object
 */
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