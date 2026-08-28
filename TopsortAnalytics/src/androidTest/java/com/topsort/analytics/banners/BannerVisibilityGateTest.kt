package com.topsort.analytics.banners

import android.content.Context
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.Analytics
import com.topsort.analytics.EventPipelineHarness
import com.topsort.analytics.FakeAnalyticsHttpService
import com.topsort.analytics.ReportedBids
import com.topsort.analytics.UserIdentity
import com.topsort.analytics.model.auctions.EntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The impression is owed when the banner is on screen, not when it is laid out. A layout pass
 * happens below the fold and in pre-bound list rows too, and every one of those used to bill.
 */
@RunWith(AndroidJUnit4::class)
class BannerVisibilityGateTest {

    private lateinit var context: Context
    private lateinit var parent: FrameLayout
    private lateinit var bannerView: BannerView
    private lateinit var fake: FakeAnalyticsHttpService
    private var onScreen = false

    private val winner = BannerResponse(
        id = "p_SA0238",
        type = EntityType.PRODUCT,
        url = "https://example.invalid/creative.png",
        resolvedBidId = "resolved-bid-visibility",
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        fake = EventPipelineHarness.install()
        Analytics.setup(EventPipelineHarness.application, UserIdentity.of("test-user-id"), EventPipelineHarness.TOKEN)
        parent = FrameLayout(context)
        bannerView = BannerView(context, attributeSet(context))
        bannerView.isOnScreen = { onScreen }
        parent.addView(bannerView)
    }

    @After
    fun tearDown() {
        parent.removeAllViews()
        EventPipelineHarness.uninstall()
    }

    private fun reportedBids(): List<String?> {
        EventPipelineHarness.runPendingEventWork()
        return fake.impressionsSent.flatMap { it.impressions }.map { it.resolvedBidId }
    }

    @Test
    fun a_layout_pass_off_screen_reports_nothing() {
        runBlocking(Dispatchers.Main) {
            bannerView.setup(winner, path = "/home", location = null) { _, _ -> }
            bannerView.viewTreeObserver.dispatchOnGlobalLayout()

            assertThat(reportedBids()).isEmpty()
        }
    }

    @Test
    fun the_impression_is_reported_once_the_banner_comes_on_screen_and_only_once() {
        runBlocking(Dispatchers.Main) {
            bannerView.setup(winner, path = "/home", location = null) { _, _ -> }
            bannerView.viewTreeObserver.dispatchOnGlobalLayout()
            onScreen = true
            bannerView.viewTreeObserver.dispatchOnGlobalLayout()
            // The per-bid deduplication would hide a second report; clear it so this pins the view.
            ReportedBids.clear()
            bannerView.viewTreeObserver.dispatchOnGlobalLayout()

            assertThat(reportedBids()).containsExactly(winner.resolvedBidId)
        }
    }

    /** A view with no window is not on screen, whatever its layout state. */
    @Test
    fun the_real_check_is_false_without_a_window() {
        val detached = BannerView(context, attributeSet(context))
        assertThat(detached.isOnScreen()).isFalse()
    }
}
