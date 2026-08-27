package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Initialize cache with test credentials
        Cache.setup(
            context = context,
            identity = UserIdentity.of("test-user-id"),
            token = "test-token"
        )
    }

    @After
    fun cleanup() {
        Cache.clearForTests()
    }

    // ==================== Token and session storage tests ====================

    @Test
    fun setup_stores_token() {
        Cache.setup(context, UserIdentity.of("user-123"), "my-api-token")

        assertThat(Cache.token).isEqualTo("my-api-token")
    }

    @Test
    fun setup_can_update_token() {
        Cache.setup(context, UserIdentity.of("user-1"), "token-1")
        Cache.setup(context, UserIdentity.of("user-2"), "token-2")

        assertThat(Cache.token).isEqualTo("token-2")
    }

    // ==================== Impression storage tests ====================

    @Test
    fun storeImpression_returns_incrementing_record_id() {
        val event1 = getTestImpressionEvent()
        val event2 = getTestImpressionEvent()

        val id1 = Cache.storeImpression(event1)
        val id2 = Cache.storeImpression(event2)

        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun readImpression_returns_stored_event() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)

        val retrieved = Cache.readImpression(recordId)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.impressions).hasSize(1)
        assertThat(retrieved.impressions[0].resolvedBidId)
            .isEqualTo(event.impressions[0].resolvedBidId)
    }

    @Test
    fun readImpression_returns_null_for_nonexistent_id() {
        val retrieved = Cache.readImpression(999999L)

        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteEvent_removes_impression() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)

        Cache.deleteEvent(recordId)
        val retrieved = Cache.readImpression(recordId)

        assertThat(retrieved).isNull()
    }

    // ==================== Click storage tests ====================

    @Test
    fun storeClick_returns_incrementing_record_id() {
        val event1 = getTestClickEvent()
        val event2 = getTestClickEvent()

        val id1 = Cache.storeClick(event1)
        val id2 = Cache.storeClick(event2)

        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun readClick_returns_stored_event() {
        val event = getTestClickEvent()
        val recordId = Cache.storeClick(event)

        val retrieved = Cache.readClick(recordId)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.clicks).hasSize(1)
        assertThat(retrieved.clicks[0].resolvedBidId)
            .isEqualTo(event.clicks[0].resolvedBidId)
    }

    @Test
    fun readClick_returns_null_for_nonexistent_id() {
        val retrieved = Cache.readClick(999998L)

        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteEvent_removes_click() {
        val event = getTestClickEvent()
        val recordId = Cache.storeClick(event)

        Cache.deleteEvent(recordId)
        val retrieved = Cache.readClick(recordId)

        assertThat(retrieved).isNull()
    }

    // ==================== Purchase storage tests ====================

    @Test
    fun storePurchase_returns_incrementing_record_id() {
        val event1 = getTestPurchaseEvent()
        val event2 = getTestPurchaseEvent()

        val id1 = Cache.storePurchase(event1)
        val id2 = Cache.storePurchase(event2)

        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun readPurchase_returns_stored_event() {
        val event = getTestPurchaseEvent()
        val recordId = Cache.storePurchase(event)

        val retrieved = Cache.readPurchase(recordId)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.purchases).hasSize(1)
        assertThat(retrieved.purchases[0].id)
            .isEqualTo(event.purchases[0].id)
    }

    @Test
    fun readPurchase_returns_null_for_nonexistent_id() {
        val retrieved = Cache.readPurchase(999997L)

        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteEvent_removes_purchase() {
        val event = getTestPurchaseEvent()
        val recordId = Cache.storePurchase(event)

        Cache.deleteEvent(recordId)
        val retrieved = Cache.readPurchase(recordId)

        assertThat(retrieved).isNull()
    }

    // ==================== PageView storage tests ====================

    @Test
    fun storePageView_returns_incrementing_record_id() {
        val event1 = getTestPageViewEvent()
        val event2 = getTestPageViewEvent()

        val id1 = Cache.storePageView(event1)
        val id2 = Cache.storePageView(event2)

        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun readPageView_returns_stored_event() {
        val event = getTestPageViewEvent()
        val recordId = Cache.storePageView(event)

        val retrieved = Cache.readPageView(recordId)

        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.pageviews).hasSize(1)
        assertThat(retrieved.pageviews[0].id)
            .isEqualTo(event.pageviews[0].id)
    }

    @Test
    fun readPageView_returns_null_for_nonexistent_id() {
        val retrieved = Cache.readPageView(999996L)

        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteEvent_removes_pageview() {
        val event = getTestPageViewEvent()
        val recordId = Cache.storePageView(event)

        Cache.deleteEvent(recordId)
        val retrieved = Cache.readPageView(recordId)

        assertThat(retrieved).isNull()
    }

    // ==================== Mixed event type tests ====================

    @Test
    fun different_event_types_get_unique_record_ids() {
        val impression = getTestImpressionEvent()
        val click = getTestClickEvent()
        val purchase = getTestPurchaseEvent()
        val pageView = getTestPageViewEvent()

        val impressionId = Cache.storeImpression(impression)
        val clickId = Cache.storeClick(click)
        val purchaseId = Cache.storePurchase(purchase)
        val pageViewId = Cache.storePageView(pageView)

        // All IDs should be unique
        assertThat(setOf(impressionId, clickId, purchaseId, pageViewId)).hasSize(4)
    }

    /**
     * The thrown type is load-bearing, not incidental: JSONException specifically is what
     * EventEmitterWorker.doWork() catches, to prune the record and report success. Any other type
     * escapes that catch, propagates out of doWork(), and WorkManager marks the unit FAILED - a
     * materially different outcome. This is the only executable statement of that contract.
     *
     * Scoped to the read rather than annotated on the method, so a throw from storeImpression
     * cannot satisfy it.
     */
    @Test
    fun reading_wrong_event_type_throws_json_exception() {
        val impression = getTestImpressionEvent()
        val recordId = Cache.storeImpression(impression)

        // Reading as click throws because the JSON has "impressions", not "clicks".
        assertThatThrownBy { Cache.readClick(recordId) }
            .isInstanceOf(JSONException::class.java)
    }

    // ==================== Persistence tests ====================

    @Test
    fun stored_events_readable_after_setup_called_again() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)

        // Call setup again (does not truly restart the process, but re-initializes credentials)
        Cache.setup(context, UserIdentity.of("test-user-id"), "test-token")

        // Event should still be readable from SharedPreferences
        val retrieved = Cache.readImpression(recordId)
        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.impressions[0].resolvedBidId)
            .isEqualTo(event.impressions[0].resolvedBidId)
    }
}
