package com.topsort.analytics

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.topsort.analytics.core.Logger
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.JsonSerializable
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import com.topsort.analytics.worker.EventEmitterWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicBoolean

private const val PREFERENCES_NAME = "topsort_event_cache_async"

private val KEY_IMPRESSION_EVENTS= stringPreferencesKey("KEY_IMPRESSION_EVENTS")
private val KEY_CLICK_EVENTS = stringPreferencesKey("KEY_CLICK_EVENTS")
private val KEY_PURCHASE_EVENTS = stringPreferencesKey("KEY_PURCHASE_EVENTS")

@VisibleForTesting
const val UPLOAD_SIGNAL = "UPLOAD"

val Context.eventDatastore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

internal object EventPipeline {

    private lateinit var applicationContext: Context

    private val scope = CoroutineScope(SupervisorJob())
    private val dispatcher = Dispatchers.IO

    private var workManager: WorkManager? = null

    private var uploadQueued = AtomicBoolean(false)

    fun setup(
        context: Context,
    ) {
        initialize(context)
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent, shouldFlush: Boolean = true
    ) = asyncWrite(impressionEvent.impressions, KEY_IMPRESSION_EVENTS, shouldFlush)

    fun storeClick(
        clickEvent: ClickEvent, shouldFlush: Boolean = true
    ) = asyncWrite(clickEvent.clicks, KEY_CLICK_EVENTS, shouldFlush)

    fun storePurchase(
        purchaseEvent: PurchaseEvent, shouldFlush: Boolean = true
    ) = asyncWrite(purchaseEvent.purchases, KEY_PURCHASE_EVENTS, shouldFlush)

    @VisibleForTesting
    fun upload() {
        val constraints = Constraints.Builder()
            //.setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<EventEmitterWorker>()
            .setConstraints(constraints)

        workManager!!
            .enqueueUniqueWork(
                UPLOAD_SIGNAL,
                ExistingWorkPolicy.REPLACE,
                requestBuilder.build()
            )
    }

    @VisibleForTesting
    fun readImpressions(): String? {
        return read(KEY_IMPRESSION_EVENTS)
    }

    @VisibleForTesting
    fun readClicks(): String? {
        return read(KEY_CLICK_EVENTS)
    }

    @VisibleForTesting
    fun readPurchases(): String? {
        return read(KEY_PURCHASE_EVENTS)
    }

    private fun initialize(context: Context) {
        applicationContext = context.applicationContext
        workManager = WorkManager.getInstance(applicationContext)
    }

    private fun <T : JsonSerializable> asyncWrite(
        events: List<T>,
        key: Preferences.Key<String>,
        shouldFlush: Boolean = true
    ) =
        scope.launch(dispatcher) {
            val json = StringBuilder()
            for (event in events) {
                json.append(event.toJsonObject().toString())
                json.append(",")
            }

            applicationContext.eventDatastore.edit { store ->
                if (store.contains(key)) {
                    store[key] = store[key] + json.toString()
                } else {
                    store[key] = json.toString()
                }
            }

            if(shouldFlush && !uploadQueued.getAndSet(true)){
                upload()
            }
        }

    @VisibleForTesting
    suspend fun aggregateEvents(): Event {
        val data = applicationContext.eventDatastore.data.first()
        val impressions = data[KEY_IMPRESSION_EVENTS]?.trim(',')
        val clicks = data[KEY_CLICK_EVENTS]?.trim(',')
        val purchases = data[KEY_PURCHASE_EVENTS]?.trim(',')

        val impressionEvent =
            impressions?.let { Impression.Factory.fromJsonArray(JSONArray("[$it]")) }
        val clickEvent =
            clicks?.let { Click.Factory.fromJsonArray(JSONArray("[$it]")) }
        val purchaseEvent =
            purchases?.let { Purchase.fromJsonArray(JSONArray("[$it]")) }

        val aggregated = Event(
            impressions = impressionEvent,
            clicks = clickEvent,
            purchases = purchaseEvent,
        )

        return aggregated
    }

    private fun read(key: Preferences.Key<String>): String? {
        return runBlocking {
            val ret = scope.async {
                applicationContext.eventDatastore.data.first()[key]
            }.await()

            ret?.trim(',')
        }
    }

    @VisibleForTesting
    suspend fun clear() {
        applicationContext.eventDatastore.edit { store ->
            store.remove(KEY_IMPRESSION_EVENTS)
            store.remove(KEY_CLICK_EVENTS)
            store.remove(KEY_PURCHASE_EVENTS)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    internal class EventEmitterWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(
        context,
        params
    ) {
        override suspend fun doWork(): Result {
            val aggregated = aggregateEvents()
            if (!aggregated.clicks.isNullOrEmpty() ||
                !aggregated.impressions.isNullOrEmpty() ||
                !aggregated.purchases.isNullOrEmpty()
            ) {
                try {
                    TopsortAnalyticsHttpService.service.reportEvent(aggregated)
                } catch(_: ExceptionInInitializerError) {
                    // ignored, occurs in testing when no http service is available
                } catch(ex: Exception){
                    return Result.retry()
                }

                Logger.log.add("uploading: ${aggregated.toJsonObject()}")

                clear()
                uploadQueued.set(false)
            }
            return Result.success()
        }
    }
}
