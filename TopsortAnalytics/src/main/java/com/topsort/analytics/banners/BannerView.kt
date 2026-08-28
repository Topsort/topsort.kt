package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.topsort.analytics.Analytics
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.EntityType
import kotlinx.coroutines.CancellationException


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
     * The impression listener waiting for the next layout pass, if one is.
     *
     * Held so that a second setup() can take it off the observer before adding its own. Each
     * listener removes itself when it fires, which stops one setup reporting twice - but nothing
     * stopped two setups leaving two listeners for the same view, and both would then fire on the
     * same layout pass. That matters most for the winner overload, which is cheap and synchronous
     * and therefore called exactly where views get reused: pools, RecyclerView, an AndroidView
     * update block.
     */
    private var pendingImpressionListener: ViewTreeObserver.OnGlobalLayoutListener? = null

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
                setup(result, path, location, onClick)
            } else {
                onNoWinnersCallback?.invoke()
            }
        } catch (e: AuctionError.EmptyResponse) {
            onNoWinnersCallback?.invoke()
        } catch (e: AuctionError.HttpError) {
            Log.e("BannerView", "HttpError: ${e.message}")
            onAuctionErrorCallback?.invoke(e)
            onErrorCallback?.invoke(e)
        } catch (e: AuctionError.InvalidNumberAuctions) {
            onAuctionErrorCallback?.invoke(e)
            onErrorCallback?.invoke(e)
        } catch (e: AuctionError.SerializationError) {
            onAuctionErrorCallback?.invoke(e)
            onErrorCallback?.invoke(e)
        } catch (e: AuctionError.DeserializationError) {
            onAuctionErrorCallback?.invoke(e)
            onErrorCallback?.invoke(e)
        } catch (e: CancellationException) {
            // The caller's scope was cancelled - navigating away, usually. Reporting it through
            // onError would hand the host a spurious failure and keep running past the scope.
            throw e
        } catch (e: Throwable) {
            onErrorCallback?.invoke(e)
        }
    }

    /**
     * Display a banner you have already won, and report its impression and clicks.
     *
     * Use this when the auction is yours to run - your own HTTP stack, your own auth, retries or
     * telemetry, or a winner you resolved earlier and cached. Map whatever your auction returned
     * onto [BannerResponse] and this view does the rest: loads the creative, reports the
     * impression once the banner has actually been laid out, and reports a click when it is
     * tapped.
     *
     * The point of this overload is that reporting stays here. The impression must fire once per
     * resolved bid, when the ad is really on screen - not on every redraw, recomposition or view
     * rebind - and getting that wrong is billable on a CPM campaign. Owning your auction should
     * not mean owning that too.
     *
     * No [AuctionError] is thrown or reported here, because no auction is run: you handled that
     * before calling. [onNoWinners] is likewise never invoked - an absent winner means there is
     * nothing to show, so do not call this at all.
     *
     * @param winner the winning bid to display, from [runBannerAuction] or from your own auction.
     * @param path identifier for the screen where the banner is displayed. Prefer the real route
     * or deeplink - a constant here makes every per-page report collapse into one bucket.
     * @param location optional name for the location within the screen where the banner is shown.
     * @param onClick callback for when the banner is clicked, receiving the winner's entity id and
     * type. Usually this navigates to whatever the banner advertises.
     */
    fun setup(
        winner: BannerResponse,
        path: String,
        location: String?,
        onClick: (String, EntityType) -> Unit
    ) {
        val placement = Placement(path = path, location = location)

        this.load(winner.url) {
            listener(
                onSuccess = { _, _ ->
                    onImageLoadCallback?.invoke()
                },
                onError = { _: ImageRequest, error: ErrorResult ->
                    onErrorCallback?.invoke(error.throwable)
                }
            )
        }
        // On layout rather than on load: the impression is owed when the banner occupies the
        // screen. A listener still waiting from an earlier setup is dropped first - that banner
        // never reached the screen, so it is owed nothing, and leaving it registered would report
        // it alongside this one on the same layout pass.
        pendingImpressionListener?.let { viewTreeObserver.removeOnGlobalLayoutListener(it) }
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                pendingImpressionListener = null
                Analytics.reportImpressionPromoted(
                    resolvedBidId = winner.resolvedBidId,
                    placement = placement
                )
            }
        }
        pendingImpressionListener = listener
        this.viewTreeObserver.addOnGlobalLayoutListener(listener)
        this.setOnClickListener {
            Analytics.reportClickPromoted(
                resolvedBidId = winner.resolvedBidId,
                placement = placement
            )
            onClick(winner.id, winner.type)
        }
    }
}