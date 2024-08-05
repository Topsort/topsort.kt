package com.topsort.analytics

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.JsonSerializable
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray

private const val PREFERENCES_NAME = "topsort_event_cache_async"

private val KEY_IMPRESSION_EVENTS= stringPreferencesKey("KEY_IMPRESSION_EVENTS")
private val KEY_CLICK_EVENTS = stringPreferencesKey("KEY_CLICK_EVENTS")
private val KEY_PURCHASE_EVENTS = stringPreferencesKey("KEY_PURCHASE_EVENTS")

val Context.eventDatastore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

internal object EventPipeline {

    private lateinit var applicationContext: Context

    private val scope = CoroutineScope(SupervisorJob())
    private val dispatcher = Dispatchers.IO

    private val uploadChannel = Channel<String>()


    fun setup(
        context: Context,
    ) {
        initialize(context)

        launchUploadLoop()
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent
    ) = asyncWrite(impressionEvent.impressions, KEY_IMPRESSION_EVENTS)

    fun storeClick(
        clickEvent: ClickEvent
    ) = asyncWrite(clickEvent.clicks, KEY_CLICK_EVENTS)

    fun storePurchase(
        purchaseEvent: PurchaseEvent
    ) = asyncWrite(purchaseEvent.purchases, KEY_PURCHASE_EVENTS)

    fun upload() {
        uploadChannel.trySend("UPLOAD")
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
    }

    private fun launchUploadLoop() = scope.launch(dispatcher) {
        for (a in uploadChannel) {
            val aggregated = aggregateEvents()

            // Actually send
            //TopsortAnalyticsHttpService.service.reportEvent(aggregated)
            println("uploading: ${aggregated.toJsonObject()}")

            clear()
        }
    }

    private fun <T : JsonSerializable> asyncWrite(
        events: List<T>,
        key: Preferences.Key<String>
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
        }

    private suspend fun aggregateEvents(): Event {
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
}
