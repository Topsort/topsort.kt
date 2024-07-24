package com.topsort.analytics

import android.content.Context
import android.text.TextUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

private const val PREFERENCES_NAME = "topsort_event_cache_async"

private const val KEY_RECORD_FORMAT = "KEY_RECORD_%d"
private val KEY_RECENT_RECORD_ID = longPreferencesKey("KEY_RECORD_ID")

val Context.eventDatastore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

internal object EventCache {

    private lateinit var applicationContext: Context

    private var recentRecordId: Long = 0

    private fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun setup(
        context: Context,
    ) {
        initialize(context)

        runBlocking {
            val store = applicationContext.eventDatastore.data.first()
            recentRecordId = store[KEY_RECENT_RECORD_ID] ?: 0L
        }
    }

    suspend fun storeImpression(
        impressionEvent: ImpressionEvent
    ): Long {
        val json = impressionEvent.toJsonObject().toString()
        return storeEvent(json)
    }

    suspend fun readImpression(recordId: Long): ImpressionEvent? {
        return ImpressionEvent.fromJson(readEvent(recordId))
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

        applicationContext.eventDatastore.edit { store ->
            store[KEY_RECENT_RECORD_ID] = recentRecordId
        }

        return recentRecordKey
    }
}
