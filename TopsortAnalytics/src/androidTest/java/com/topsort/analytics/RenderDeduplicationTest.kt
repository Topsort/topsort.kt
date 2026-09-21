package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topsort.analytics.model.Placement
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Render's counterpart to [ImpressionDeduplicationTest]. A render fires as soon as the ad enters
 * the view - immediately on [com.topsort.analytics.banners.BannerView.setup], well before the
 * on-screen-gated impression - so the same repeated-call shapes that motivated impression
 * deduplication (INTE-2706: a recomposition or view rebind re-firing the beacon for a bid already
 * reported) apply here too, and are tracked separately in [ReportedRenderBids] so a render and an
 * impression for the same bid never consume each other's tracked-bid slot.
 */
@RunWith(AndroidJUnit4::class)
class RenderDeduplicationTest {

    private lateinit var fake: FakeAnalyticsHttpService

    private fun setUpWith(opaqueUserId: String = EventPipelineHarness.OPAQUE_USER_ID) {
        fake = EventPipelineHarness.install()
        setup(UserIdentity.of(opaqueUserId), EventPipelineHarness.TOKEN)
    }

    private fun setup(identity: UserIdentity, token: String) =
        Analytics.setup(EventPipelineHarness.application, identity, token)

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    private fun reportRender(bidId: String) {
        Analytics.reportRender(
            resolvedBidId = bidId,
            placement = Placement(path = "/dedup"),
        )
    }

    /** One bid, many reports, one render on the wire - the same shape as the impression bug. */
    @Test
    fun a_bid_reported_many_times_is_sent_once() {
        setUpWith()

        repeat(50) { reportRender("bid-looping") }
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.rendersSent).hasSize(1)
        assertThat(fake.rendersSent.single().renders).hasSize(1)
    }

    /** A repeat must not even reach the cache, or the sweep would deliver it later. */
    @Test
    fun a_repeat_is_never_cached() {
        setUpWith()

        reportRender("bid-cached-once")
        reportRender("bid-cached-once")

        EventPipelineHarness.runPendingEventWork()
        // A record the dedup let through would surface here on the sweep as a second delivery.
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.rendersSent).hasSize(1)
    }

    /** Deduplication is per bid: distinct ads in the same session all report. */
    @Test
    fun distinct_bids_all_report() {
        setUpWith()

        reportRender("bid-a")
        reportRender("bid-b")
        reportRender("bid-a")
        reportRender("bid-c")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.rendersSent.flatMap { it.renders }.map { it.resolvedBidId })
            .containsExactlyInAnyOrder("bid-a", "bid-b", "bid-c")
    }

    /**
     * A render and an impression for the same bid must not share one tracked-bid slot: the render
     * fires first, and if it consumed the impression's slot the real, billable impression would be
     * silently dropped when it fires later.
     */
    @Test
    fun a_render_does_not_suppress_the_impression_for_the_same_bid() {
        setUpWith()

        reportRender("bid-shared-with-impression")
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-shared-with-impression",
            placement = Placement(path = "/dedup"),
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.rendersSent.flatMap { it.renders }.map { it.resolvedBidId })
            .containsExactly("bid-shared-with-impression")
        assertThat(fake.impressionsSent.flatMap { it.impressions }.map { it.resolvedBidId })
            .containsExactly("bid-shared-with-impression")
    }

    /**
     * A second setup() may be a different user, whose renders are their own and must not be
     * dropped as duplicates of the previous user's.
     */
    @Test
    fun a_new_session_reports_a_bid_again() {
        setUpWith()

        reportRender("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        setup(UserIdentity.of("second-user"), EventPipelineHarness.TOKEN)
        reportRender("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.rendersSent).hasSize(2)
        assertThat(fake.rendersSent.map { it.renders.single().opaqueUserId })
            .containsExactly(EventPipelineHarness.OPAQUE_USER_ID, "second-user")
    }
}
