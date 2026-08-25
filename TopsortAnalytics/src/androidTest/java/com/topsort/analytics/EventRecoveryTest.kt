package com.topsort.analytics

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.testing.TestWorkerBuilder
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.EventType
import com.topsort.analytics.worker.EventEmitterWorker
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recovery of events that were cached but never delivered, and the age cap that stops a long
 * backlog from being flushed with heavily backdated timestamps.
 */
@RunWith(AndroidJUnit4::class)
class EventRecoveryTest {

    private lateinit var fake: FakeAnalyticsHttpService

    private fun setUpWith(opaqueUserId: String = EventPipelineHarness.OPAQUE_USER_ID) {
        fake = EventPipelineHarness.install()
        Analytics.setup(EventPipelineHarness.application, opaqueUserId, EventPipelineHarness.TOKEN)
    }

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    /** Drops scheduled work without touching the cache, as a dropped work unit used to. */
    private fun loseScheduledWork() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            EventPipelineHarness.context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build(),
        )
    }

    private fun reportImpression(bidId: String) {
        Analytics.reportImpressionPromoted(
            resolvedBidId = bidId,
            placement = Placement(path = "/recovery"),
        )
    }

    /**
     * A cache record is only deleted once a worker has actually run for it, so events dropped by a
     * terminated chain used to sit there forever with nothing to pick them up.
     */
    @Test
    fun stranded_cache_records_are_resent_on_setup() {
        setUpWith()
        reportImpression("bid-stranded")
        assertThat(Cache.pendingRecords(limit = 100)).isNotEmpty()

        loseScheduledWork()
        assertThat(fake.impressionsSent).isEmpty()

        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * setup() is documented as callable again. Enqueueing is idempotent per record, so a second
     * sweep must not hand the same event to a second worker and deliver it twice - the events API
     * does not de-duplicate on event id.
     */
    @Test
    fun a_second_setup_does_not_deliver_a_pending_record_twice() {
        setUpWith()
        reportImpression("bid-double-setup")

        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * A record whose event type cannot be determined can never be sent, so leaving it in place
     * would mean re-reading and re-decrypting it on every sweep forever. It is pruned instead.
     */
    @Test
    fun a_cached_record_that_cannot_be_interpreted_is_pruned_rather_than_swept_forever() {
        setUpWith()
        EventPipelineHarness.plantRawRecord(
            recordId = 701,
            json = """{"somethingElse":[{"occurredAt":"2026-08-20T10:00:00.000Z"}]}""",
        )

        loseScheduledWork()
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.sent).isEmpty()
        assertThat(Cache.cachedRecordIds()).doesNotContain(701L)
    }

    /**
     * Installs upgrading from the shared-chain version still have work pending under the bare
     * WORK_NAME. That is a different unique name from the per-record WORK_NAME-<id>, so KEEP cannot
     * collapse it, and the record would end up with two owners - the surviving chain unit and the
     * one the sweep enqueues. Both can read it before either deletes it, and the events API does
     * not de-duplicate, so that is a duplicate counted and billed event.
     *
     * The legacy request carries the same network constraint real event work does, so it stays
     * ENQUEUED rather than running inline - otherwise it would already be SUCCEEDED by the time the
     * sweep ran and the test would pass whether or not anything was cancelled.
     */
    @Test
    fun the_legacy_shared_work_chain_is_cancelled_on_the_first_sweep() {
        setUpWith()
        val wm = WorkManager.getInstance(EventPipelineHarness.context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        wm.enqueueUniqueWork(
            EventEmitterWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<EventEmitterWorker>()
                .setConstraints(constraints)
                .build(),
        )
        assertThat(wm.getWorkInfosForUniqueWork(EventEmitterWorker.WORK_NAME).get())
            .allMatch { it.state == WorkInfo.State.ENQUEUED }

        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )

        assertThat(wm.getWorkInfosForUniqueWork(EventEmitterWorker.WORK_NAME).get())
            .allMatch { it.state == WorkInfo.State.CANCELLED }
    }

    /**
     * A backdated event is delivered, not discarded.
     *
     * The SDK has no basis for deciding an event is too old to send: whether it still attributes
     * depends on the marketplace's attribution window, and whether it is still billable depends on
     * the campaign's charge type - a CPM impression is chargeable long after it can attribute.
     * Both live server-side. This replaces an age cap that deleted such events client-side.
     */
    @Test
    fun a_long_backdated_event_is_still_delivered() {
        setUpWith()
        val longAgo = ISODateTimeFormat.dateTime().print(DateTime.now().minusDays(90))

        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-backdated",
            placement = Placement(path = "/recovery"),
            occurredAt = longAgo,
        )
        val recordId = Cache.cachedRecordIds().single()

        loseScheduledWork()
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(fake.impressionsSent.single().impressions.single().occurredAt).isEqualTo(longAgo)
        assertThat(Cache.cachedRecordIds()).doesNotContain(recordId)
    }

    /**
     * The only thing that evicts an undeliverable record now is capacity, and it takes the oldest
     * first. Uses a lowered bound so the test does not have to write five thousand records.
     */
    @Test
    fun a_cache_over_capacity_evicts_oldest_first() {
        setUpWith()
        repeat(12) { i ->
            Analytics.reportImpressionPromoted(
                resolvedBidId = "bid-$i",
                placement = Placement(path = "/recovery"),
            )
        }
        val all = Cache.cachedRecordIds()
        assertThat(all).hasSize(12)

        val kept = Cache.pendingRecordsForTest(limit = 100, capacity = 5)

        // The five newest survive; the seven oldest are gone.
        assertThat(Cache.cachedRecordIds()).isEqualTo(all.takeLast(5))
        assertThat(kept.map { it.recordId }).isEqualTo(all.takeLast(5))
    }
}
