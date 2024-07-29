package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Purchase
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class JsonTestAndroid {

    @Test
    fun json_click_serialization() {
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
    fun json_impression_serialization() {
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
    fun json_purchase_serialization() {
        val purchase = getRandomPurchase()
        val serialized = purchase.toJsonObject().toString()
        val deserialized = Purchase.fromJsonObject(JSONObject(serialized))

        assertThat(purchase).isNotSameAs(deserialized)
        assertThat(purchase).isEqualTo(deserialized)
    }
}
