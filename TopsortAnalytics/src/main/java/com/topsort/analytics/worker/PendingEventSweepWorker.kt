package com.topsort.analytics.worker

import android.content.Context
import androidx.work.Worker
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

    override fun doWork(): Result {
        Analytics.sweepPendingEvents()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "TopsortAnalyticsPendingSweep"
    }
}
