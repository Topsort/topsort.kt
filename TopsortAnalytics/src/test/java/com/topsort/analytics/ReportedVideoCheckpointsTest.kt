package com.topsort.analytics

import com.topsort.analytics.model.VideoCheckpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ReportedVideoCheckpointsTest {

    @Before
    fun reset() {
        ReportedVideoCheckpoints.clear()
    }

    private fun mark(
        checkpoint: VideoCheckpoint = VideoCheckpoint.START,
        bid: String = "bid-1",
        user: String = "user-1",
    ) = ReportedVideoCheckpoints.markReported(user, bid, checkpoint)

    @Test
    fun `first report of a checkpoint is kept`() {
        assertThat(mark()).isTrue()
    }

    @Test
    fun `repeat reports of the same checkpoint are dropped`() {
        mark()

        assertThat(mark()).isFalse()
        assertThat(mark()).isFalse()
    }

    @Test
    fun `each checkpoint of a bid is tracked independently`() {
        VideoCheckpoint.entries.forEach {
            assertThat(mark(checkpoint = it)).isTrue()
        }
    }

    @Test
    fun `the same checkpoint of distinct bids is tracked independently`() {
        assertThat(mark(bid = "bid-1")).isTrue()
        assertThat(mark(bid = "bid-2")).isTrue()
    }

    @Test
    fun `another user's report of the same bid is not a repeat`() {
        assertThat(mark(user = "user-a")).isTrue()
        assertThat(mark(user = "user-b")).isTrue()
        assertThat(mark(user = "user-a")).isFalse()
    }

    @Test
    fun `tracking is bounded and evicts the oldest checkpoint first`() {
        repeat(ReportedVideoCheckpoints.MAX_TRACKED_CHECKPOINTS) { mark(bid = "bid-$it") }

        // One past the cap evicts bid-0, the least recently used.
        assertThat(mark(bid = "overflow")).isTrue()

        // Assert the survivor before the evicted one: re-reporting an evicted entry reinserts it,
        // which evicts whatever is eldest by then.
        assertThat(mark(bid = "bid-1")).isFalse()
        assertThat(mark(bid = "bid-0")).isTrue()
    }
}
