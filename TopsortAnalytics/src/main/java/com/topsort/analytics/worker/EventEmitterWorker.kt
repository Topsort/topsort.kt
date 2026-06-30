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

internal class EventEmitterWorker(
    context: Context,
    params: WorkerParameters
) : Worker(
    context,
    params
) {

    private lateinit var eventType: EventType
    private var recordId = -1L

    init {
        Cache.initialize(context)
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    override fun doWork(): Result {
        with (inputData) {
            val eventTypeOrdinal = getInt(EXTRA_EVENT_TYPE, -1)
            recordId = getLong(EXTRA_RECORD_ID, -1)

            if (recordId < 0 || eventTypeOrdinal < 0 || eventTypeOrdinal >= EventType.entries.size) {
                return Result.success()
            }

            eventType = EventType.entries[eventTypeOrdinal]
        }

        val sendResult = when (eventType) {
            EventType.Impression -> {
                val event = Cache.readImpression(recordId) ?: return Result.success()
                reportImpression(event)
            }
            EventType.Click -> {
                val event = Cache.readClick(recordId) ?: return Result.success()
                reportClick(event)
            }
            EventType.Purchase -> {
                val event = Cache.readPurchase(recordId) ?: return Result.success()
                reportPurchase(event)
            }
            EventType.PageView -> {
                val event = Cache.readPageView(recordId) ?: return Result.success()
                reportPageView(event)
            }
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

        const val WORK_NAME = "TopsortAnalyticsReporter"
    }
}
