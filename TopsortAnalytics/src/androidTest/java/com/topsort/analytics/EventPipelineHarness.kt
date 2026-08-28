package com.topsort.analytics

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import com.topsort.analytics.worker.EventEmitterWorker
import java.util.UUID
import org.json.JSONObject

/**
 * Shared instrumentation for exercising the event pipeline: Analytics -> Cache -> WorkManager ->
 * TopsortAnalyticsHttpService.
 *
 * Two things make the pipeline awkward to test directly, and this harness handles both.
 *
 * WorkManager is initialised through [WorkManagerTestInitHelper] with a [SynchronousExecutor], so
 * unconstrained work runs inline on enqueue. Note that work carrying constraints stays ENQUEUED or
 * BLOCKED under test until a `TestDriver` releases it, which is deliberate here: the states a
 * request lands in are themselves the thing worth asserting, and reaching them needs no network.
 *
 * The HTTP layer is replaced with [FakeAnalyticsHttpService] so a test can script exact status
 * codes - a 4xx is a permanent failure to the worker, a 5xx a transient one - without touching the
 * network.
 */
internal object EventPipelineHarness {

    const val TOKEN = "test-token"
    const val OPAQUE_USER_ID = "test-opaque-user-id"

    val application: Application get() = ApplicationProvider.getApplicationContext()

    val context: Context get() = application

    private var installed = false

    /** Bound on [runPendingEventWork]'s drain loop, so a stuck chain fails instead of hanging. */
    private const val MAX_DRAIN_PASSES = 20

    /**
     * Puts the pipeline into a known state: synchronous WorkManager, scripted HTTP, empty cache.
     * Returns the fake so the test can script responses and read what was sent.
     */
    fun install(): FakeAnalyticsHttpService {
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        // WorkManager first, then the cache: clearing the cache resets its record id counter, so a
        // stale work DB could collide with recycled ids.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)

        val fake = FakeAnalyticsHttpService()
        TopsortAnalyticsHttpService.setMockService(fake)

        Cache.initialize(context)
        Cache.clearForTests()
        // Reported bids are process-wide and outlive a test, and Analytics.setup() only clears
        // them when the user changes. Without this a bid id reused by another test is silently
        // deduplicated away, and the test that reports it sees fewer records than it wrote.
        ReportedBids.clear()
        installed = true

        return fake
    }

    /**
     * Undoes [install]. Safe to call when [install] never ran or threw partway: Cache holds its
     * preferences in a lateinit, so clearing it uninitialized would throw from an @After block and
     * bury the real failure under an unrelated one.
     */
    fun uninstall() {
        TopsortAnalyticsHttpService.resetToDefaultService()
        if (installed) {
            Cache.clearForTests()
            installed = false
        }
    }

    /**
     * Writes a record straight into the cache's own store, bypassing Analytics, so a test can set
     * up a corrupt entry that the public reporting API cannot produce - it only ever serialises
     * well-formed events.
     *
     * The preferences name, master key and encryption schemes are duplicated from [Cache] on
     * purpose: the point is to write to the same file Cache reads. That coupling is silent if it
     * ever breaks - Cache falls back to plaintext preferences when encryption is unavailable, and
     * this would then be writing to a different file entirely - so the plant is verified through
     * Cache itself before the test proceeds.
     */
    fun plantRawRecord(recordId: Long, json: String) {
        rawPreferences().edit().putString(rawRecordKey(recordId), json).commit()
        check(recordId in Cache.cachedRecordIds()) {
            "planted record $recordId is not visible to Cache - the test is writing to a " +
                "different store than the one under test"
        }
    }

    /**
     * Overwrites the `occurredAt` of the first event in an existing record, leaving the rest of the
     * body exactly as the library wrote it.
     *
     * Corrupting a real record rather than hand-writing one matters: a hand-written body is missing
     * fields that `fromJson` requires, so it fails to deserialise for reasons that have nothing to
     * do with the timestamp, and the test would pass or fail for the wrong reason.
     */
    fun corruptOccurredAt(recordId: Long, replacement: String) {
        val prefs = rawPreferences()
        val key = rawRecordKey(recordId)
        val body = JSONObject(requireNotNull(prefs.getString(key, null)) { "no record $recordId" })
        val arrayKey = body.keys().asSequence().first()
        body.getJSONArray(arrayKey).getJSONObject(0).put("occurredAt", replacement)
        prefs.edit().putString(key, body.toString()).commit()
    }

    private fun rawRecordKey(recordId: Long) = "KEY_RECORD_$recordId"

    private fun rawPreferences(): SharedPreferences =
        EncryptedSharedPreferences.create(
            "TOPSORT_EVENTS_CACHE_ENCRYPTED",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private fun workManager(): WorkManager = WorkManager.getInstance(context)

    /**
     * Every [WorkInfo] for event delivery, found by tag rather than by unique work name so that
     * assertions do not depend on how the work happens to be scheduled.
     */
    fun eventWork(): List<WorkInfo> =
        workManager().getWorkInfosByTag(EventEmitterWorker.WORK_NAME).get()

    /**
     * Releases the network constraint on pending event work so it executes inline, draining until
     * there is nothing left to release. Event requests are enqueued with
     * [androidx.work.NetworkType.CONNECTED], which a test WorkManager never satisfies on its own.
     *
     * The loop matters while events share a work chain, which is the state this harness is written
     * against: a chain leaves only the head-most node ENQUEUED, and the next leaves BLOCKED only
     * while its predecessor runs - after a single pass has already taken its snapshot. Once
     * per-record work units replace the chain, every unit is independently ENQUEUED and one pass
     * releases all of them, so the extra passes become no-ops. Kept because a single-pass drain
     * fails silently rather than loudly: it delivers one event and strands the rest, which reads as
     * the pipeline dropping events rather than as the harness not having released them.
     *
     * Each work unit is released at most once per call, which is what separates "this only just
     * became releasable" from "this ran and asked to be retried". A chain successor is a different
     * work id and so still gets released; a unit that returned Result.retry() keeps the same id and
     * is deliberately left pending, because re-driving it here would silently convert a transient
     * failure into however many attempts the loop had passes for.
     *
     * Absence of a driver is an error rather than a no-op: it means [install] did not run, and
     * every "was not delivered" assertion downstream would otherwise pass for the wrong reason.
     */
    fun runPendingEventWork() {
        val driver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(context)) {
            "No TestDriver - install() did not initialise a test WorkManager"
        }
        val released = mutableSetOf<UUID>()
        repeat(MAX_DRAIN_PASSES) {
            // Re-query every pass: releasing one node is what lets the next leave BLOCKED.
            val pending = eventWork()
                .filter { it.state == WorkInfo.State.ENQUEUED && it.id !in released }
            if (pending.isEmpty()) return
            pending.forEach {
                released += it.id
                driver.setAllConstraintsMet(it.id)
            }
        }
        error("Event work was still pending after $MAX_DRAIN_PASSES drain passes")
    }

}

/**
 * A [TopsortAnalyticsHttpService.Service] that records what it was asked to send and replies with
 * scripted status codes.
 *
 * By default every call succeeds with 204. Call [scriptNext] to queue specific codes; once the
 * script is exhausted the default applies again.
 */
internal class FakeAnalyticsHttpService : TopsortAnalyticsHttpService.Service {

    /** Every event handed to the service, oldest first. */
    val sent = mutableListOf<Any>()

    private val scriptedCodes = ArrayDeque<Int>()
    private val defaultCode = SUCCESS_CODE

    fun scriptNext(vararg codes: Int) {
        scriptedCodes.addAll(codes.toList())
    }

    val impressionsSent: List<ImpressionEvent> get() = sent.filterIsInstance<ImpressionEvent>()

    /** Bids of every impression delivered so far, after draining the pending work. */
    fun reportedImpressionBids(): List<String?> {
        EventPipelineHarness.runPendingEventWork()
        return impressionsSent.flatMap { it.impressions }.map { it.resolvedBidId }
    }

    override fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse =
        record(impressionEvent)

    override fun reportClick(clickEvent: ClickEvent): HttpResponse = record(clickEvent)

    override fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse = record(purchaseEvent)

    override fun reportPageView(pageViewEvent: PageViewEvent): HttpResponse = record(pageViewEvent)

    override fun reportEvent(event: Event): HttpResponse = record(event)

    private fun record(event: Any): HttpResponse {
        sent += event
        val code = scriptedCodes.removeFirstOrNull() ?: defaultCode
        return HttpResponse(code = code, message = "scripted $code")
    }

    companion object {
        const val SUCCESS_CODE = 204
        const val BAD_REQUEST_CODE = 400
        const val SERVER_ERROR_CODE = 500
    }
}
