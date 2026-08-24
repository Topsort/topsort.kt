package com.topsort.analytics

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent
import java.util.Locale

private const val PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE"
private const val ENCRYPTED_PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE_ENCRYPTED"

private const val KEY_TOKEN = "KEY_TOKEN"
private const val KEY_SESSION_ID = "KEY_SESSION_ID"
private const val KEY_RECORD = "KEY_RECORD_%d"
private const val KEY_RECORD_PREFIX = "KEY_RECORD_"
private const val KEY_RECENT_RECORD_ID = "KEY_RECORD_ID"


private const val TAG = "TopsortCache"

internal object Cache {

    private lateinit var applicationContext: Context
    private lateinit var preferences: SharedPreferences

    private var recentRecordId: Long = 0

    /**
     * Bearer token in effect. Persisted by [setup] rather than on assignment, so it is written in
     * the same editor as the opaque user id.
     */
    var token: String = ""
        private set

    private var opaqueUserId: String = ""

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        preferences = createEncryptedPreferences(applicationContext)

        migrateFromPlaintextPreferences(applicationContext)

        token = preferences.getString(KEY_TOKEN, "")!!
        opaqueUserId = preferences.getString(KEY_SESSION_ID, "")!!
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFERENCES_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted preferences, falling back to plaintext", e)
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun migrateFromPlaintextPreferences(context: Context) {
        val plaintext = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        // Skip migration if encryption failed and we fell back to the same plaintext prefs.
        // Clearing would wipe all data since both references point to the same instance.
        if (plaintext === preferences) return

        val plaintextToken = plaintext.getString(KEY_TOKEN, "")
        if (plaintextToken.isNullOrEmpty()) return

        try {
            val editor = preferences.edit()
            for ((key, value) in plaintext.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Long -> editor.putLong(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                }
            }
            editor.apply()
            plaintext.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate plaintext preferences", e)
        }
    }

    /**
     * Initialises the cache and stores the session identity. Returns the opaque user id actually
     * in effect, which may differ from [opaqueUserId] when that value is blank.
     */
    // Synchronized for the same reason storeEvent is: both write recentRecordId. Without it a
    // concurrent setup can roll the counter back to a stale on-disk value, after which storeEvent
    // overwrites an existing undelivered record and that event is lost silently.
    @Synchronized
    fun setup(
        context: Context,
        opaqueUserId: String,
        token: String
    ): String {
        initialize(context)

        recentRecordId = preferences.getLong(KEY_RECENT_RECORD_ID, 0)

        val identity = resolveOpaqueUserId(opaqueUserId)
        this.opaqueUserId = identity.opaqueUserId
        this.token = token

        // One editor, so the two identity values cannot tear apart. Committed synchronously only
        // when a placeholder was just minted: its entire value is being stable across launches, so
        // losing it to a process death would mint a different id next time and break correlation.
        // The common path - an id supplied by the integrator - stays asynchronous.
        val editor = preferences
            .edit()
            .putString(KEY_SESSION_ID, identity.opaqueUserId)
            .putString(KEY_TOKEN, token)
        if (identity.wasGenerated) {
            editor.commit()
        } else {
            editor.apply()
        }

        return identity.opaqueUserId
    }

    private data class ResolvedIdentity(val opaqueUserId: String, val wasGenerated: Boolean)

    /**
     * Decides which opaque user id to use.
     *
     * The marketplace's own identifier always wins when one is supplied, because audience matching
     * requires the id to correspond to the marketplace's records - an id minted here matches
     * nothing. A blank value therefore never overwrites an id we already hold, and only when there
     * is nothing at all to fall back on do we generate a placeholder, so that events remain
     * reportable instead of being rejected by the API for a missing opaqueUserId. Calling setup
     * again with a real id replaces the placeholder.
     */
    private fun resolveOpaqueUserId(supplied: String): ResolvedIdentity {
        if (supplied.isNotBlank()) return ResolvedIdentity(supplied, wasGenerated = false)

        val known = opaqueUserId
        if (known.isNotBlank()) {
            Log.w(TAG, "Blank opaqueUserId supplied; keeping the previously supplied one")
            return ResolvedIdentity(known, wasGenerated = false)
        }

        Log.w(
            TAG,
            "No opaqueUserId supplied; generating a placeholder so events remain reportable. " +
                "Call setup again with the marketplace's own id once it is available, " +
                "otherwise audience matching will not work for these events.",
        )
        return ResolvedIdentity(randomId(), wasGenerated = true)
    }

    fun storeImpression(impressionEvent: ImpressionEvent): Long =
        storeEvent(impressionEvent.toJsonObject().toString())

    fun readImpression(recordId: Long): ImpressionEvent? {
        return ImpressionEvent.fromJson(readEvent(recordId))
    }

    fun storeClick(clickEvent: ClickEvent): Long =
        storeEvent(clickEvent.toJsonObject().toString())

    fun readClick(recordId: Long): ClickEvent? {
        return ClickEvent.fromJson(readEvent(recordId))
    }

    fun storePurchase(purchaseEvent: PurchaseEvent): Long =
        storeEvent(purchaseEvent.toJsonObject().toString())

    fun readPurchase(recordId: Long): PurchaseEvent? {
        return PurchaseEvent.fromJson(readEvent(recordId))
    }

    fun storePageView(pageViewEvent: PageViewEvent): Long =
        storeEvent(pageViewEvent.toJsonObject().toString())

    fun readPageView(recordId: Long): PageViewEvent? {
        return PageViewEvent.fromJson(readEvent(recordId))
    }

    /**
     * Drops every cached record and the stored session identity, in memory as well as on disk.
     * Clearing only the preferences would leave stale values behind for a caller that does not
     * re-initialize, so the in-memory fields are reset too. Test-only.
     */
    @VisibleForTesting
    fun clearForTests() {
        preferences.edit().clear().commit()
        recentRecordId = 0
        token = ""
        opaqueUserId = ""
    }

    /**
     * Record ids currently held in the cache. Test-only; the delivery tests assert on whether a
     * record survived its send.
     */
    @VisibleForTesting
    fun cachedRecordIds(): List<Long> =
        preferences.all.keys.mapNotNull(::recordIdOrNull).sorted()

    /**
     * Parses a record id out of a preferences key, or null when the key is not a record. Note that
     * KEY_RECORD_ID shares the record key prefix, so the suffix has to be numeric.
     */
    private fun recordIdOrNull(key: String): Long? {
        if (!key.startsWith(KEY_RECORD_PREFIX)) return null
        return key.removePrefix(KEY_RECORD_PREFIX).toLongOrNull()
    }

    /**
     * Removes a delivered record. Written synchronously: this runs on a worker thread, and if the
     * process dies before an async write lands the record survives and the next sweep delivers the
     * same event again - which the events API would count twice, since it does not de-duplicate on
     * event id.
     */
    fun deleteEvent(recordId: Long) {
        preferences
            .edit()
            .remove(recordKey(recordId))
            .commit()
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

    /**
     * Writes an event and the record counter in a single editor so the two cannot tear apart.
     *
     * The counter used to be committed synchronously before the event was applied asynchronously,
     * so a process death in between left the counter advanced with no record behind it. The worker
     * enqueued for that id then found nothing and dropped the event silently.
     */
    @Synchronized
    private fun storeEvent(json: String): Long {
        val recordId = if (recentRecordId < Long.MAX_VALUE) recentRecordId + 1 else 0

        preferences
            .edit()
            .putString(recordKey(recordId), json)
            .putLong(KEY_RECENT_RECORD_ID, recordId)
            .apply()

        recentRecordId = recordId
        return recordId
    }
}
