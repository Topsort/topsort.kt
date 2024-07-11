package com.topsort.analytics

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.*
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.*
import com.topsort.analytics.model.events.Click
import com.topsort.analytics.model.events.ClickEvent
import com.topsort.analytics.model.events.Entity
import com.topsort.analytics.model.events.EventType
import com.topsort.analytics.model.events.Impression
import com.topsort.analytics.model.events.ImpressionEvent
import com.topsort.analytics.model.events.Placement
import com.topsort.analytics.model.events.Purchase
import com.topsort.analytics.model.events.PurchaseEvent
import com.topsort.analytics.model.events.PurchasedItem
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
     * Setup initial properties required for the analytics library,
     * Call this from the Application class, before submitting any event,
     * Or when a new sessionId or bearer token has to be used.
     *
     * @param application The Application instance of the app.
     * @param sessionId The SessionId allows correlating user activity during a session whether or not they are actually logged in.
     * @param token The bearer token
     */
    @SuppressLint("KotlinNullnessAnnotation")
    fun setup(
        @NonNull application: Application,
        @NonNull sessionId: String,
        @NonNull token: String
    ) {
        applicationContext = application.applicationContext
        workManager = WorkManager.getInstance(applicationContext!!)
        Cache.setup(application, sessionId, token)

        session = Session(
            sessionId = sessionId
        )
    }

    override fun reportImpressionPromoted(
        resolvedBidId: String,
        placement: Placement,
        opaqueUserId: String?,
        id: String?,
        occurredAt: String?,
    ) {
        val impressions = listOf(
            Impression.Factory.buildPromoted(
                resolvedBidId = resolvedBidId,
                placement = placement,
                opaqueUserId = opaqueUserId ?: session!!.sessionId,
                id = id?: randomId(),
                occurredAt = occurredAt ?: eventTime(),
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
    ) {
        val impressions = listOf(
            Impression.Factory.buildOrganic(
                entity = entity,
                placement = placement,
                opaqueUserId = opaqueUserId ?: session!!.sessionId,
                id = id?: randomId(),
                occurredAt = occurredAt ?: eventTime(),
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
    ) {
        val clicks = listOf(
            Click.Factory.buildPromoted(
                resolvedBidId = resolvedBidId,
                placement = placement,
                opaqueUserId = opaqueUserId ?: session!!.sessionId,
                id = id?: randomId(),
                occurredAt = occurredAt ?: eventTime()
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
    ) {
        val clicks = listOf(
            Click.Factory.buildOrganic(
                entity = entity,
                placement = placement,
                opaqueUserId = opaqueUserId ?: session!!.sessionId,
                id = id?: randomId(),
                occurredAt = occurredAt ?: eventTime()
            )
        )

        reportClicks(clicks)
    }

    override fun reportPurchase(
        items: List<PurchasedItem>,
        id: String,
        opaqueUserId: String?,
        occurredAt: String?,
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
                    opaqueUserId = opaqueUserId ?: session!!.sessionId,
                ),
            ),
        )

        val recordId = Cache.storePurchase(purchaseEvent)
        enqueueEventRequest(recordId, EventType.Purchase)
    }

    /**
     * Returns ISO8601/RFC3339 formatted timestamp
     */
    private fun eventTime() = ISODateTimeFormat.dateTime().print(DateTime())

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
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<EventEmitterWorker>()
            .setInputData(data)
            .setConstraints(constraints)


        val continuation = workManager!!
            .beginUniqueWork(
                EventEmitterWorker.WORK_NAME,
                ExistingWorkPolicy.APPEND,
                OneTimeWorkRequest.Companion.from(EventEmitterWorker::class.java)
            )

        continuation
            .then(requestBuilder.build())
            .enqueue()
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
            impressions = impressions,
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
            clicks = clicks
        )

        val recordId = Cache.storeClick(clickEvent)
        enqueueEventRequest(recordId, EventType.Click)
    }
}
