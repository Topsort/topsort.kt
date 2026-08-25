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
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

private const val LOG_TAG = "TopSortAnalytics"
private const val INVALID_CONFIG_ERROR_MESSAGE = "Please call setup from the application context before logging events"

object Analytics : TopsortAnalytics {

    private var applicationContext: Context? = null
    private var workManager: WorkManager? = null
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
        enqueueEventRequest(recordId, EventType.Purchase)
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
        enqueueEventRequest(recordId, EventType.PageView)
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
     * Schedules a work and enqueues it, the work manager will execute this work based on the
     * work configuration provided!
     */
    private fun enqueueEventRequest(
        recordId: Long,
        eventType: EventType
    ) {
        val data = Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, recordId)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, eventType.ordinal)
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

        val wm = workManager ?: run {
            Log.e(LOG_TAG, INVALID_CONFIG_ERROR_MESSAGE)
            return
        }

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
        wm.enqueueUniqueWork(
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
        enqueueEventRequest(recordId, EventType.Impression)
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
        enqueueEventRequest(recordId, EventType.Click)
    }
}
