package com.topsort.analytics

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.Locale

private const val preferencesName = "TOPSORT_EVENTS_CACHE"

private const val KEY_TOKEN = "KEY_TOKEN"
private const val KEY_SESSION_ID = "KEY_SESSION_ID"
private const val KEY_RECORD = "KEY_RECORD_%d"
private const val KEY_RECENT_RECORD_ID = "KEY_RECORD_ID"

internal object Cache {

    private lateinit var applicationContext: Context
    private lateinit var preferences: SharedPreferences

    private var recentRecordId: Long = 0

    var token: String = ""
        set(value) {
            field = value
            preferences
                .edit()
                .putString(KEY_TOKEN, value)
                .apply()
        }

    private var sessionId: String = ""
        set(value) {
            field = value
            preferences
                .edit()
                .putString(KEY_SESSION_ID, value)
                .apply()
        }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        preferences = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

        token = preferences.getString(KEY_TOKEN, "")!!
        sessionId = preferences.getString(KEY_SESSION_ID, "")!!
    }

    fun setup(
        context: Context,
        sessionId: String,
        token: String
    ) {
        initialize(context)

        recentRecordId = preferences.getLong(KEY_RECENT_RECORD_ID, 0)
        this.sessionId = sessionId
        this.token = token
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent
    ): Long {
        val json = Json.encodeToString(impressionEvent)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readImpression(recordId: Long): ImpressionEvent? {
        return readEvent(recordId)
    }

    fun storeClick(
        clickEvent: ClickEvent
    ): Long {
        val json = Json.encodeToString(clickEvent)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readClick(recordId: Long): ClickEvent? {
        return readEvent(recordId)
    }

    fun storePurchase(
        purchaseEvent: PurchaseEvent
    ): Long {
        val json = Json.encodeToString(purchaseEvent)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readPurchase(recordId: Long): PurchaseEvent? {
        return readEvent(recordId)
    }

    fun deleteEvent(recordId: Long) {
        preferences
            .edit()
            .remove(recordKey(recordId))
            .apply()
    }

    private inline fun <reified T> readEvent(recordId: Long): T? {
        val json = preferences.getString(recordKey(recordId), "")
        if (json == null || TextUtils.isEmpty(json)) {
            return null
        }

        return try {
            Json.decodeFromString<T>(json)
        } catch (e: IOException) {
            null
        }
    }

    private fun recordKey(recordId: Long) = String.format(
        Locale.ENGLISH,
        KEY_RECORD,
        recordId
    )

    private fun nextRecordKey(): String {
        recentRecordId = if (recentRecordId < Long.MAX_VALUE) {
            recentRecordId + 1
        } else {
            0
        }

        preferences
            .edit()
            .putLong(KEY_RECENT_RECORD_ID, recentRecordId)
            .apply()
        return recordKey(recentRecordId)
    }
}
