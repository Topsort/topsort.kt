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
     * A stranded record keeps its original occurredAt, so flushing a long backlog after an app
     * upgrade would deliver heavily backdated events. Anything past the cap is discarded instead.
     */
    @Test
    fun an_undelivered_event_older_than_a_week_is_discarded_rather_than_sent() {
        setUpWith()
        val nineDaysAgo = ISODateTimeFormat.dateTime().print(DateTime.now().minusDays(9))
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-stale",
            placement = Placement(path = "/regression"),
            occurredAt = nineDaysAgo,
        )
        assertThat(Cache.pendingRecords(limit = 100)).isNotEmpty()

        // Strand the record. Without this the original work unit is still live and the assertion
        // below passes because the worker no-ops on an already-deleted record, not because the age
        // cap prevented a delivery.
        loseScheduledWork()

        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).isEmpty()
        assertThat(Cache.pendingRecords(limit = 100)).isEmpty()
    }

    /** A record inside the cap is still recovered. */
    @Test
    fun an_undelivered_event_inside_the_cap_is_still_resent() {
        setUpWith()
        val twoDaysAgo = ISODateTimeFormat.dateTime().print(DateTime.now().minusDays(2))
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-recent",
            placement = Placement(path = "/regression"),
            occurredAt = twoDaysAgo,
        )
        loseScheduledWork()

        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * A timestamp that will not parse means the age is unknown, not that the record is junk.
     * `occurredAt` is carried as a string all the way to the wire - only the sweep's age check
     * parses it - so the event is still perfectly deliverable and must still be delivered.
     *
     * Regression: that parse used to throw, the sweep treated it as an unreadable record and
     * skipped it, and nothing removed it - so the event was never sent and never pruned.
     */
    @Test
    fun an_undelivered_event_with_an_unparseable_timestamp_is_still_resent() {
        setUpWith()
        reportImpression("bid-unparseable-timestamp")
        val recordId = Cache.cachedRecordIds().single()
        EventPipelineHarness.corruptOccurredAt(recordId, "not-a-timestamp")

        loseScheduledWork()
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(fake.impressionsSent.single().impressions.single().occurredAt)
            .isEqualTo("not-a-timestamp")
        assertThat(Cache.cachedRecordIds()).doesNotContain(recordId)
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
     * A record with unfinished delivery work is not stranded, so the sweep must leave it alone.
     *
     * Regression: an event reported with an explicit backdated occurredAt - a documented public
     * parameter, and normal for backfilled order sync - was classified past the cap by the next
     * sweep and destroyed while its own work unit was still pending. The same event was delivered
     * when no sweep ran, so the loss was nondeterministic rather than a policy decision.
     */
    @Test
    fun a_backdated_event_with_work_pending_is_not_swept_away() {
        setUpWith()
        val nineDaysAgo = ISODateTimeFormat.dateTime().print(DateTime.now().minusDays(9))

        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-backdated",
            placement = Placement(path = "/recovery"),
            occurredAt = nineDaysAgo,
        )
        val recordId = Cache.cachedRecordIds().single()

        // setup() is documented as callable again; it schedules a sweep each time.
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(Cache.cachedRecordIds()).doesNotContain(recordId)
    }

    /**
     * The worker-side cap, driven directly rather than through the scheduler.
     *
     * The PR originally shipped this uncovered on the grounds that proving it needed time control
     * or an injected anchor. The anchor is already injected - it travels in the work's input data -
     * so the cost estimate was simply wrong, and this is the branch that deletes an event.
     */
    @Test
    fun the_worker_discards_an_event_whose_age_anchor_is_past_the_cap() {
        setUpWith()
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-anchored",
            placement = Placement(path = "/recovery"),
        )
        val recordId = Cache.cachedRecordIds().single()
        fake.sent.clear()

        val worker = TestWorkerBuilder<EventEmitterWorker>(
            context = EventPipelineHarness.context,
            executor = Executors.newSingleThreadExecutor(),
            inputData = workDataOf(
                EventEmitterWorker.EXTRA_RECORD_ID to recordId,
                EventEmitterWorker.EXTRA_EVENT_TYPE to EventType.Impression.ordinal,
                EventEmitterWorker.EXTRA_AGE_ANCHOR_MILLIS to
                    DateTime.now().minusDays(EventEmitterWorker.MAX_EVENT_AGE_DAYS + 2).millis,
            ),
        ).build()

        assertThat(worker.doWork()).isEqualTo(androidx.work.ListenableWorker.Result.success())
        assertThat(fake.sent).isEmpty()
        assertThat(Cache.cachedRecordIds()).doesNotContain(recordId)
    }

    /**
     * The prune is a batch operation over a mixed set, and every other test here has exactly one
     * record in the cache - so a bug that removed the wrong keys, or took a fresh record along with
     * the stale ones, would pass all of them.
     */
    @Test
    fun a_mixed_batch_prunes_only_the_records_that_should_go() {
        setUpWith()
        val old = ISODateTimeFormat.dateTime().print(DateTime.now().minusDays(9))

        Analytics.reportImpressionPromoted(
            resolvedBidId = "stale-1",
            placement = Placement(path = "/recovery"),
            occurredAt = old,
        )
        Analytics.reportImpressionPromoted(
            resolvedBidId = "stale-2",
            placement = Placement(path = "/recovery"),
            occurredAt = old,
        )
        Analytics.reportImpressionPromoted(
            resolvedBidId = "fresh",
            placement = Placement(path = "/recovery"),
        )
        val ids = Cache.cachedRecordIds()
        assertThat(ids).hasSize(3)
        val freshId = ids.last()
        EventPipelineHarness.plantRawRecord(
            recordId = 9_000,
            json = """{"somethingElse":[{"occurredAt":"$old"}]}""",
        )

        loseScheduledWork()
        fake.sent.clear()
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
        EventPipelineHarness.runPendingEventWork()

        // The two stale records and the uninterpretable one are gone; the fresh one was delivered.
        assertThat(Cache.cachedRecordIds()).isEmpty()
        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(fake.impressionsSent.single().impressions.single().resolvedBidId)
            .isEqualTo("fresh")
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
}
