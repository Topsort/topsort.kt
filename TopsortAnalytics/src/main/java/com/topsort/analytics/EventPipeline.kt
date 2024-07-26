package com.topsort.analytics

import android.content.Context
import android.text.TextUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.JsonSerializable
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import kotlinx.coroutines.CompletionHandlerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

private const val PREFERENCES_NAME = "topsort_event_cache_async"

private val KEY_IMPRESSION_EVENTS= stringPreferencesKey("KEY_IMPRESSION_EVENTS")
private val KEY_CLICK_EVENTS = stringPreferencesKey("KEY_CLICK_EVENTS")
private val KEY_PURCHASE_EVENTS = stringPreferencesKey("KEY_PURCHASE_EVENTS")

val Context.eventDatastore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

internal object EventPipeline {

    private lateinit var applicationContext: Context

    private val scope = CoroutineScope(SupervisorJob())
    private val dispatcher = Dispatchers.IO

    private val writeChannelImpressions = Channel<List<Impression>>()
    private val writeChannelClicks = Channel<List<Click>>()
    private val writeChannelPurchases = Channel<List<Purchase>>()

    private val uploadChannel = Channel<String>()


    fun setup(
        context: Context,
    ) {
        initialize(context)

        launchWriteLoop(writeChannelImpressions, KEY_IMPRESSION_EVENTS)
        launchWriteLoop(writeChannelClicks, KEY_CLICK_EVENTS)
        launchWriteLoop(writeChannelPurchases, KEY_PURCHASE_EVENTS)

        launchUploadLoop()
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent
    ) {
        writeChannelImpressions.trySend(impressionEvent.impressions)
    }

    fun storeClick(
        clickEvent: ClickEvent
    ) {
        writeChannelClicks.trySend(clickEvent.clicks)
    }

    fun storePurchase(
        purchaseEvent: PurchaseEvent
    ) {
        writeChannelPurchases.trySend(purchaseEvent.purchases)
    }

    fun upload(){
        uploadChannel.trySend("UPLOAD")
    }

    private fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private fun launchUploadLoop() = scope.launch(dispatcher) {
        uploadChannel.consumeEach { _ ->
            val data = applicationContext.eventDatastore.data.first()
            val impressions = data[KEY_IMPRESSION_EVENTS]?.trim(',')
            val clicks = data[KEY_CLICK_EVENTS]?.trim(',')
            val purchases = data[KEY_PURCHASE_EVENTS]?.trim(',')

            applicationContext.eventDatastore.edit { store ->
                store.remove(KEY_IMPRESSION_EVENTS)
                store.remove(KEY_CLICK_EVENTS)
                store.remove(KEY_IMPRESSION_EVENTS)
            }

            val aggregated = StringBuilder()
            aggregated.append("""{""")
            impressions?.let { aggregated.append(""""impressions":[$it],""".trimMargin()) }
            clicks?.let { aggregated.append(""""clicks":[$it],""".trimMargin()) }
            purchases?.let { aggregated.append(""""purchases":[$it],""".trimMargin()) }
            aggregated.trim(',')
            aggregated.append("""}""")

            // Actually send
            TopsortAnalyticsHttpService.service.reportAggregated(aggregated.toString())
        }
    }

    private fun <T : JsonSerializable> launchWriteLoop(
        channel: Channel<List<T>>,
        key: Preferences.Key<String>
    ) =
        scope.launch(dispatcher) {
            for (events in channel) {
                val json = StringBuilder()
                for (event in events) {
                    json.append(event.toJsonObject().toString())
                    json.append(",")
                }

                applicationContext.eventDatastore.edit { store ->
                    store[key] = store[key] + json.toString()
                }
            }
        }

}
