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
