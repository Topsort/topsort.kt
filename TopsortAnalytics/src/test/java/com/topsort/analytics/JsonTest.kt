package com.topsort.analytics

import com.topsort.analytics.model.events.Click
import com.topsort.analytics.model.events.Impression
import com.topsort.analytics.model.events.Purchase
import org.json.JSONObject
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

internal class JsonTest {

    @Test
    fun `json click serialization`() {
        val clicks = listOf(
            getClickPromoted(),
            getClickOrganic()
        )

        for(click in clicks) {
            val serialized = click.toJsonObject().toString()
            val deserialized = Click.Factory.fromJsonObject(JSONObject(serialized))

            assertThat(click).isNotSameAs(deserialized)
            assertThat(click).isEqualTo(deserialized)
        }
    }

    @Test
    fun `json impression serialization`() {
        val impressions = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        for(impression in impressions) {
            val serialized = impression.toJsonObject().toString()
            val deserialized = Impression.Factory.fromJsonObject(JSONObject(serialized))

            assertThat(impression).isNotSameAs(deserialized)
            assertThat(impression).isEqualTo(deserialized)
        }
    }

    @Test
    fun `json purchase serialization`() {
        val purchase = getRandomPurchase()
        val serialized = purchase.toJsonObject().toString()
        val deserialized = Purchase.fromJsonObject(JSONObject(serialized))

        assertThat(purchase).isNotSameAs(deserialized)
        assertThat(purchase).isEqualTo(deserialized)
    }
}
