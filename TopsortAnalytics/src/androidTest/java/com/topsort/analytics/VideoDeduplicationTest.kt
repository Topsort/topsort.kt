package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.VideoCheckpoint
import com.topsort.analytics.model.VideoEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Video's counterpart to [RenderDeduplicationTest], through the whole pipeline. Checkpoints are
 * deduplicated per user, bid and checkpoint in [ReportedVideoCheckpoints]; errors never are.
 */
@RunWith(AndroidJUnit4::class)
class VideoDeduplicationTest {

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

    private fun reportCheckpoint(
        bidId: String,
        checkpoint: VideoCheckpoint = VideoCheckpoint.START,
        opaqueUserId: String? = null,
    ) {
        Analytics.reportVideoCheckpoint(
            resolvedBidId = bidId,
            checkpoint = checkpoint,
            opaqueUserId = opaqueUserId,
        )
    }

    private fun sentEventTypes(): List<VideoEventType> {
        EventPipelineHarness.runPendingEventWork()
        return fake.videoEventsSent.flatMap { it.videoEvents }.map { it.eventType }
    }

    @Test
    fun a_checkpoint_reported_many_times_is_sent_once() {
        setUpWith()

        repeat(10) { reportCheckpoint("bid-looping") }
        EventPipelineHarness.runPendingEventWork()
        // A record the dedup let through would surface here on the sweep as a second delivery.
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.videoEventsSent).hasSize(1)
    }

    @Test
    fun every_checkpoint_of_one_play_is_sent() {
        setUpWith()

        VideoCheckpoint.entries.forEach { reportCheckpoint("bid-play", it) }

        assertThat(sentEventTypes())
            .containsExactlyElementsOf(VideoCheckpoint.entries.map { VideoEventType.Checkpoint(it) })
    }

    @Test
    fun repeated_errors_are_all_sent() {
        setUpWith()

        Analytics.reportVideoError(resolvedBidId = "bid-failing", errorCode = 405)
        Analytics.reportVideoError(resolvedBidId = "bid-failing", errorCode = 405)
        Analytics.reportVideoError(resolvedBidId = "bid-failing", errorCode = 402)

        assertThat(sentEventTypes()).containsExactly(
            VideoEventType.Error(405),
            VideoEventType.Error(405),
            VideoEventType.Error(402),
        )
    }

    /** The per-user key, the lesson from topsort.swift#98: another user's report is not a repeat. */
    @Test
    fun the_same_checkpoint_from_another_user_is_sent() {
        setUpWith()

        reportCheckpoint("bid-shared", opaqueUserId = "user-a")
        reportCheckpoint("bid-shared", opaqueUserId = "user-b")
        reportCheckpoint("bid-shared", opaqueUserId = "user-a")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.videoEventsSent.flatMap { it.videoEvents }.map { it.opaqueUserId })
            .containsExactlyInAnyOrder("user-a", "user-b")
    }

    @Test
    fun a_new_session_user_reports_a_checkpoint_again() {
        setUpWith()

        reportCheckpoint("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        setup(UserIdentity.of("second-user"), EventPipelineHarness.TOKEN)
        reportCheckpoint("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.videoEventsSent.map { it.videoEvents.single().opaqueUserId })
            .containsExactly(EventPipelineHarness.OPAQUE_USER_ID, "second-user")
    }

    /** Video events track their own set, so they never use up the bid's render or impression. */
    @Test
    fun a_video_event_does_not_suppress_the_render_or_impression_for_the_same_bid() {
        setUpWith()

        reportCheckpoint("bid-video")
        Analytics.reportRender(resolvedBidId = "bid-video", placement = Placement(path = "/dedup"))
        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-video",
            placement = Placement(path = "/dedup"),
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.videoEventsSent).hasSize(1)
        assertThat(fake.rendersSent).hasSize(1)
        assertThat(fake.impressionsSent).hasSize(1)
    }
}
