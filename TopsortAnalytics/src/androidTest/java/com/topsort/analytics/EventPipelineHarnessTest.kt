package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import com.topsort.analytics.model.Placement
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers [EventPipelineHarness] itself, so that a failure in a pipeline test can be read as a
 * problem with the pipeline rather than with the scaffolding around it.
 */
@RunWith(AndroidJUnit4::class)
class EventPipelineHarnessTest {

    private lateinit var fake: FakeAnalyticsHttpService

    @Before
    fun setUp() {
        fake = EventPipelineHarness.install()
        Analytics.setup(
            EventPipelineHarness.application,
            EventPipelineHarness.OPAQUE_USER_ID,
            EventPipelineHarness.TOKEN,
        )
    }

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    @Test
    fun install_replaces_the_http_service() {
        assertThat(TopsortAnalyticsHttpService.service).isSameAs(fake)
    }

    @Test
    fun reporting_an_event_enqueues_tagged_event_work() {
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-1",
            placement = Placement(path = "/harness"),
        )

        assertThat(EventPipelineHarness.eventWork()).isNotEmpty()
    }

    @Test
    fun releasing_constraints_delivers_the_event_to_the_fake() {
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-2",
            placement = Placement(path = "/harness"),
        )

        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * The drain has to keep going while work is still becoming releasable, not release whatever
     * happened to be ENQUEUED when it was called.
     *
     * Regression: a single release pass delivered exactly one of these two events. Events share a
     * work chain at this point in the stack, so the second was BLOCKED at snapshot time and left
     * stranded - which in a delivery test would have looked like the pipeline dropping an event.
     * Per-record work units remove the chain, after which one pass would suffice; this pins the
     * harness against the shape it is written for.
     */
    @Test
    fun releasing_constraints_drains_every_pending_event() {
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-4",
            placement = Placement(path = "/harness"),
        )
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-5",
            placement = Placement(path = "/harness"),
        )

        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(2)
    }

    @Test
    fun a_scripted_four_hundred_makes_the_event_work_fail() {
        fake.scriptNext(FakeAnalyticsHttpService.BAD_REQUEST_CODE)

        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-3",
            placement = Placement(path = "/harness"),
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(EventPipelineHarness.eventWork().map { it.state })
            .contains(WorkInfo.State.FAILED)
    }

}
