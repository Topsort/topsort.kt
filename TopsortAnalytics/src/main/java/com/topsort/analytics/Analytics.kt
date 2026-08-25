package com.topsort.analytics

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

private const val LOG_TAG = "TopSortAnalytics"
private const val INVALID_CONFIG_ERROR_MESSAGE = "Please call setup from the application context before logging events"

/** Upper bound on how many cached records one sweep reads and re-enqueues. */
private const val MAX_RESEND_PER_SETUP = 100

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
     * This is not always the value passed to [setup]: a blank argument falls back to the last
     * non-blank id, or to a generated placeholder when there is nothing to fall back on. Read this
     * to reconcile reported events against your own records, and note that a placeholder will not
     * audience-match - call [setup] again with your own identifier once it is available.
     */
    val opaqueUserId: String?
        get() = session?.opaqueUserId

    /**
     * Setup initial properties required for the analytics library,
     * Call this from the Application class, before submitting any event,
     * Or when a new opaqueUserId or bearer token has to be used.
     *
     * @param application The Application instance of the app.
     * @param opaqueUserId The SessionId allows correlating user activity during a session whether or not they are actually logged in.
     * @param token The bearer token
     */
    @SuppressLint("KotlinNullnessAnnotation")
    fun setup(
        @NonNull application: Application,
        @NonNull opaqueUserId: String,
        @NonNull token: String
    ) {
        applicationContext = application.applicationContext
        workManager = WorkManager.getInstance(applicationContext!!)
        val resolvedOpaqueUserId = Cache.setup(application, opaqueUserId, token)

        session = Session(
            opaqueUserId = resolvedOpaqueUserId
        )

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
                occurredAt = occurredAt ?: eventTime(),
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
                occurredAt = occurredAt ?: eventTime(),
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
                occurredAt = occurredAt ?: eventTime(),
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
                occurredAt = occurredAt ?: eventTime(),
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
                    occurredAt = occurredAt ?: eventTime(),
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
                    occurredAt = occurredAt ?: eventTime(),
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
     * Returns ISO8601/RFC3339 formatted timestamp
     */
    private fun eventTime() = ISODateTimeFormat.dateTime().print(DateTime())

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
     * Re-enqueues undelivered records so a stranded backlog gets another chance at delivery.
     *
     * Only called from [PendingEventSweepWorker], so never on the main thread. The [workManager] is
     * supplied by that worker rather than read from the field: the field is only set by [setup],
     * and WorkManager can run this worker in a process where setup has not been called (process
     * death with pending work, or an integrator calling setup from an Activity).
     *
     * The sweep deliberately does NOT decide what is too old to send. It re-enqueues every record
     * it reads, anchored to that record's own occurredAt, and [EventEmitterWorker] applies the age
     * cap when the work runs. Two reasons:
     *
     * - It cannot tell "stranded for a week" from "reported a moment ago and already enqueued".
     *   A caller may report an event with an explicit backdated occurredAt through the public API,
     *   and a sweep that pruned on occurredAt alone would destroy it while its own work unit was
     *   still pending - delivering the same event when no sweep happened to be in flight. Asking
     *   WorkManager whether a record has live work is not an option here: the query blocks on the
     *   executor the sweep is already running on.
     * - KEEP makes the re-enqueue a no-op for any record that already has work pending, so the
     *   record stays owned by whoever enqueued it first, and the age decision lands in one place.
     */
    internal fun sweepPendingEvents(workManager: WorkManager) {
        // Installs upgrading from a version that enqueued onto one shared chain still have work
        // pending under the bare WORK_NAME. KEEP cannot see it - that is a different unique name
        // from the per-record WORK_NAME-<id> - so without this a record would have two owners: the
        // surviving chain unit and the one enqueued below. Per-record units run in parallel, so
        // both can read the record before either deletes it, and the events API does not
        // de-duplicate: that is a duplicate counted event, and for CPM campaigns a billed one.
        //
        // Targets only the legacy chain. Note WORK_NAME is also the TAG on every per-record unit,
        // so cancelAllWorkByTag(WORK_NAME) would destroy the entire pending queue - cancelling by
        // unique name is what makes this safe. Idempotent, so it stays after the migration window.
        workManager.cancelUniqueWork(EventEmitterWorker.WORK_NAME)

        val candidates = Cache.pendingRecords(MAX_RESEND_PER_SETUP)
        if (candidates.isEmpty()) return

        Log.i(LOG_TAG, "Re-enqueueing ${candidates.size} undelivered cached event(s)")
        candidates.forEach {
            enqueueEventRequest(workManager, it.recordId, it.eventType, it.occurredAt)
        }
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
        enqueueEventRequest(wm, recordId, eventType)
    }

    /**
     * Schedules a work and enqueues it, the work manager will execute this work based on the
     * work configuration provided!
     */
    private fun enqueueEventRequest(
        workManager: WorkManager,
        recordId: Long,
        eventType: EventType,
        ageAnchor: DateTime? = DateTime.now(),
    ) {
        val data = Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, recordId)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, eventType.ordinal)
            .putLong(EventEmitterWorker.EXTRA_AGE_ANCHOR_MILLIS, ageAnchor?.millis ?: -1)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<EventEmitterWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            // Tagged so all event work stays queryable as a group, independently of how it is
            // scheduled. Useful for diagnostics and for tests.
            .addTag(EventEmitterWorker.WORK_NAME)

        // One unique work name per record, rather than one shared chain for every event.
        //
        // A shared chain couples events that have nothing to do with each other: work appended
        // after a chain has terminated is itself cancelled, so a single terminal failure used to
        // silence an install permanently, and one event stuck in retry backoff held up every event
        // behind it. Independent units also make enqueueing idempotent - KEEP means re-enqueueing a
        // record that already has work pending is a no-op, so repeated setup() calls cannot deliver
        // the same event twice.
        //
        // Events carry their own occurredAt, so delivery order does not matter.
        workManager.enqueueUniqueWork(
            EventEmitterWorker.workNameFor(recordId),
            ExistingWorkPolicy.KEEP,
            requestBuilder.build(),
        )
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

        val impressionEvent = ImpressionEvent(
            impressions = impressions.map { it.withResolvedOpaqueUserId() },
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
