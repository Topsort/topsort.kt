package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import com.topsort.analytics.model.Placement
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Delivery behaviour: one event's failure must not touch another, a permanent rejection must not
 * leave its record behind, and a blank opaqueUserId must never reach the wire.
 */
@RunWith(AndroidJUnit4::class)
class EventDeliveryTest {

    private lateinit var fake: FakeAnalyticsHttpService

    private fun setUpWith(opaqueUserId: String = EventPipelineHarness.OPAQUE_USER_ID) {
        fake = EventPipelineHarness.install()
        Analytics.setup(EventPipelineHarness.application, opaqueUserId, EventPipelineHarness.TOKEN)
    }

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    private fun reportImpression(bidId: String) {
        Analytics.reportImpressionPromoted(
            resolvedBidId = bidId,
            placement = Placement(path = "/delivery"),
        )
    }

    /**
     * Events used to share one work chain, so a terminal failure on any of them silenced every
     * event enqueued afterwards. Each record now has its own work unit, so a failure is isolated.
     */
    @Test
    fun a_failed_event_does_not_stop_later_events() {
        setUpWith()
        fake.scriptNext(FakeAnalyticsHttpService.BAD_REQUEST_CODE)

        reportImpression("bid-fails")
        EventPipelineHarness.runPendingEventWork()

        reportImpression("bid-after-failure")
        EventPipelineHarness.runPendingEventWork()

        // Both were attempted: the first was rejected, the second still went out.
        assertThat(fake.impressionsSent).hasSize(2)
        assertThat(EventPipelineHarness.eventWork().map { it.state })
            .contains(WorkInfo.State.SUCCEEDED)
    }

    /**
     * The API rejects a blank opaqueUserId with 400, which is what used to terminate the chain in
     * the first place, so the SDK must never emit one.
     */
    @Test
    fun a_blank_opaque_user_id_is_never_reported() {
        setUpWith(opaqueUserId = "")

        reportImpression("bid-blank-opaque")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(fake.impressionsSent.first().impressions.first().opaqueUserId).isNotBlank()
    }

    /** A blank per-call override must fall back to the session id rather than reach the wire. */
    @Test
    fun a_blank_per_call_opaque_user_id_falls_back_to_the_session_id() {
        setUpWith()

        Analytics.reportImpressionPromoted(
            resolvedBidId = "bid-blank-override",
            placement = Placement(path = "/regression"),
            opaqueUserId = "",
        )
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent.first().impressions.first().opaqueUserId)
            .isEqualTo(EventPipelineHarness.OPAQUE_USER_ID)
    }

    /**
     * Audience matching needs the marketplace's own identifier, so a blank must never downgrade an
     * id we already hold to a generated placeholder.
     */
    @Test
    fun a_blank_never_replaces_an_already_supplied_opaque_user_id() {
        setUpWith(opaqueUserId = "marketplace-id")

        Analytics.setup(EventPipelineHarness.application, "", EventPipelineHarness.TOKEN)

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    /** Conversely, a supplied id always replaces a generated placeholder. */
    @Test
    fun a_supplied_opaque_user_id_replaces_a_generated_placeholder() {
        setUpWith(opaqueUserId = "")
        val placeholder = Analytics.opaqueUserId
        assertThat(placeholder).isNotBlank()

        Analytics.setup(
            EventPipelineHarness.application,
            "marketplace-id",
            EventPipelineHarness.TOKEN,
        )

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
        assertThat(Analytics.opaqueUserId).isNotEqualTo(placeholder)
    }

    @Test
    fun the_effective_opaque_user_id_is_exposed() {
        setUpWith(opaqueUserId = "marketplace-id")

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    /**
     * A 4xx is permanent, so the record must go. Left in place it would be re-read and retried by
     * every subsequent sweep for the lifetime of the install.
     */
    @Test
    fun a_permanently_rejected_event_is_removed_from_the_cache() {
        setUpWith()
        fake.scriptNext(FakeAnalyticsHttpService.BAD_REQUEST_CODE)

        reportImpression("bid-rejected")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(Cache.cachedRecordIds()).isEmpty()
    }

    /** A 5xx is transient, so the record must survive for the retry. */
    @Test
    fun a_transient_failure_leaves_the_event_in_the_cache() {
        setUpWith()
        fake.scriptNext(FakeAnalyticsHttpService.SERVER_ERROR_CODE)

        reportImpression("bid-transient")
        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(1)
        assertThat(Cache.cachedRecordIds()).isNotEmpty()
    }

    /**
     * A record id is reused if the cache's counter is ever reset while the WorkManager database
     * still holds the terminated work unit from the previous owner of that id - so the same unique
     * work name is enqueued twice, the second time while a FAILED entry under that name exists.
     *
     * This pins the WorkManager semantics the per-record fix depends on: KEEP only keeps work that
     * is *pending*. FAILED and CANCELLED are finished states, so the new request is inserted rather
     * than silently dropped. If that ever stopped being true, every reused id would go dark and
     * nothing else in the suite would notice.
     */
    @Test
    fun a_work_name_reused_after_a_failed_send_still_delivers() {
        setUpWith()
        fake.scriptNext(FakeAnalyticsHttpService.BAD_REQUEST_CODE)

        reportImpression("bid-rejected")
        val reusedRecordId = Cache.cachedRecordIds().single()
        EventPipelineHarness.runPendingEventWork()
        assertThat(EventPipelineHarness.eventWork().map { it.state })
            .contains(WorkInfo.State.FAILED)

        // Reset the counter so the next event lands on the same record id, and so on the same
        // unique work name, while that FAILED work unit is still in the database.
        Cache.clearForTests()
        reportImpression("bid-reusing-the-id")
        assertThat(Cache.cachedRecordIds()).containsExactly(reusedRecordId)

        EventPipelineHarness.runPendingEventWork()

        assertThat(fake.impressionsSent).hasSize(2)
        assertThat(Cache.cachedRecordIds()).isEmpty()
    }
}
