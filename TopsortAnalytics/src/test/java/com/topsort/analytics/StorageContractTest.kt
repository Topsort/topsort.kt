package com.topsort.analytics

import com.topsort.analytics.model.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Pins the names in StorageContract.kt. Events already stored in installed apps are found by
 * these exact strings, so changing one strands them. If this test needs editing, the change needs
 * a migration.
 */
class StorageContractTest {

    @Test
    fun `preference file names`() {
        assertThat(PREFERENCES_NAME).isEqualTo("TOPSORT_EVENTS_CACHE")
        assertThat(ENCRYPTED_PREFERENCES_NAME).isEqualTo("TOPSORT_EVENTS_CACHE_ENCRYPTED")
    }

    @Test
    fun `preference keys`() {
        assertThat(KEY_TOKEN).isEqualTo("KEY_TOKEN")
        assertThat(KEY_SESSION_ID).isEqualTo("KEY_SESSION_ID")
        assertThat(KEY_RECORD).isEqualTo("KEY_RECORD_%d")
        assertThat(KEY_RECORD_PREFIX).isEqualTo("KEY_RECORD_")
        assertThat(KEY_RECENT_RECORD_ID).isEqualTo("KEY_RECORD_ID")
    }

    @Test
    fun `work names and input data keys`() {
        assertThat(EMITTER_WORK_NAME).isEqualTo("TopsortAnalyticsReporter")
        assertThat(SWEEP_WORK_NAME).isEqualTo("TopsortAnalyticsPendingSweep")
        assertThat(EXTRA_RECORD_ID).isEqualTo("EXTRA_RECORD_ID")
        assertThat(EXTRA_EVENT_TYPE).isEqualTo("EXTRA_EVENT_TYPE")
    }

    @Test
    fun `json key to event type mapping`() {
        assertThat(EVENT_TYPE_BY_JSON_KEY).containsExactly(
            entry("impressions", EventType.Impression),
            entry("clicks", EventType.Click),
            entry("purchases", EventType.Purchase),
            entry("pageviews", EventType.PageView),
            entry("renders", EventType.Render),
        )
    }

    /** The ordinal is stored in work data, so reordering or inserting strands queued work. */
    @Test
    fun `event type order`() {
        assertThat(EventType.values().map { it.name })
            .containsExactly("Impression", "Click", "Purchase", "PageView", "Render")
    }

    private fun entry(key: String, value: EventType) = java.util.AbstractMap.SimpleEntry(key, value)
}
