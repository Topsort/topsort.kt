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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

private const val preferencesName = "TOPSORT_EVENTS_CACHE"

private val KEY_TOKEN = stringPreferencesKey("KEY_TOKEN")
private val KEY_OPAQUE_USER_ID = stringPreferencesKey("KEY_OPAQUE_USER_ID")
private const val KEY_RECORD_FORMAT = "KEY_RECORD_%d"
private val KEY_RECENT_RECORD_ID = longPreferencesKey("KEY_RECORD_ID")

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

internal object CacheAsync {

    private lateinit var applicationContext: Context

    private var recentRecordId: Long = 0

    private var token: String = ""
    fun getToken() = token

    private suspend fun setToken(value: String) {
       token = value
       applicationContext.dataStore.edit { store ->
           store[KEY_TOKEN] = value
       }
    }

    private var opaqueUserId: String = ""
    fun getOpaqueUserId() = opaqueUserId
    private suspend fun setOpaqueUserId(value: String){
        opaqueUserId = value
        applicationContext.dataStore.edit { store ->
            store[KEY_OPAQUE_USER_ID] = value
        }
    }

    private fun initialize(context: Context) {
        applicationContext = context.applicationContext

        runBlocking {
            val store = applicationContext.dataStore.data.first()
            token = store[KEY_TOKEN] ?: ""
            opaqueUserId = store[KEY_OPAQUE_USER_ID] ?: ""
        }
    }

    fun setup(
        context: Context,
        opaqueUserId: String,
        token: String
    ) {
        initialize(context)

        runBlocking {
            val store = applicationContext.dataStore.data.first()
            recentRecordId = store[KEY_RECENT_RECORD_ID] ?: 0L
        }

        this.opaqueUserId = opaqueUserId
        this.token = token
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
        applicationContext.dataStore.edit { store ->
            store.remove(recordKey(recordId))
        }
    }

    private suspend fun readEvent(recordId: Long): String? {
        val json = applicationContext.dataStore.data.first()[recordKey(recordId)] ?: ""
        if (TextUtils.isEmpty(json)) {
            return null
        }
        return json
    }

    private suspend fun storeEvent(json: String): Long {
        val recordId = nextRecordKey()
        applicationContext.dataStore.edit { store ->
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

        applicationContext.dataStore.edit { store ->
            store[KEY_RECENT_RECORD_ID] = recentRecordId
        }

        return recentRecordKey
    }
}
