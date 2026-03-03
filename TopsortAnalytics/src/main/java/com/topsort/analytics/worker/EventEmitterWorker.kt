package com.topsort.analytics.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.topsort.analytics.Cache
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.EventType
import com.topsort.analytics.model.ImpressionEvent
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

    override fun doWork(): Result {
        with (inputData) {
            val eventTypeOrdinal = getInt(EXTRA_EVENT_TYPE, -1)
            recordId = getLong(EXTRA_RECORD_ID, -1)

            if (recordId < 0 || eventTypeOrdinal < 0) {
                return Result.success()
            }

            eventType = EventType.values()[eventTypeOrdinal]
        }

        when (eventType) {
            EventType.Impression -> {
                val event = Cache.readImpression(recordId) ?: return Result.success()
                return if (reportImpression(event)) {
                    Cache.deleteEvent(recordId)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
            EventType.Click -> {
                val event = Cache.readClick(recordId) ?: return Result.success()
                return if (reportClick(event)) {
                    Cache.deleteEvent(recordId)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
            EventType.Purchase -> {
                val event = Cache.readPurchase(recordId) ?: return Result.success()
                return if (reportPurchase(event)) {
                    Cache.deleteEvent(recordId)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        }
    }

    private fun reportImpression(impressionEvent: ImpressionEvent): Boolean {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportImpression(impressionEvent)
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to report impression: ${response.code} ${response.message}")
            }
            response.isSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting impression", e)
            false
        }
    }

    private fun reportClick(clickEvent: ClickEvent): Boolean {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportClick(clickEvent)
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to report click: ${response.code} ${response.message}")
            }
            response.isSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting click", e)
            false
        }
    }

    private fun reportPurchase(purchaseEvent: PurchaseEvent): Boolean {
        return try {
            val response = TopsortAnalyticsHttpService.service.reportPurchase(purchaseEvent)
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to report purchase: ${response.code} ${response.message}")
            }
            response.isSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting purchase", e)
            false
        }
    }

    companion object {
        private const val TAG = "TopsortEventEmitter"

        const val EXTRA_RECORD_ID = "EXTRA_RECORD_ID"
        const val EXTRA_EVENT_TYPE = "EXTRA_EVENT_TYPE"

        const val WORK_NAME = "TopsortAnalyticsReporter"
    }
}
