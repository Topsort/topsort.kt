package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Placement
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression cover for INTE-2706, where one marketplace's Android traffic reported 61 impressions
 * per winning auction against exactly 1.00 on web and iOS, because an integration that had forked
 * [com.topsort.analytics.banners.BannerView] re-fired the beacon for a bid it had already
 * reported. A resolved bid earns one impression, and on a CPM campaign every repeat is billed, so
 * the SDK enforces that below the public API rather than trusting each caller to get it right.
 */
@RunWith(AndroidJUnit4::class)
class ImpressionDeduplicationTest {

    private lateinit var fake: FakeAnalyticsHttpService

    private fun setUpWith(opaqueUserId: String = EventPipelineHarness.OPAQUE_USER_ID) {
        fake = EventPipelineHarness.install()
        setup(requireNotNull(UserIdentity.Marketplace.of(opaqueUserId)), EventPipelineHarness.TOKEN)
    }

    private fun setup(identity: UserIdentity, token: String) =
        Analytics.setup(EventPipelineHarness.application, identity, token)

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    private fun reportImpression(bidId: String) {
        Analytics.reportImpressionPromoted(
            resolvedBidId = bidId,
            placement = Placement(path = "/dedup"),
        )
    }

    /** The reported failure, in miniature: one bid, many reports, one impression on the wire. */
    @Test
    fun a_bid_reported_many_times_is_sent_once() {
        setUpWith()

        repeat(50) { reportImpression("bid-looping") }
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(fake.impressionsSent.single().impressions).hasSize(1)
    }

    /** A repeat must not even reach the cache, or the sweep would deliver it later. */
    @Test
    fun a_repeat_is_never_cached() {
        setUpWith()

        reportImpression("bid-cached-once")
        reportImpression("bid-cached-once")

        EventPipelineHarness.runPendingEventWork()
        // A record the dedup let through would surface here on the sweep as a second delivery.
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /** Deduplication is per bid: distinct ads in the same session all report. */
    @Test
    fun distinct_bids_all_report() {
        setUpWith()

        reportImpression("bid-a")
        reportImpression("bid-b")
        reportImpression("bid-a")
        reportImpression("bid-c")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent.flatMap { it.impressions }.map { it.resolvedBidId })
            .containsExactlyInAnyOrder("bid-a", "bid-b", "bid-c")
    }

    /**
     * Organic impressions carry no bid, are not billable, and legitimately repeat as the same
     * entity appears elsewhere, so they must pass through untouched.
     */
    @Test
    fun organic_impressions_are_not_deduplicated() {
        setUpWith()

        repeat(3) {
            Analytics.reportImpressionOrganic(
                entity = Entity(type = EntityType.PRODUCT, id = "product-1"),
                placement = Placement(path = "/dedup"),
            )
        }
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent.flatMap { it.impressions }).hasSize(3)
    }

    /**
     * A click is a deliberate act the user can repeat, and on CPC the marketplace is entitled to
     * every one, so clicks are not filtered the way impressions are.
     */
    @Test
    fun clicks_are_not_deduplicated() {
        setUpWith()

        repeat(3) {
            Analytics.reportClickPromoted(
                resolvedBidId = "bid-clicked",
                placement = Placement(path = "/dedup"),
            )
        }
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.sent.filterIsInstance<com.topsort.analytics.model.ClickEvent>()).hasSize(3)
    }

    /**
     * setup() is also how a caller refreshes an expired token, and [UserIdentity.Unidentified]
     * keeps the id already in effect. That is the same user, so the tracked bids must survive -
     * clearing them would reopen the duplicate this guards against.
     */
    @Test
    fun a_token_refresh_for_the_same_user_still_deduplicates() {
        setUpWith(opaqueUserId = "marketplace-id")

        reportImpression("bid-across-refresh")
        EventPipelineHarness.runPendingEventWork()

        // Unidentified deliberately keeps "marketplace-id" in effect.
        setup(UserIdentity.Unidentified, "refreshed-token")
        reportImpression("bid-across-refresh")
        EventPipelineHarness.runPendingEventWork()

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * The same invariant through the deprecated overload, whose blank maps to
     * [UserIdentity.Unidentified]. Kept so the legacy path cannot regress while callers migrate.
     */
    @Suppress("DEPRECATION")
    @Test
    fun a_token_refresh_through_the_deprecated_overload_still_deduplicates() {
        setUpWith(opaqueUserId = "marketplace-id")

        reportImpression("bid-legacy-refresh")
        EventPipelineHarness.runPendingEventWork()

        Analytics.setup(EventPipelineHarness.application, "", "refreshed-token")
        reportImpression("bid-legacy-refresh")
        EventPipelineHarness.runPendingEventWork()

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
        assertThat(fake.impressionsSent).hasSize(1)
    }

    /** Re-supplying the same id is likewise the same user, so the tracked bids survive. */
    @Test
    fun repeating_setup_with_the_same_user_still_deduplicates() {
        setUpWith(opaqueUserId = "marketplace-id")

        reportImpression("bid-repeat-setup")
        EventPipelineHarness.runPendingEventWork()

        setup(requireNotNull(UserIdentity.Marketplace.of("marketplace-id")), EventPipelineHarness.TOKEN)
        reportImpression("bid-repeat-setup")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
    }

    /**
     * A second setup() may be a different user, whose impressions are their own and must not be
     * dropped as duplicates of the previous user's.
     */
    @Test
    fun a_new_session_reports_a_bid_again() {
        setUpWith()

        reportImpression("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        setup(requireNotNull(UserIdentity.Marketplace.of("second-user")), EventPipelineHarness.TOKEN)
        reportImpression("bid-shared")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(2)
        assertThat(fake.impressionsSent.map { it.impressions.single().opaqueUserId })
            .containsExactly(EventPipelineHarness.OPAQUE_USER_ID, "second-user")
    }
}
