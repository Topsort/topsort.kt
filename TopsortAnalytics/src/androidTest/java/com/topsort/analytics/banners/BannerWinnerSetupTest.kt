package com.topsort.analytics.banners

import android.content.Context
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.Analytics
import com.topsort.analytics.EventPipelineHarness
import com.topsort.analytics.FakeAnalyticsHttpService
import com.topsort.analytics.UserIdentity
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.auctions.EntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The overload that takes a winner someone else resolved.
 *
 * It exists so that running your own auction - your HTTP stack, your auth, a winner you cached -
 * does not also mean owning when the impression fires. That trigger is the part that costs money
 * when it goes wrong, so it stays in the SDK even when the auction does not.
 */
@RunWith(AndroidJUnit4::class)
class BannerWinnerSetupTest {

    private lateinit var context: Context
    private lateinit var parent: FrameLayout
    private lateinit var bannerView: BannerView
    private lateinit var fake: FakeAnalyticsHttpService

    private val winner = BannerResponse(
        id = "p_SA0238",
        type = EntityType.PRODUCT,
        url = "https://example.invalid/creative.png",
        resolvedBidId = "resolved-bid-from-our-own-auction",
    )

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        fake = EventPipelineHarness.install()
        Analytics.setup(
            EventPipelineHarness.application,
            UserIdentity.of("test-user-id"),
            EventPipelineHarness.TOKEN,
        )
        parent = FrameLayout(context)
        bannerView = BannerView(context, attributeSet(context))
        parent.addView(bannerView)
    }

    @After
    fun tearDown() {
        parent.removeAllViews()
        EventPipelineHarness.uninstall()
    }

    /** A click reports against the supplied bid and hands the entity back to the caller. */
    @Test
    fun a_click_reports_the_supplied_bid_and_invokes_onClick() {
        runBlocking(Dispatchers.Main) {
        var clickedId: String? = null
        var clickedType: EntityType? = null

        bannerView.setup(winner, path = "/search", location = "banner_top") { id, type ->
            clickedId = id
            clickedType = type
        }
        bannerView.performClick()
        EventPipelineHarness.runPendingEventWork()

        assertThat(clickedId).isEqualTo(winner.id)
        assertThat(clickedType).isEqualTo(winner.type)
        assertThat(fake.sent.filterIsInstance<ClickEvent>().flatMap { it.clicks }
            .map { it.resolvedBidId })
            .containsExactly(winner.resolvedBidId)
        }
    }

    /** The placement the caller passed is the placement reported. */
    @Test
    fun the_reported_click_carries_the_supplied_placement() {
        runBlocking(Dispatchers.Main) {
        bannerView.setup(winner, path = "/search", location = "banner_top") { _, _ -> }
        bannerView.performClick()
        EventPipelineHarness.runPendingEventWork()

        val click = fake.sent.filterIsInstance<ClickEvent>().flatMap { it.clicks }.single()
        assertThat(click.placement.path).isEqualTo("/search")
        assertThat(click.placement.location).isEqualTo("banner_top")
        }
    }

    /**
     * Two setups before a layout pass must leave one listener, not two.
     *
     * Different winners on purpose: with the same bid the per-bid deduplication would mask a
     * second report, and this is testing the view, not that guard. The first banner never
     * reached the screen, so it is owed no impression.
     */
    @Test
    fun a_second_setup_replaces_the_pending_impression_of_the_first() {
        runBlocking(Dispatchers.Main) {
            val superseded = winner.copy(resolvedBidId = "bid-that-never-reached-the-screen")

            bannerView.setup(superseded, path = "/search", location = null) { _, _ -> }
            bannerView.setup(winner, path = "/search", location = null) { _, _ -> }
            bannerView.viewTreeObserver.dispatchOnGlobalLayout()
            EventPipelineHarness.runPendingEventWork()

            assertThat(fake.impressionsSent.flatMap { it.impressions }.map { it.resolvedBidId })
                .containsExactly(winner.resolvedBidId)
        }
    }

    /**
     * No auction is run, so onNoWinners has nothing to describe - an absent winner means the
     * caller should not have called this at all.
     */
    @Test
    fun it_never_reports_no_winners() {
        runBlocking(Dispatchers.Main) {
        var sawNoWinners = false

        bannerView
            .onNoWinners { sawNoWinners = true }
            .setup(winner, path = "/search", location = null) { _, _ -> }

        assertThat(sawNoWinners).isFalse()
        }
    }
}
