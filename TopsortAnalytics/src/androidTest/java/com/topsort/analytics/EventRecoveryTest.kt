package com.topsort.analytics

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.topsort.analytics.model.Placement
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
}
