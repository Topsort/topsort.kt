package com.topsort.analytics

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent
import java.lang.reflect.Type
import java.util.*

private const val preferencesName = "TOPSORT_EVENTS_CACHE"

private val TYPE_IMPRESSION = object : TypeToken<ImpressionEvent>() {}.type
private val TYPE_CLICK = object : TypeToken<ClickEvent>() {}.type
private val TYPE_PURCHASE = object : TypeToken<PurchaseEvent>() {}.type

private const val KEY_TOKEN = "KEY_TOKEN"
private const val KEY_SESSION_ID = "KEY_SESSION_ID"
private const val KEY_RECORD = "KEY_RECORD_%d"
private const val KEY_RECENT_RECORD_ID = "KEY_RECORD_ID"

internal object Cache {

    private lateinit var applicationContext: Context
    private lateinit var preferences: SharedPreferences

    private val gson = Gson()
    private var recentRecordId: Long = 0

    var token: String = ""
        set(value) {
            field = value
            preferences
                .edit()
                .putString(KEY_TOKEN, value)
                .apply()
        }

    var sessionId: String = ""
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
        val json = gson.toJson(impressionEvent, TYPE_IMPRESSION)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readImpression(recordId: Long): ImpressionEvent? {
        return readEvent(recordId, TYPE_IMPRESSION)
    }

    fun storeClick(
        clickEvent: ClickEvent
    ): Long {
        val json = gson.toJson(clickEvent, TYPE_CLICK)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readClick(recordId: Long): ClickEvent? {
        return readEvent(recordId, TYPE_CLICK)
    }

    fun storePurchase(
        purchaseEvent: PurchaseEvent
    ): Long {
        val json = gson.toJson(purchaseEvent, TYPE_PURCHASE)
        preferences
            .edit()
            .putString(nextRecordKey(), json)
            .apply()

        return recentRecordId
    }

    fun readPurchase(recordId: Long): PurchaseEvent? {
        return readEvent(recordId, TYPE_PURCHASE)
    }

    fun deleteEvent(recordId: Long) {
        preferences
            .edit()
            .remove(recordKey(recordId))
            .apply()
    }

    private fun <T> readEvent(recordId: Long, type: Type): T? {
        val json = preferences.getString(recordKey(recordId), "")
        if (TextUtils.isEmpty(json)) {
            return null
        }

        return try {
            gson.fromJson<T>(json, type)
        } catch (e: JsonSyntaxException) {
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
