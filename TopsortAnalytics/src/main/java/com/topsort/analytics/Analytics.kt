package com.topsort.analytics

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.topsort.analytics.core.eventNow
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.Channel
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ClickType
import com.topsort.analytics.model.auctions.Device
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EventType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Page
import com.topsort.analytics.model.PageView
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.model.PurchasedItem
import com.topsort.analytics.model.Session
import com.topsort.analytics.worker.EventEmitterWorker
import com.topsort.analytics.worker.PendingEventSweepWorker

private const val LOG_TAG = "TopSortAnalytics"
private const val INVALID_CONFIG_ERROR_MESSAGE = "Please call setup from the application context before logging events"

object Analytics : TopsortAnalytics {

    // Volatile: setup() writes these from the host's thread while WorkManager threads and any
    // caller of the public opaqueUserId getter read them. Without it a reader can miss the write
    // and fall into the "setup was never called" path, or - worse - see session non-null in
    // assertSetup() and null again at the session!! in resolveOpaqueUserId, which would throw out
    // of public API. Same reason the cache's shared fields and the HTTP service instance are
    // volatile; these were the ones left out.
    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var workManager: WorkManager? = null

    @Volatile
    private var session: Session? = null

    /**
     * The opaque user id currently in effect for reported events, or null before [setup] has run.
     *
     * This is not always the value passed to [setup]: [UserIdentity.Unidentified] falls back to
     * the id already in effect, or to a minted placeholder when there is nothing to fall back on.
     * Read this to reconcile reported events against your own records, and note that a placeholder
     * will not audience-match - call [setup] again with [UserIdentity.Identified] once your own
     * identifier is available.
     */
    val opaqueUserId: String?
        get() = session?.opaqueUserId

    /**
     * Notified whenever cached events are discarded undelivered. Null by default: discards are
     * then only logged. May be set before or after [setup].
     */
    var eventDiscardListener: EventDiscardListener?
        get() = Cache.discardListener
        set(value) {
            Cache.discardListener = value
        }

    /**
     * Setup initial properties required for the analytics library,
     * Call this from the Application class, before submitting any event,
     * Or when a new opaqueUserId or bearer token has to be used.
     *
     * @param application The Application instance of the app.
     * @param opaqueUserId The SessionId allows correlating user activity during a session whether or not they are actually logged in.
     * @param token The bearer token
     */
    // WARNING through 3.x, ERROR at 4.0.0. Not removed: deleting it is a NoSuchMethodError for
    // every consumer who compiled against it and has not rebuilt.
    //
    // No ReplaceWith: the mechanical rewrite is behaviour-preserving, which means it preserves the
    // silent fallback this deprecation exists to remove.
    @Deprecated(
        "A blank opaqueUserId silently means \"mint an id for me\", producing events that never " +
            "audience-match. Say which you mean: UserIdentity.of(id) if it might be blank, or " +
            "UserIdentity.Unidentified if you genuinely have none.",
        level = DeprecationLevel.WARNING,
    )
    @SuppressLint("KotlinNullnessAnnotation")
    fun setup(
        @NonNull application: Application,
        @NonNull opaqueUserId: String,
        @NonNull token: String
    ) {
        // Blank keeps mapping to Unidentified: this overload's whole contract was that blank is
        // tolerated, and changing that would break callers on upgrade. The deprecation is how they
        // find out; UserIdentity.of spells the same conversion out loud for anyone migrating.
        // Warn here, not in the cache: by the time Cache sees it this is indistinguishable from a
        // deliberate Unidentified, and a caller who passed a blank by accident is exactly who the
        // deprecation is for - and exactly who is least likely to be reading compiler warnings.
        if (opaqueUserId.isBlank()) {
            Log.w(
                LOG_TAG,
                "Blank opaqueUserId; reporting as UserIdentity.Unidentified. If that is what you " +
                    "meant, say so explicitly - events under a minted id do not audience-match.",
            )
        }

        setup(application, UserIdentity.of(opaqueUserId), token)
    }

    /**
     * Setup initial properties required for the analytics library.
     *
     * Call this from the Application class before reporting any event, and again whenever the
     * identity or the bearer token changes.
     *
     * @param application The Application instance of the app.
     * @param identity Who the reported events belong to. Pass [UserIdentity.Identified] with the
     * marketplace's own identifier whenever one is available, logged in or not, because audience
     * matching resolves it against the marketplace's records. Pass [UserIdentity.Unidentified]
     * only when there is genuinely no identifier to give.
     * @param token The bearer token
     */
    @SuppressLint("KotlinNullnessAnnotation")
    fun setup(
        @NonNull application: Application,
        @NonNull identity: UserIdentity,
        @NonNull token: String
    ) {
        applicationContext = application.applicationContext
        // Same guard as PendingEventSweepWorker: getInstance throws when the host disabled the
        // default initializer and has not initialized WorkManager yet, and a host's own
        // Configuration.Provider can throw anything. Neither may crash the host.
        @Suppress("TooGenericExceptionCaught")
        workManager = try {
            WorkManager.getInstance(applicationContext!!)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "WorkManager unavailable; events will be logged, not sent", e)
            null
        }
        val previousOpaqueUserId = session?.opaqueUserId
        val resolvedOpaqueUserId = Cache.setup(application, identity, token)

        session = Session(
            opaqueUserId = resolvedOpaqueUserId
        )

        // A setup() that changes the user starts a fresh set of bids, because the new user's
        // impressions are their own and must not be dropped as duplicates of the previous
        // one's. One that resolves to the same user must keep the set: setup() is also how a
        // caller refreshes an expired token, and UserIdentity.Unidentified deliberately keeps
        // the id already in effect, so clearing here would reopen the duplicate it is meant to
        // stop.
        if (previousOpaqueUserId != resolvedOpaqueUserId) {
            ReportedBids.clear()
        }

        schedulePendingEventSweep()
    }

    override fun reportImpressionPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
        page: Page?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val impressions = listOf(
            Impression.Factory.buildPromoted(
                resolvedBidId = resolvedBidId,
                placement = placement,
                opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                id = id ?: randomId(),
                occurredAt = occurredAt ?: eventNow(),
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        )

        reportImpressions(impressions)
    }

    override fun reportImpressionOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
        page: Page?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val impressions = listOf(
            Impression.Factory.buildOrganic(
                entity = entity,
                placement = placement,
                opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                id = id ?: randomId(),
                occurredAt = occurredAt ?: eventNow(),
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        )

        reportImpressions(impressions)
    }

    override fun reportClickPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
        page: Page?,
        clickType: ClickType?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val clicks = listOf(
            Click.Factory.buildPromoted(
                resolvedBidId = resolvedBidId,
                placement = placement,
                opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                id = id ?: randomId(),
                occurredAt = occurredAt ?: eventNow(),
                deviceType = deviceType,
                channel = channel,
                page = page,
                clickType = clickType,
            )
        )

        reportClicks(clicks)
    }

    override fun reportClickOrganic(
        entity: Entity,
        placement: Placement,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
        page: Page?,
        clickType: ClickType?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val clicks = listOf(
            Click.Factory.buildOrganic(
                entity = entity,
                placement = placement,
                opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                id = id ?: randomId(),
                occurredAt = occurredAt ?: eventNow(),
                deviceType = deviceType,
                channel = channel,
                page = page,
                clickType = clickType,
            )
        )

        reportClicks(clicks)
    }

    override fun reportPurchase(
        items: List<PurchasedItem>,
        id: String,
        opaqueUserId: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
        page: Page?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val purchaseEvent = PurchaseEvent(
            purchases = listOf(
                Purchase(
                    id = id,
                    items = items,
                    occurredAt = occurredAt ?: eventNow(),
                    opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                    deviceType = deviceType,
                    channel = channel,
                    page = page,
                ),
            ),
        )

        val recordId = Cache.storePurchase(purchaseEvent)
        enqueueReportedEvent(recordId, EventType.Purchase)
    }

    override fun reportPageView(
        page: Page,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
        deviceType: Device?,
        channel: Channel?,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val pageViewEvent = PageViewEvent(
            pageviews = listOf(
                PageView.Factory.build(
                    page = page,
                    occurredAt = occurredAt ?: eventNow(),
                    opaqueUserId = resolveOpaqueUserId(opaqueUserId),
                    id = id ?: randomId(),
                    deviceType = deviceType,
                    channel = channel,
                ),
            ),
        )

        val recordId = Cache.storePageView(pageViewEvent)
        enqueueReportedEvent(recordId, EventType.PageView)
    }

    /**
     * The opaque user id for a single reported event. A per-call value only overrides the session
     * one when it is actually populated; a blank would be rejected by the API for a missing
     * opaqueUserId, which is never what the caller meant.
     */
    private fun resolveOpaqueUserId(opaqueUserId: String?): String =
        opaqueUserId?.takeIf { it.isNotBlank() } ?: session!!.opaqueUserId

    /**
     * The batch entry points take events the caller built themselves, through public factories that
     * do not validate the id. Sanitising here rather than at each factory keeps the invariant at the
     * one choke point every event passes through on its way into the cache: nothing reaches the wire
     * with a blank opaqueUserId, whichever entry point it came in by.
     */
    private fun Impression.withResolvedOpaqueUserId(): Impression =
        if (opaqueUserId.isNotBlank()) this
        else copy(opaqueUserId = resolveOpaqueUserId(null))

    private fun Click.withResolvedOpaqueUserId(): Click =
        if (opaqueUserId.isNotBlank()) this
        else copy(opaqueUserId = resolveOpaqueUserId(null))

    /**
     * Whether this impression is the first report of its resolved bid, and so should be sent.
     *
     * Organic impressions carry no bid and always pass. See [ReportedBids] for why a promoted bid
     * only ever earns one impression, and why clicks are not filtered the same way.
     */
    private fun Impression.keepAsFirstReportOfItsBid(): Boolean {
        val bidId = resolvedBidId ?: return true
        if (ReportedBids.markReported(bidId)) {
            return true
        }
        Log.w(
            LOG_TAG,
            "Dropping a repeat impression for resolvedBidId $bidId. A resolved bid earns one " +
                "impression; report it once, when the ad is shown, not on every redraw or " +
                "recomposition."
        )
        return false
    }

    /**
     * Asks the sweep to run in the background.
     *
     * Deliberately not inline: [setup] is documented as something to call from the Application
     * class, reading the cache decrypts every record, and pruning writes synchronously. Doing that
     * on the caller's thread risked an ANR at startup - worst on exactly the installs with a large
     * stranded backlog, which are the ones the sweep exists for.
     *
     * KEEP leaves an already-pending sweep alone rather than duplicating it.
     */
    private fun schedulePendingEventSweep() {
        val wm = workManager ?: return
        wm.enqueueUniqueWork(
            PendingEventSweepWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PendingEventSweepWorker>().build(),
        )
    }

    /**
     * Enqueues delivery for an event the host app just reported.
     *
     * Separate from the sweep's path only in where the [WorkManager] comes from: here it is the one
     * [setup] resolved, and its absence means setup was never called, which is the documented
     * "logged but not sent" degradation rather than an error worth retrying.
     */
    private fun enqueueReportedEvent(recordId: Long, eventType: EventType) {
        val wm = workManager ?: run {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }
        EventEmitterWorker.enqueue(wm, recordId, eventType)
    }

    private fun assertSetup(): Boolean {
        return applicationContext != null
                && session != null
                && workManager != null
    }

    public fun reportImpressions(
        impressions : List<Impression>,
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val unreported = impressions.filter { it.keepAsFirstReportOfItsBid() }
        if (unreported.isEmpty()) {
            return
        }

        val impressionEvent = ImpressionEvent(
            impressions = unreported.map { it.withResolvedOpaqueUserId() },
        )

        val recordId = Cache.storeImpression(impressionEvent)
        enqueueReportedEvent(recordId, EventType.Impression)
    }

    private fun reportClicks(
        clicks: List<Click>
    ) {
        if (!assertSetup()) {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

        val clickEvent = ClickEvent(
            clicks = clicks.map { it.withResolvedOpaqueUserId() },
        )

        val recordId = Cache.storeClick(clickEvent)
        enqueueReportedEvent(recordId, EventType.Click)
    }
}
