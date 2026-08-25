package com.topsort.analytics.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.topsort.analytics.Analytics
import com.topsort.analytics.Cache

/**
 * Recovers events that were cached but never delivered.
 *
 * This runs the sweep off the caller's thread. It used to happen inline in [Analytics.setup], which
 * integrators are told to call from their Application class: reading the cache decrypts every
 * record and pruning aged-out ones writes synchronously, so a device with a large stranded backlog
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

        Analytics.sweepPendingEvents(workManager)
        return Result.success()
    }

    companion object {
        private const val TAG = "TopsortSweepWorker"
        const val WORK_NAME = "TopsortAnalyticsPendingSweep"
    }
}
