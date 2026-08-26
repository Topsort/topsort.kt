package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ReportedBidsTest {

    @Before
    fun reset() {
        ReportedBids.clear()
    }

    @Test
    fun `first report of a bid is kept`() {
        assertThat(ReportedBids.markReported("bid-1")).isTrue()
    }

    @Test
    fun `repeat reports of the same bid are dropped`() {
        ReportedBids.markReported("bid-1")

        assertThat(ReportedBids.markReported("bid-1")).isFalse()
        assertThat(ReportedBids.markReported("bid-1")).isFalse()
    }

    @Test
    fun `distinct bids are tracked independently`() {
        assertThat(ReportedBids.markReported("bid-1")).isTrue()
        assertThat(ReportedBids.markReported("bid-2")).isTrue()
        assertThat(ReportedBids.markReported("bid-1")).isFalse()
    }

    @Test
    fun `clear forgets tracked bids so a new session reports again`() {
        ReportedBids.markReported("bid-1")

        ReportedBids.clear()

        assertThat(ReportedBids.markReported("bid-1")).isTrue()
    }

    @Test
    fun `tracking is bounded and evicts the oldest bid first`() {
        repeat(ReportedBids.MAX_TRACKED_BIDS) { ReportedBids.markReported("bid-$it") }

        // One past the cap evicts bid-0, the least recently used.
        assertThat(ReportedBids.markReported("overflow")).isTrue()

        // bid-1 survived, so it is still deduplicated. Assert it before bid-0: re-reporting an
        // evicted bid reinserts it, which evicts whatever is eldest by then.
        assertThat(ReportedBids.markReported("bid-1")).isFalse()
        assertThat(ReportedBids.markReported("bid-0")).isTrue()
    }

    @Test
    fun `a bid touched again survives eviction ahead of older ones`() {
        repeat(ReportedBids.MAX_TRACKED_BIDS) { ReportedBids.markReported("bid-$it") }

        // Re-reporting bid-0 is dropped, but moves it to the most recent end of the LRU.
        assertThat(ReportedBids.markReported("bid-0")).isFalse()

        ReportedBids.markReported("overflow")

        // bid-1 is now the eldest and goes instead of bid-0.
        assertThat(ReportedBids.markReported("bid-0")).isFalse()
        assertThat(ReportedBids.markReported("bid-1")).isTrue()
    }

    @Test
    fun `concurrent reports of one bid let exactly one through`() {
        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val kept = AtomicInteger()

        repeat(threads) {
            pool.submit {
                start.await()
                if (ReportedBids.markReported("contended")) kept.incrementAndGet()
            }
        }
        start.countDown()
        pool.shutdown()
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue()

        assertThat(kept.get()).isEqualTo(1)
    }
}
