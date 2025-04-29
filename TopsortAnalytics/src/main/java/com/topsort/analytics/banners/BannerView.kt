package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import coil.load
import com.topsort.analytics.Analytics
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.auctions.AuctionError
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

    private var onNoWinnersCallback: (() -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null
    private var onImageLoadCallback: (() -> Unit)? = null
    private var onAuctionErrorCallback: ((AuctionError) -> Unit)? = null

    /**
     * Set a callback to be invoked when no winners are returned from the auction.
     *
     * @param callback function to invoke when no winners are returned
     * @return this BannerView instance for method chaining
     */
    fun onNoWinners(callback: () -> Unit): BannerView {
        this.onNoWinnersCallback = callback
        return this
    }

    /**
     * Set a callback to be invoked when an error occurs during the auction or image loading.
     *
     * @param callback function to invoke when an error occurs
     * @return this BannerView instance for method chaining
     */
    fun onError(callback: (Throwable) -> Unit): BannerView {
        this.onErrorCallback = callback
        return this
    }

    /**
     * Set a callback to be invoked when a specific auction error occurs.
     *
     * @param callback function to invoke when an auction error occurs
     * @return this BannerView instance for method chaining
     */
    fun onAuctionError(callback: (AuctionError) -> Unit): BannerView {
        this.onAuctionErrorCallback = callback
        return this
    }

    /**
     * Set a callback to be invoked when the banner image is successfully loaded.
     *
     * @param callback function to invoke when the image is loaded
     * @return this BannerView instance for method chaining
     */
    fun onImageLoad(callback: () -> Unit): BannerView {
        this.onImageLoadCallback = callback
        return this
    }

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
        try {
            val result = runBannerAuction(config)
            if (result != null) {
                this.load(result.url) {
                    listener(
                        onSuccess = { _, _ ->
                            onImageLoadCallback?.invoke()
                        },
                        onError = { _, throwable ->
                            onErrorCallback?.invoke(throwable)
                        }
                    )
                }
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
            } else {
                onNoWinnersCallback?.invoke()
            }
        } catch (e: AuctionError) {
            // Handle specific auction errors
            when (e) {
                is AuctionError.EmptyResponse -> onNoWinnersCallback?.invoke()
                is AuctionError.HttpError -> {
                    onAuctionErrorCallback?.invoke(e)
                    onErrorCallback?.invoke(e)
                }
                is AuctionError.InvalidNumberAuctions -> {
                    onAuctionErrorCallback?.invoke(e)
                    onErrorCallback?.invoke(e)
                }
                is AuctionError.SerializationError -> {
                    onAuctionErrorCallback?.invoke(e)
                    onErrorCallback?.invoke(e)
                }
                is AuctionError.DeserializationError -> {
                    onAuctionErrorCallback?.invoke(e)
                    onErrorCallback?.invoke(e)
                }
            }
        } catch (e: Throwable) {
            onErrorCallback?.invoke(e)
        }
    }
}