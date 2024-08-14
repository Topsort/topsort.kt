package com.topsort.analytics

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent
import java.util.Locale

private const val PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE"

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

    private var opaqueUserId: String = ""
        set(value) {
            field = value
            preferences
                .edit()
                .putString(KEY_SESSION_ID, value)
                .apply()
        }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        token = preferences.getString(KEY_TOKEN, "")!!
        opaqueUserId = preferences.getString(KEY_SESSION_ID, "")!!
    }

    fun setup(
        context: Context,
        opaqueUserId: String,
        token: String
    ) {
        initialize(context)

        recentRecordId = preferences.getLong(KEY_RECENT_RECORD_ID, 0)
        this.opaqueUserId = opaqueUserId
        this.token = token
    }

    fun storeImpression(
        impressionEvent: ImpressionEvent
    ): Long {
        val json = impressionEvent.toJsonObject().toString()
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readImpression(recordId: Long): ImpressionEvent? {
        return ImpressionEvent.fromJson(readEvent(recordId))
    }

    fun storeClick(
        clickEvent: ClickEvent
    ): Long {
        val json = clickEvent.toJsonObject().toString()
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readClick(recordId: Long): ClickEvent? {
        return ClickEvent.fromJson(readEvent(recordId))
    }

    fun storePurchase(
        purchaseEvent: PurchaseEvent
    ): Long {
        val json = purchaseEvent.toJsonObject().toString()
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readPurchase(recordId: Long): PurchaseEvent? {
        return PurchaseEvent.fromJson(readEvent(recordId))
    }

    fun deleteEvent(recordId: Long) {
        preferences
            .edit()
            .remove(recordKey(recordId))
            .apply()
    }

    private fun readEvent(recordId: Long): String? {
        val json = preferences.getString(recordKey(recordId), "")
        if (TextUtils.isEmpty(json)) {
            return null
        }

        return json
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
