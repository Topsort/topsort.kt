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
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.EventType
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent
import org.json.JSONObject
import java.util.Locale

private const val PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE"
private const val ENCRYPTED_PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE_ENCRYPTED"

private const val KEY_TOKEN = "KEY_TOKEN"
private const val KEY_SESSION_ID = "KEY_SESSION_ID"
private const val KEY_RECORD = "KEY_RECORD_%d"
private const val KEY_RECORD_PREFIX = "KEY_RECORD_"
private const val KEY_RECENT_RECORD_ID = "KEY_RECORD_ID"

/** Mirrors the top-level key each event model serialises itself under. */
private val EVENT_TYPE_BY_JSON_KEY = mapOf(
    "impressions" to EventType.Impression,
    "clicks" to EventType.Click,
    "purchases" to EventType.Purchase,
    "pageviews" to EventType.PageView,
)
/**
 * Upper bound on undelivered records held on disk.
 *
 * Sized from measurement rather than taste: enumerating the cache decrypts every record, at roughly
 * 0.33ms per record on a mid-range device, so 5000 keeps a sweep's read under ~1.7s in the worst
 * case while sitting far above any healthy install. See INTE-2694 for removing the decryption cost
 * itself, after which this could go up.
 */
private const val MAX_CACHED_RECORDS = 5_000

private const val TAG = "TopsortCache"

/** An event sitting in the cache that has not been delivered. */
internal data class PendingRecord(
    val recordId: Long,
    val eventType: EventType,
)

internal object Cache {

    @Volatile
    private lateinit var applicationContext: Context

    // Volatile because initialize() reassigns these from WorkManager threads while other threads
    // read them. Per-record work units run in parallel, so there is no longer a single-worker
    // invariant making that safe.
    @Volatile
    private lateinit var preferences: SharedPreferences

    private var recentRecordId: Long = 0

    /**
     * Bearer token in effect. Persisted by [setup] rather than on assignment, so it is written in
     * the same editor as the opaque user id.
     */
    @Volatile
    var token: String = ""
        private set

    @Volatile
    private var opaqueUserId: String = ""

    /**
     * Synchronized for the same reason [setup] and [storeEvent] are, and it matters more than it
     * looks: this is the one mutator reached from WorkManager threads, via EventEmitterWorker's
     * init block. Under the old shared work chain at most one worker existed at a time, so an
     * unguarded initialize() could not interleave with anything. Per-record work units remove that
     * accident, and without the monitor a worker re-reading identity from disk can land in the
     * middle of a setup() that has already decided a different one, leaving the in-memory id blank
     * while disk holds the real one.
     *
     * Reentrant, so setup() calling this while holding the monitor is fine.
     */
    @Synchronized
    fun initialize(context: Context) {
        // Once per process. Without this guard every worker construction re-runs
        // MasterKeys.getOrCreate + EncryptedSharedPreferences.create + the migration check while
        // holding the monitor that storeEvent also needs - and storeEvent is reached from
        // report*() on the host's UI thread. Per-record work units mean several workers can be
        // constructed at once, so a burst serialised that keystore cost in front of the UI thread.
        // Under the old shared chain only one worker existed at a time and initialize took no lock,
        // so the cost was never on anyone's critical path.
        if (::preferences.isInitialized) return

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
     * in effect, which for [UserIdentity.Unidentified] is whatever was already persisted, or a
     * newly minted id when there is nothing to fall back on.
     */
    // Synchronized for the same reason storeEvent is: both write recentRecordId. Without it a
    // concurrent setup can roll the counter back to a stale on-disk value, after which storeEvent
    // overwrites an existing undelivered record and that event is lost silently.
    @Synchronized
    fun setup(
        context: Context,
        identity: UserIdentity,
        token: String
    ): String {
        initialize(context)

        recentRecordId = preferences.getLong(KEY_RECENT_RECORD_ID, 0)

        val resolved = resolveOpaqueUserId(identity)
        this.opaqueUserId = resolved.opaqueUserId
        this.token = token

        // One editor, so the two identity values cannot tear apart. Committed synchronously only
        // when a placeholder was just minted: its entire value is being stable across launches, so
        // losing it to a process death would mint a different id next time and break correlation.
        // The common path - an id supplied by the integrator - stays asynchronous.
        val editor = preferences
            .edit()
            .putString(KEY_SESSION_ID, resolved.opaqueUserId)
            .putString(KEY_TOKEN, token)
        if (resolved.wasGenerated) {
            editor.commit()
        } else {
            editor.apply()
        }

        return resolved.opaqueUserId
    }

    private data class ResolvedIdentity(val opaqueUserId: String, val wasGenerated: Boolean)

    /**
     * Decides which opaque user id to use.
     *
     * A [UserIdentity.Identified] id always wins, because audience matching requires the id to
     * correspond to the marketplace's records - an id minted here matches nothing. It is non-blank
     * by construction, so there is nothing to validate again at this point.
     *
     * [UserIdentity.Unidentified] never downgrades an identity we already hold,
     * marketplace-supplied or previously minted, so a caller who cannot name the user does not
     * cost us the id we already had. Only with nothing at all to fall back on do we mint one, so that events remain
     * reportable instead of being rejected by the API for a missing opaqueUserId. Calling setup
     * again with a [UserIdentity.Identified] id replaces it.
     */
    private fun resolveOpaqueUserId(identity: UserIdentity): ResolvedIdentity {
        // Exhaustive over a sealed subject, which the compiler enforces. A third case - the
        // logout/clear this type does not yet have - must not fall silently into the branch below,
        // which keeps the previous user's id and would attribute one person's events to another.
        when (identity) {
            is UserIdentity.Identified -> {
                // Identified.of rejects a blank id, but its constructor is internal, and Kotlin
                // emits internal constructors as public JVM methods - so Java can reach it, and so
                // can any future call site in this module that bypasses the factory. Falling
                // through rather than trusting the invariant keeps the one this cache actually
                // owes the wire: no event leaves with a blank opaqueUserId, which the API rejects.
                if (identity.id.isNotBlank()) {
                    return ResolvedIdentity(identity.id, wasGenerated = false)
                }
                Log.w(TAG, "Blank UserIdentity.Identified id; treating it as Unidentified")
            }
            UserIdentity.Unidentified -> Unit
        }

        val known = opaqueUserId
        if (known.isNotBlank()) {
            // Info, not a warning: Unidentified is a documented, deliberate choice, so an app that
            // legitimately has no id would otherwise warn on every cold start for doing as it was
            // told. The mint below stays at warning - that one is actionable.
            Log.i(TAG, "No opaqueUserId supplied; keeping the id already in effect")
            return ResolvedIdentity(known, wasGenerated = false)
        }

        Log.w(
            TAG,
            "No opaqueUserId supplied; generating a placeholder so events remain reportable. " +
                "Call setup again with UserIdentity.Identified once the marketplace's own id is " +
                "available, otherwise audience matching will not work for these events.",
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
    @Synchronized
    fun clearForTests() {
        preferences.edit().clear().commit()
        recentRecordId = 0
        token = ""
        opaqueUserId = ""
    }

    /**
     * Undelivered records, oldest first, at most [limit] of them.
     *
     * A record is only removed once a worker has actually run for it, so anything still here was
     * never delivered. Each record is read and parsed exactly once - the event type and the
     * timestamp both come out of that single pass, because reading a record decrypts it.
     *
     * [limit] bounds how many records are parsed and re-enqueued per sweep. It does NOT bound
     * decryption: enumeration goes through the preferences map, and an encrypted store decrypts
     * every entry to build it, so the read cost is proportional to the whole backlog regardless of
     * this value. Measured at ~73ms for 200 records on a mid-range device, and it scales with the
     * backlog - which is worst on exactly the installs this sweep exists for. Fixing that needs an
     * enumeration path that does not decrypt; tracked in INTE-2694.
     *
     * A record whose JSON cannot be parsed is skipped on its own rather than aborting the
     * enumeration. Note this does not extend to a record that cannot be DECRYPTED: that failure is
     * raised while building the preferences map, before any per-record handling is reachable, and
     * it disables recovery for the whole install. Tracked in INTE-2694 with the cost issue above.
     */
    @Suppress("TooGenericExceptionCaught")
    fun pendingRecords(limit: Int): List<PendingRecord> =
        pendingRecords(limit, MAX_CACHED_RECORDS)

    /** [pendingRecords] with the capacity bound injected, so tests need not write MAX records. */
    @VisibleForTesting
    fun pendingRecordsForTest(limit: Int, capacity: Int): List<PendingRecord> =
        pendingRecords(limit, capacity)

    private fun pendingRecords(limit: Int, capacity: Int): List<PendingRecord> {
        val all = try {
            cachedRecordIds()
        } catch (e: Exception) {
            Log.e(TAG, "Could not enumerate cached records", e)
            return emptyList()
        }

        // Enforce the capacity bound off the enumeration we already paid for. Doing it as a
        // separate call would double the cost of a sweep, and the enumeration - not the delete - is
        // the expensive half: the delete is one editor and one commit however many records go.
        //
        // This is a resource bound, not a judgement about whether an event is still worth sending.
        // The SDK cannot make that judgement: whether a late event still attributes depends on the
        // marketplace's attribution window, and whether it is still billable depends on the
        // campaign's charge type - a CPM impression is chargeable long after it can attribute.
        // Both facts live server-side, so nothing is discarded here for being old.
        val ids = if (all.size > capacity) {
            val excess = all.take(all.size - capacity)
            discardAll(excess, DiscardReason.CACHE_OVER_CAPACITY)
            all.drop(excess.size).take(limit)
        } else {
            all.take(limit)
        }

        val records = mutableListOf<PendingRecord>()
        val uninterpretable = mutableListOf<Long>()
        ids.forEach { recordId ->
            val record = try {
                parseRecord(recordId)
            } catch (e: Exception) {
                Log.e(TAG, "Cached record $recordId cannot be read", e)
                null
            }
            if (record != null) records += record else uninterpretable += recordId
        }

        // A record whose event type cannot be determined can never be sent - nothing knows which
        // endpoint it belongs to. Skipping it without removing it would leave it to be re-read and
        // re-decrypted by every sweep for the lifetime of the install. This is the same call the
        // worker makes when a cached body will not parse back into an event.
        discardAll(uninterpretable, DiscardReason.UNKNOWN_EVENT_TYPE)

        return records
    }

    /**
     * Reads a record once and pulls out both the event type, from its top-level JSON key, and when
     * its first event occurred. Null when the record is absent or its shape is unrecognised.
     */
    private fun parseRecord(recordId: Long): PendingRecord? {
        val json = readEvent(recordId) ?: return null
        val obj = JSONObject(json)
        val key = EVENT_TYPE_BY_JSON_KEY.keys.firstOrNull { obj.has(it) } ?: return null
        return PendingRecord(recordId, EVENT_TYPE_BY_JSON_KEY.getValue(key))
    }

    /**
     * Record ids currently held in the cache, oldest first.
     *
     * Production surface: [pendingRecords] enumerates through this, so the sweep depends on it. The
     * delivery tests also assert on it to see whether a record survived its send.
     *
     * Note this reads [preferences] as a whole, which on an encrypted store decrypts every entry -
     * see the cost note on [pendingRecords].
     */
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

    /**
     * Removes several records in one editor, so pruning a backlog is one synchronous write rather
     * than one per record.
     */
    /**
     * Why a cached event was thrown away without being delivered.
     *
     * Every discard is data the marketplace will never see, so the reason travels with the record
     * id rather than being buried in a log string. Delivery is deliberately not one of these -
     * [deleteEvent] after a successful send is a completion, not a loss, and conflating the two
     * would make the discard count useless.
     */
    internal enum class DiscardReason {
        /** Evicted to keep the cache under MAX_CACHED_RECORDS. */
        CACHE_OVER_CAPACITY,

        /** The cached body will not parse back into an event, so nothing can ever send it. */
        UNPARSEABLE_BODY,

        /** The API rejected it with a 4xx; retrying the same body would be rejected again. */
        PERMANENTLY_REJECTED,

        /** The record's event type cannot be determined, so nothing knows where to send it. */
        UNKNOWN_EVENT_TYPE,
    }

    /**
     * The single exit for an undelivered event.
     *
     * All discards route through here so that "what can destroy an event, and why" is answerable by
     * reading one function, and so that adding a real signal later - a host-app callback, a counter
     * piggybacked on the next successful send - is one change rather than four.
     */
    fun discard(recordId: Long, reason: DiscardReason, detail: String? = null) {
        Log.e(TAG, "Discarding record $recordId: $reason${detail?.let { " ($it)" } ?: ""}")
        deleteEvent(recordId)
    }

    /** [discard] for a batch, in one editor rather than one synchronous write per record. */
    fun discardAll(recordIds: Collection<Long>, reason: DiscardReason) {
        if (recordIds.isEmpty()) return
        Log.e(TAG, "Discarding ${recordIds.size} record(s): $reason - ids=$recordIds")
        deleteEvents(recordIds)
    }

    fun deleteEvents(recordIds: Collection<Long>) {
        if (recordIds.isEmpty()) return
        val editor = preferences.edit()
        recordIds.forEach { editor.remove(recordKey(it)) }
        editor.commit()
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
