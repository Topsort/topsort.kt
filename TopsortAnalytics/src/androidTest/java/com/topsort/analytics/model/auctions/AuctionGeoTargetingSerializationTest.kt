package com.topsort.analytics.model.auctions

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against Android's org.json. The JVM unit tests use the desktop artifact, whose
 * [org.json.JSONObject.wrap] turns a Kotlin data class into JSON. Android's returns null.
 */
@RunWith(AndroidJUnit4::class)
class AuctionGeoTargetingSerializationTest {

    @Test
    fun toJsonObject_keeps_geoTargeting_on_android_org_json() {
        val auction = Auction.fromConfig(
            AuctionConfig.ProductIds(
                numSlots = 1,
                ids = listOf("p1"),
                geo = "BR",
            )
        )

        val json = auction.toJsonObject()

        assertThat(json.has("geoTargeting")).isTrue()
        assertThat(json.getJSONObject("geoTargeting").getString("location")).isEqualTo("BR")
    }
}
