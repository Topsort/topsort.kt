package com.topsort.analytics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
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
            opaqueUserId = "test-user-id",
            token = "test-token"
        )
    }

    @After
    fun cleanup() {
        // Clear SharedPreferences to ensure test isolation
        context.getSharedPreferences("TOPSORT_EVENTS_CACHE", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("TOPSORT_EVENTS_CACHE_ENCRYPTED", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ==================== Token and session storage tests ====================

    @Test
    fun setup_stores_token() {
        Cache.setup(context, "user-123", "my-api-token")

        assertThat(Cache.token).isEqualTo("my-api-token")
    }

    @Test
    fun setup_can_update_token() {
        Cache.setup(context, "user-1", "token-1")
        Cache.setup(context, "user-2", "token-2")

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

        // All IDs should be unique and incrementing
        assertThat(setOf(impressionId, clickId, purchaseId, pageViewId)).hasSize(4)
        assertThat(clickId).isGreaterThan(impressionId)
        assertThat(purchaseId).isGreaterThan(clickId)
        assertThat(pageViewId).isGreaterThan(purchaseId)
    }

    @Test
    fun reading_wrong_event_type_throws_or_returns_mismatched_data() {
        val impression = getTestImpressionEvent()
        val recordId = Cache.storeImpression(impression)

        // Reading as click will fail because JSON has "impressions" not "clicks"
        val exceptionThrown = try {
            Cache.readClick(recordId)
            false
        } catch (e: Exception) {
            // Expected - JSON structure is incompatible
            true
        }

        assertThat(exceptionThrown).isTrue()

        // Reading as impression should work
        val asImpression = Cache.readImpression(recordId)
        assertThat(asImpression).isNotNull
    }

    // ==================== Persistence tests ====================

    @Test
    fun stored_events_persist_after_reinitialize() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)

        // Reinitialize cache (simulates app restart)
        Cache.setup(context, "test-user-id", "test-token")

        // Event should still be readable
        val retrieved = Cache.readImpression(recordId)
        assertThat(retrieved).isNotNull
        assertThat(retrieved!!.impressions[0].resolvedBidId)
            .isEqualTo(event.impressions[0].resolvedBidId)
    }
}
