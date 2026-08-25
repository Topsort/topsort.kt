package com.topsort.analytics.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.topsort.analytics.Cache

/**
 * Recovers events that were cached but never delivered.
 *
 * This runs the sweep off the caller's thread. It used to happen inline in [Analytics.setup], which
 * integrators are told to call from their Application class: reading the cache decrypts every
 * record and pruning the ones nothing can ever send writes synchronously, so a device with a large stranded backlog
 * risked an ANR at startup - and the backlog is largest on exactly the installs the sweep exists
 * for.
 */
internal class PendingEventSweepWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    init {
        Cache.initialize(context)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun doWork(): Result {
        // Resolved here rather than read from Analytics: that field is only set by setup(), and
        // WorkManager can run this worker in a process where setup() has not been called. The
        // sweep deletes records, so it must never run on a path that cannot also deliver them.
        val workManager = try {
            WorkManager.getInstance(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager unavailable; leaving the cache untouched", e)
            return Result.retry()
        }

        sweep(workManager)
        return Result.success()
    }

    /**
     * Re-enqueues undelivered records so a stranded backlog gets another chance at delivery.
     *
     * This deliberately does NOT decide what is too old to send. It re-enqueues every record it
     * reads, anchored to that record's own occurredAt, and [EventEmitterWorker] applies the age cap
     * when the work runs. Two reasons:
     *
     * - It cannot tell "stranded for a week" from "reported a moment ago and already enqueued". A
     *   caller may report an event with an explicit backdated occurredAt through the public API,
     *   and pruning on occurredAt alone destroyed those while their own work unit was still
     *   pending - delivering the same event whenever no sweep happened to be in flight. Asking
     *   WorkManager whether a record has live work is not available here: that query blocks on the
     *   executor this worker is already running on.
     * - KEEP makes the re-enqueue a no-op for any record that already has work pending, so the
     *   record keeps one owner and the age decision lands in exactly one place.
     */
    private fun sweep(workManager: WorkManager) {
        // Installs upgrading from a version that enqueued onto one shared chain still have work
        // pending under the bare WORK_NAME. KEEP cannot see it - that is a different unique name
        // from the per-record WORK_NAME-<id> - so without this a record would have two owners: the
        // surviving chain unit and the one enqueued below. Per-record units run in parallel, so
        // both can read the record before either deletes it, and the events API does not
        // de-duplicate: that is a duplicate counted event, and for CPM campaigns a billed one.
        //
        // Targets only the legacy chain. Note WORK_NAME is also the TAG on every per-record unit,
        // so cancelAllWorkByTag(WORK_NAME) would destroy the entire pending queue - cancelling by
        // unique name is what makes this safe. Idempotent, so it stays after the migration window.
        workManager.cancelUniqueWork(EventEmitterWorker.WORK_NAME)

        val candidates = Cache.pendingRecords(MAX_RESEND_PER_SWEEP)
        if (candidates.isEmpty()) return

        Log.i(TAG, "Re-enqueueing ${candidates.size} undelivered cached event(s)")
        candidates.forEach {
            EventEmitterWorker.enqueue(workManager, it.recordId, it.eventType, it.occurredAt)
        }
    }

    companion object {
        private const val TAG = "TopsortSweepWorker"

        /**
         * Upper bound on how many cached records one sweep reads and re-enqueues. Lives beside
         * MAX_EVENT_AGE_DAYS in this package rather than in Analytics: the two constants jointly
         * define recovery policy and are best read together.
         */
        private const val MAX_RESEND_PER_SWEEP = 100
        const val WORK_NAME = "TopsortAnalyticsPendingSweep"
    }
}
