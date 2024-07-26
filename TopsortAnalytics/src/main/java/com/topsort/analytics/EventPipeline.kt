package com.topsort.analytics

import android.content.Context
import android.text.TextUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

private const val PREFERENCES_NAME = "topsort_event_cache_async"
private const val KEY_RECORD_FORMAT = "KEY_RECORD_%d"

private val KEY_IMPRESSION_EVENTS= stringPreferencesKey("KEY_IMPRESSION_EVENTS")
private val KEY_CLICK_EVENTS = stringPreferencesKey("KEY_CLICK_EVENTS")
private val KEY_PURCHASE_EVENTS = stringPreferencesKey("KEY_PURCHASE_EVENTS")

val Context.eventDatastore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

internal object EventPipeline {

    private lateinit var applicationContext: Context

    private var recentRecordId: Long = 0

    private val scope = CoroutineScope(SupervisorJob())
    private val dispatcher = Dispatchers.IO

    private val writeChannelImpressions = Channel<List<Impression>>()
    private val writeChannelClicks = Channel<Click>()
    private val writeChannelPurchases = Channel<Purchase>()

    private val uploadChannel = Channel<String>()

    private fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private fun uploadLoop() = scope.launch(dispatcher) {
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

        }
    }

    private fun writeLoopImpressions() = scope.launch(dispatcher) {
        for(impressions in writeChannelImpressions){
            val json = StringBuilder()
            for(impression in impressions) {
                json.append(impression.toJsonObject().toString())
                json.append(",")
            }

            applicationContext.eventDatastore.edit { store ->
                store[KEY_IMPRESSION_EVENTS] = store[KEY_IMPRESSION_EVENTS] + json.toString()
            }
        }
    }

    fun setup(
        context: Context,
    ) {
        initialize(context)

        writeLoopImpressions()
        writeLoopImpressions()
        uploadLoop()
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent
    ) {
        writeChannelImpressions.trySend(impressionEvent.impressions)
    }

    suspend fun storeClick(
        clickEvent: ClickEvent
    ): Long {
        val json = clickEvent.toJsonObject().toString()
        return storeEvent(json)
    }

    suspend fun readClick(recordId: Long): ClickEvent? {
        return ClickEvent.fromJson(readEvent(recordId))
    }

    suspend fun storePurchase(
        purchaseEvent: PurchaseEvent
    ): Long {
        val json = purchaseEvent.toJsonObject().toString()
        return storeEvent(json)
    }

    suspend fun readPurchase(recordId: Long): PurchaseEvent? {
        return PurchaseEvent.fromJson(readEvent(recordId))
    }

    suspend fun deleteEvent(recordId: Long) {
        applicationContext.eventDatastore.edit { store ->
            store.remove(recordKey(recordId))
        }
    }

    private suspend fun readEvent(recordId: Long): String? {
        val json = applicationContext.eventDatastore.data.first()[recordKey(recordId)] ?: ""
        if (TextUtils.isEmpty(json)) {
            return null
        }
        return json
    }

    private suspend fun storeEvent(json: String): Long {
        val recordId = nextRecordKey()
        applicationContext.eventDatastore.edit { store ->
            store[recordId].plus(json)
            store[recordId] = json
        }

        return recentRecordId
    }

    private fun recordKey(recordId: Long) = stringPreferencesKey(
        String.format(
            Locale.ENGLISH,
            KEY_RECORD_FORMAT,
            recordId
        )
    )

    private suspend fun nextRecordKey(): Preferences.Key<String> {
        recentRecordId = if (recentRecordId < Long.MAX_VALUE) {
            recentRecordId + 1
        } else {
            0
        }

        val recentRecordKey = recordKey(recentRecordId)

//        applicationContext.eventDatastore.edit { store ->
//            store[KEY_RECENT_RECORD_ID] = recentRecordId
//        }

        return recentRecordKey
    }
}
