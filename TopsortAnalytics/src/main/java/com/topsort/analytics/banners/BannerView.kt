package com.topsort.analytics.banners

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
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
 * Inflate it from XML as a `com.topsort.analytics.banners.BannerView` element, or create it from
 * code - `BannerView(context)` - which is how it goes inside Jetpack Compose's `AndroidView`.
 *
 * @param context
 * @param attrs AttributeSet from XML inflation, if any. Since this view inherits from `ImageView`
 * you can set attributes as you would with a regular `ImageView`.
 * @param defStyleAttr default style attribute, as for any `View`.
 */
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    /** The impression owed to the current winner, reported once the view is on screen. */
    private var pendingImpression: (() -> Unit)? = null

    private val onLayout = ViewTreeObserver.OnGlobalLayoutListener { reportIfOnScreen() }
    private val onScroll = ViewTreeObserver.OnScrollChangedListener { reportIfOnScreen() }

    private val visibleRect = Rect()

    /**
     * Whether the banner is on screen: shown with every ancestor, in a visible window, and not
     * clipped away by an ancestor. Occlusion by a sibling or another window is not detected, and
     * there is no duration - the IAB viewable standard is tracked separately. Replaceable in
     * tests, where views have no window.
     */
    @VisibleForTesting
    internal var isOnScreen: () -> Boolean = {
        isShown && windowVisibility == VISIBLE && getGlobalVisibleRect(visibleRect)
    }

    private fun reportIfOnScreen() {
        val report = pendingImpression ?: return
        if (!isOnScreen()) return
        pendingImpression = null
        stopWatching()
        report()
    }

    private fun startWatching() {
        // Idempotent: attaching merges a floating observer's listeners into the window's before
        // onAttachedToWindow runs, and remove() only takes the first copy.
        stopWatching()
        viewTreeObserver.addOnGlobalLayoutListener(onLayout)
        viewTreeObserver.addOnScrollChangedListener(onScroll)
    }

    private fun stopWatching() {
        viewTreeObserver.removeOnGlobalLayoutListener(onLayout)
        viewTreeObserver.removeOnScrollChangedListener(onScroll)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Detached and re-attached without a new setup - an off-screen pager page - still owes it.
        if (pendingImpression != null) startWatching()
    }

    override fun onDetachedFromWindow() {
        stopWatching()
        super.onDetachedFromWindow()
    }

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
     * impression once the banner is on screen - not on layout, which happens below the fold too -
     * and reports a click when it is tapped. Call from the main thread.
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
        // A second setup replaces the pending impression: that banner never reached the screen.
        pendingImpression = {
            Analytics.reportImpressionPromoted(
                resolvedBidId = winner.resolvedBidId,
                placement = placement
            )
        }
        stopWatching()
        startWatching()
        post { reportIfOnScreen() }
        this.setOnClickListener {
            Analytics.reportClickPromoted(
                resolvedBidId = winner.resolvedBidId,
                placement = placement
            )
            onClick(winner.id, winner.type)
        }
    }
}