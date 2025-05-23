package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.ApiConstants
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.TopsortAuctionsHttpService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Run a banner auction with a single slot
 *
 * @param config the banner configuration that specifies which kind of banner auction to run
 * @return A BannerResponse if the auction successfully returned a winner or null if not.
 * @throws AuctionError if there was an error during the auction process
 */
suspend fun runBannerAuction(config: BannerConfig): BannerResponse? {
    try {
        val auction = buildBannerAuction(config)
        
        // Validate auction count
        val auctionCount = 1 // Single auction for now
        if (auctionCount < ApiConstants.MIN_AUCTIONS || auctionCount > ApiConstants.MAX_AUCTIONS) {
            throw AuctionError.InvalidNumberAuctions(auctionCount)
        }

        val request = AuctionRequest(listOf(auction))
        
        try {
            val response = withContext(Dispatchers.IO) {
                TopsortAuctionsHttpService.runAuctions(request)
            }
            
            // Check if there are any results with winners
            if (response.results.isNotEmpty() && response.results[0].winners.isNotEmpty()) {
                val winner = response.results[0].winners[0]
                return BannerResponse(
                    id = winner.id,
                    url = winner.asset!![0].url,
                    type = winner.type,
                    resolvedBidId = winner.resolvedBidId
                )
            }
            // No error, but no winners either
            return null
        } catch (e: Exception) {
            throw AuctionError.HttpError(e)
        }
    } catch (e: AuctionError) {
        // Re-throw AuctionError exceptions
        throw e
    } catch (e: Exception) {
        // Wrap other exceptions
        throw AuctionError.HttpError(e)
    }
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