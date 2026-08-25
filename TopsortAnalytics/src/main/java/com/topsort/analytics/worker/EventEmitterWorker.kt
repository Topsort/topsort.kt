package com.topsort.analytics.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.topsort.analytics.Cache
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.EventType
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import org.joda.time.DateTime
import org.json.JSONException

internal class EventEmitterWorker(
    context: Context,
    params: WorkerParameters
) : Worker(
    context,
    params
) {

    private lateinit var eventType: EventType
    private var recordId = -1L
    private var ageAnchorMillis = -1L

    init {
        Cache.initialize(context)
    }

    override fun doWork(): Result {
        with (inputData) {
            val eventTypeOrdinal = getInt(EXTRA_EVENT_TYPE, -1)
            recordId = getLong(EXTRA_RECORD_ID, -1)

            if (recordId < 0 || eventTypeOrdinal < 0) {
                return Result.success()
            }

            eventType = EventType.values()[eventTypeOrdinal]
            ageAnchorMillis = getLong(EXTRA_AGE_ANCHOR_MILLIS, -1)
        }

        if (isPastAgeCap()) {
            // Logged with the record id and type: this is silent data destruction from the
            // marketplace's point of view, and a bare count cannot be reconciled against anything.
            Log.e(
                TAG,
                "Discarding undelivered $eventType record $recordId: " +
                    "age anchor ${DateTime(ageAnchorMillis)} is past the $MAX_EVENT_AGE_DAYS-day cap",
            )
            Cache.deleteEvent(recordId)
            return Result.success()
        }

        val sendResult = try {
            sendCachedEvent() ?: return Result.success()
        } catch (e: JSONException) {
            // The cached body cannot be turned back into an event, so it can never be sent. Left in
            // place it would be re-read by every sweep for the lifetime of the install.
            Log.e(TAG, "Discarding unparseable cached record $recordId", e)
            Cache.deleteEvent(recordId)
            return Result.success()
        }

        return when (sendResult) {
            SendResult.SUCCESS -> {
                Cache.deleteEvent(recordId)
                Result.success()
            }
            SendResult.PERMANENT_FAILURE -> {
                Cache.deleteEvent(recordId)
                Result.failure()
            }
            SendResult.TRANSIENT_FAILURE -> Result.retry()
        }
    }

    /**
     * Whether this event is too old to be worth sending.
     *
     * Checked here rather than only in the sweep, because a work unit can sit in retry backoff or
     * wait on connectivity for days. Without this, an event stranded that way still ships with its
     * original backdated timestamp - most likely outside its attribution window.
     *
     * The anchor travels in the work's input data, so this costs no extra read of the cache. A
     * freshly reported event anchors at the moment it was accepted for delivery; a record picked up
     * by the sweep anchors at its own occurredAt, which is the more conservative of the two.
     */
    private fun isPastAgeCap(): Boolean {
        if (ageAnchorMillis < 0) return false
        return DateTime(ageAnchorMillis).isBefore(DateTime.now().minusDays(MAX_EVENT_AGE_DAYS))
    }

    private fun sendCachedEvent(): SendResult? = when (eventType) {
        EventType.Impression -> Cache.readImpression(recordId)?.let(::reportImpression)
        EventType.Click -> Cache.readClick(recordId)?.let(::reportClick)
        EventType.Purchase -> Cache.readPurchase(recordId)?.let(::reportPurchase)
        EventType.PageView -> Cache.readPageView(recordId)?.let(::reportPageView)
    }

    private fun reportImpression(impressionEvent: ImpressionEvent): SendResult {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportImpression(impressionEvent)
            toSendResult(response.code, response.message, "impression")
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting impression", e)
            SendResult.TRANSIENT_FAILURE
        }
    }

    private fun reportClick(clickEvent: ClickEvent): SendResult {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportClick(clickEvent)
            toSendResult(response.code, response.message, "click")
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting click", e)
            SendResult.TRANSIENT_FAILURE
        }
    }

    private fun reportPurchase(purchaseEvent: PurchaseEvent): SendResult {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportPurchase(purchaseEvent)
            toSendResult(response.code, response.message, "purchase")
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting purchase", e)
            SendResult.TRANSIENT_FAILURE
        }
    }

    private fun reportPageView(pageViewEvent: PageViewEvent): SendResult {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportPageView(pageViewEvent)
            toSendResult(response.code, response.message, "pageview")
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting pageview", e)
            SendResult.TRANSIENT_FAILURE
        }
    }

    @Suppress("detekt:MagicNumber")
    private fun toSendResult(code: Int, message: String, eventType: String): SendResult {
        return when {
            code in 200..299 -> SendResult.SUCCESS
            code in 400..499 -> {
                Log.e(TAG, "Permanent failure reporting $eventType: $code $message")
                SendResult.PERMANENT_FAILURE
            }
            else -> {
                Log.e(TAG, "Transient failure reporting $eventType: $code $message")
                SendResult.TRANSIENT_FAILURE
            }
        }
    }

    private enum class SendResult {
        SUCCESS,
        PERMANENT_FAILURE,
        TRANSIENT_FAILURE,
    }

    companion object {
        private const val TAG = "TopsortEventEmitter"

        const val EXTRA_RECORD_ID = "EXTRA_RECORD_ID"
        const val EXTRA_EVENT_TYPE = "EXTRA_EVENT_TYPE"
        const val EXTRA_AGE_ANCHOR_MILLIS = "EXTRA_AGE_ANCHOR_MILLIS"

        /**
         * How long delivery may keep being attempted, measured from the event's age anchor. Lives
         * here rather than in Analytics because both the sweep and this worker enforce it.
         */
        const val MAX_EVENT_AGE_DAYS = 7

        const val WORK_NAME = "TopsortAnalyticsReporter"

        /**
         * Unique work name for a single cached record. One name per record keeps enqueueing
         * idempotent and keeps one event's failure from touching any other.
         */
        fun workNameFor(recordId: Long): String = "$WORK_NAME-$recordId"
    }
}
