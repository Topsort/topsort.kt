package com.topsort.analytics.banners

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import com.topsort.analytics.service.AuctionsHttpService
import com.topsort.analytics.service.TopsortAuctionsHttpService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A cancelled scope is the host navigating away, not a banner failure. setup() must let the
 * cancellation through rather than turning it into an onError callback.
 */
@RunWith(AndroidJUnit4::class)
class BannerCancellationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val cancelling = object : AuctionsHttpService {
        override fun runAuctionsSync(request: AuctionRequest): AuctionResponse? =
            throw CancellationException("scope cancelled")

        override suspend fun runAuctions(request: AuctionRequest): AuctionResponse =
            throw CancellationException("scope cancelled")
    }

    @Before
    fun setUp() = TopsortAuctionsHttpService.setMockService(cancelling)

    @After
    fun tearDown() = TopsortAuctionsHttpService.resetToDefaultService()

    @Test
    fun cancellation_propagates_and_reports_no_error() {
        val bannerView = BannerView(context, attributeSet(context))
        var reported: Throwable? = null
        bannerView.onError { reported = it }

        assertThatThrownBy {
            runBlocking(Dispatchers.Main) {
                bannerView.setup(BannerConfig.LandingPage(slotId = "slot"), "/home", null) { _, _ -> }
            }
        }.isInstanceOf(CancellationException::class.java)
        assertThat(reported).isNull()
    }

}
