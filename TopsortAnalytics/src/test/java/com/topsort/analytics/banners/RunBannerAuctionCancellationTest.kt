package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.AuctionsHttpService
import com.topsort.analytics.service.TopsortAuctionsHttpService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * Cancelling the scope while the auction is in flight must come out of runBannerAuction as
 * cancellation, not as an AuctionError: the caller gets no callback for a screen it left.
 */
class RunBannerAuctionCancellationTest {

    private val entered = CountDownLatch(1)
    private val release = CountDownLatch(1)

    /** Blocks inside the IO call until the test has cancelled the job. */
    private val blocking = object : AuctionsHttpService {
        override fun runAuctionsSync(request: AuctionRequest): AuctionResponse? {
            entered.countDown()
            release.await()
            return AuctionResponse.fromJson("""{"results":[]}""")
        }

        override suspend fun runAuctions(request: AuctionRequest): AuctionResponse =
            throw AuctionError.EmptyResponse
    }

    @After
    fun tearDown() = TopsortAuctionsHttpService.resetToDefaultService()

    @Test
    fun `a cancelled job surfaces as CancellationException, not HttpError`() = runBlocking<Unit> {
        TopsortAuctionsHttpService.setMockService(blocking)
        var caught: Throwable? = null

        val job = launch(Dispatchers.Default) {
            try {
                runBannerAuction(BannerConfig.LandingPage(slotId = "slot"))
            } catch (e: Throwable) {
                caught = e
            }
        }
        entered.await()
        job.cancel()
        release.countDown()
        job.join()

        assertThat(caught).isInstanceOf(CancellationException::class.java)
    }
}
