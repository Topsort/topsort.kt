package com.topsort.analytics.banners

import android.view.Choreographer
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.Analytics
import com.topsort.analytics.EventPipelineHarness
import com.topsort.analytics.FakeAnalyticsHttpService
import com.topsort.analytics.ReportedBids
import com.topsort.analytics.UserIdentity
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The visibility gate against a real window: attach, detach, layout and scroll are the
 * framework's own, and the on-screen check is the real one. BannerVisibilityGateTest covers the
 * same logic with the check stubbed; this covers the check and the lifecycle wiring.
 */
@RunWith(AndroidJUnit4::class)
class BannerVisibilityInWindowTest {

    private companion object {
        const val BANNER_HEIGHT_PX = 200
    }

    private lateinit var scenario: ActivityScenario<BannerHostActivity>
    private lateinit var fake: FakeAnalyticsHttpService
    private lateinit var bannerView: BannerView

    @Before
    fun setUp() {
        fake = EventPipelineHarness.install()
        Analytics.setup(EventPipelineHarness.application, UserIdentity.of("test-user-id"), EventPipelineHarness.TOKEN)
        scenario = ActivityScenario.launch(BannerHostActivity::class.java)
        scenario.onActivity { activity ->
            check(activity.scrollView.height < BannerHostActivity.SPACER_HEIGHT_PX) {
                "viewport taller than the spacer; the banner would not start below the fold"
            }
            bannerView = BannerView(activity, attributeSet(activity))
            activity.column.addView(bannerView, MATCH_PARENT, BANNER_HEIGHT_PX)
        }
        settle()
    }

    @After
    fun tearDown() {
        try {
            if (::scenario.isInitialized) scenario.close()
        } finally {
            EventPipelineHarness.uninstall()
        }
    }

    /**
     * Lets the framework run the traversal a scroll or layout change scheduled. Traversals run on
     * a vsync frame, after the main looper is already idle, so idle alone is not enough. Two
     * frames, not one: a frame callback runs *before* that frame's traversal, so the first only
     * proves a frame happened and the second proves the traversal did.
     */
    private fun settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        repeat(2) {
            val frame = CountDownLatch(1)
            scenario.onActivity { Choreographer.getInstance().postFrameCallback { frame.countDown() } }
            check(frame.await(10, TimeUnit.SECONDS)) { "no frame within 10s" }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun reportedBids() = fake.reportedImpressionBids()

    /** Instant, not smooth: a smooth scroll animates after waitForIdleSync returns. */
    private fun scrollToBanner() {
        scenario.onActivity { it.scrollView.scrollTo(0, BannerHostActivity.SPACER_HEIGHT_PX) }
        settle()
    }

    private fun onScreen(): Boolean {
        var result = false
        scenario.onActivity { result = bannerView.isOnScreen() }
        return result
    }

    @Test
    fun a_banner_below_the_fold_reports_nothing() {
        scenario.onActivity { bannerView.setup(bannerWinner("bid-below-fold"), "/home", null) { _, _ -> } }
        settle()

        assertThat(onScreen()).isFalse()
        assertThat(reportedBids()).isEmpty()
    }

    @Test
    fun scrolling_the_banner_into_view_reports_the_impression_once() {
        scenario.onActivity { bannerView.setup(bannerWinner("bid-scrolled"), "/home", null) { _, _ -> } }
        settle()

        scrollToBanner()

        assertThat(onScreen()).isTrue()
        assertThat(reportedBids()).containsExactly("bid-scrolled")
    }

    /**
     * The real-world ordering for pooled and paged views: setup() before the view has a window.
     * Attaching merges the floating observer's listeners into the window's; the report must still
     * happen exactly once.
     */
    @Test
    fun setup_before_attach_reports_once_when_scrolled_into_view() {
        lateinit var late: BannerView
        scenario.onActivity { activity ->
            late = BannerView(activity, attributeSet(activity))
            late.setup(bannerWinner("bid-setup-before-attach"), "/home", null) { _, _ -> }
            activity.column.addView(late, MATCH_PARENT, BANNER_HEIGHT_PX)
        }
        settle()
        assertThat(reportedBids()).isEmpty()

        scenario.onActivity { it.scrollView.scrollTo(0, BannerHostActivity.SPACER_HEIGHT_PX + BANNER_HEIGHT_PX) }
        settle()
        ReportedBids.clear()
        scenario.onActivity { it.scrollView.scrollBy(0, -10) }
        settle()

        assertThat(reportedBids()).containsExactly("bid-setup-before-attach")
    }

    @Test
    fun a_banner_detached_and_reattached_without_a_new_setup_still_reports_once() {
        scenario.onActivity { bannerView.setup(bannerWinner("bid-reattached"), "/home", null) { _, _ -> } }
        settle()
        scenario.onActivity { it.column.removeView(bannerView) }
        settle()
        scenario.onActivity { it.column.addView(bannerView, MATCH_PARENT, BANNER_HEIGHT_PX) }
        settle()

        scrollToBanner()

        assertThat(reportedBids()).containsExactly("bid-reattached")
    }

    /** A banner already on screen when setup() runs reports on the posted check, before any frame. */
    @Test
    fun a_banner_already_on_screen_reports_without_waiting() {
        scrollToBanner()
        scenario.onActivity { bannerView.setup(bannerWinner("bid-already-visible"), "/home", null) { _, _ -> } }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertThat(reportedBids()).containsExactly("bid-already-visible")
    }
}
