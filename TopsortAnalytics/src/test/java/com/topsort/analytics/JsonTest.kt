package com.topsort.analytics

import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
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

    @Test
    fun `placement toJsonObject with null categoryIds does not crash`() {
        val placement = Placement(
            path = "test/path",
            categoryIds = null,
        )

        val json = placement.toJsonObject()

        assertThat(json.getString("path")).isEqualTo("test/path")
        assertThat(json.isNull("categoryIds")).isTrue()
    }

    @Test
    fun `placement toJsonObject with categoryIds serializes array`() {
        val placement = Placement(
            path = "test/path",
            categoryIds = listOf("cat1", "cat2"),
        )

        val json = placement.toJsonObject()

        assertThat(json.getString("path")).isEqualTo("test/path")
        val categoryArray = json.getJSONArray("categoryIds")
        assertThat(categoryArray.length()).isEqualTo(2)
        assertThat(categoryArray.getString(0)).isEqualTo("cat1")
        assertThat(categoryArray.getString(1)).isEqualTo("cat2")
    }

    @Test
    fun `placement roundtrip with all optional fields null`() {
        val placement = Placement(path = "minimal/path")

        val serialized = placement.toJsonObject().toString()
        val deserialized = Placement.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized.path).isEqualTo("minimal/path")
        assertThat(deserialized.position).isNull()
        assertThat(deserialized.page).isNull()
        assertThat(deserialized.pageSize).isNull()
        assertThat(deserialized.productId).isNull()
        assertThat(deserialized.categoryIds).isNull()
        assertThat(deserialized.searchQuery).isNull()
        assertThat(deserialized.location).isNull()
    }

    @Test
    fun `placement roundtrip with all fields populated`() {
        val placement = Placement(
            path = "full/path",
            position = 3,
            page = 2,
            pageSize = 10,
            productId = "prod-1",
            categoryIds = listOf("cat-a", "cat-b"),
            searchQuery = "shoes",
            location = "top-banner",
        )

        val serialized = placement.toJsonObject().toString()
        val deserialized = Placement.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(placement)
    }

    @Test
    fun `placement build factory with null categoryIds`() {
        val placement = Placement.build(
            path = "factory/path",
            categoryIds = null,
        )

        val json = placement.toJsonObject()

        assertThat(json.getString("path")).isEqualTo("factory/path")
        assertThat(json.isNull("categoryIds")).isTrue()
    }

    // Tests for enhanced event context fields

    @Test
    fun `json click serialization with enhanced context fields`() {
        val clicks = listOf(
            getClickPromotedWithContext(),
            getClickOrganicWithContext()
        )

        for (click in clicks) {
            val serialized = click.toJsonObject().toString()
            val deserialized = Click.Factory.fromJsonObject(JSONObject(serialized))

            assertThat(click).isNotSameAs(deserialized)
            assertThat(click).isEqualTo(deserialized)
            assertThat(deserialized.deviceType).isNotNull()
            assertThat(deserialized.channel).isNotNull()
            assertThat(deserialized.page).isNotNull()
            assertThat(deserialized.clickType).isNotNull()
        }
    }

    @Test
    fun `json impression serialization with enhanced context fields`() {
        val impressions = listOf(
            getImpressionPromotedWithContext(),
            getImpressionOrganicWithContext()
        )

        for (impression in impressions) {
            val serialized = impression.toJsonObject().toString()
            val deserialized = Impression.Factory.fromJsonObject(JSONObject(serialized))

            assertThat(impression).isNotSameAs(deserialized)
            assertThat(impression).isEqualTo(deserialized)
            assertThat(deserialized.deviceType).isNotNull()
            assertThat(deserialized.channel).isNotNull()
            assertThat(deserialized.page).isNotNull()
        }
    }

    @Test
    fun `json purchase serialization with enhanced context fields`() {
        val purchase = getRandomPurchaseWithContext()
        val serialized = purchase.toJsonObject().toString()
        val deserialized = Purchase.fromJsonObject(JSONObject(serialized))

        assertThat(purchase).isNotSameAs(deserialized)
        assertThat(purchase).isEqualTo(deserialized)
        assertThat(deserialized.deviceType).isNotNull()
        assertThat(deserialized.channel).isNotNull()
        assertThat(deserialized.items.first().vendorId).isNotNull()
    }

    @Test
    fun `click constants are correctly defined`() {
        assertThat(Click.CLICK_TYPE_PRODUCT).isEqualTo("product")
        assertThat(Click.CLICK_TYPE_LIKE).isEqualTo("like")
        assertThat(Click.CLICK_TYPE_ADD_TO_CART).isEqualTo("add-to-cart")
    }

    @Test
    fun `click deserialization handles missing optional context fields`() {
        // Simulate JSON from older SDK version without new fields
        val jsonWithoutNewFields = """
            {
                "resolvedBidId": "bid-123",
                "placement": {"path": "test"},
                "occurredAt": "2024-01-01T00:00:00Z",
                "opaqueUserId": "user-123",
                "id": "click-123"
            }
        """.trimIndent()

        val click = Click.Factory.fromJsonObject(JSONObject(jsonWithoutNewFields))

        assertThat(click.resolvedBidId).isEqualTo("bid-123")
        assertThat(click.deviceType).isNull()
        assertThat(click.channel).isNull()
        assertThat(click.page).isNull()
        assertThat(click.clickType).isNull()
    }

    @Test
    fun `impression deserialization handles missing optional context fields`() {
        val jsonWithoutNewFields = """
            {
                "resolvedBidId": "bid-123",
                "placement": {"path": "test"},
                "occurredAt": "2024-01-01T00:00:00Z",
                "opaqueUserId": "user-123",
                "id": "impression-123"
            }
        """.trimIndent()

        val impression = Impression.Factory.fromJsonObject(JSONObject(jsonWithoutNewFields))

        assertThat(impression.resolvedBidId).isEqualTo("bid-123")
        assertThat(impression.deviceType).isNull()
        assertThat(impression.channel).isNull()
        assertThat(impression.page).isNull()
    }

    @Test
    fun `purchase deserialization handles missing optional context fields`() {
        val jsonWithoutNewFields = """
            {
                "occurredAt": "2024-01-01T00:00:00Z",
                "opaqueUserId": "user-123",
                "id": "purchase-123",
                "items": [{"productId": "prod-1", "quantity": 1}]
            }
        """.trimIndent()

        val purchase = Purchase.fromJsonObject(JSONObject(jsonWithoutNewFields))

        assertThat(purchase.id).isEqualTo("purchase-123")
        assertThat(purchase.deviceType).isNull()
        assertThat(purchase.channel).isNull()
        assertThat(purchase.items.first().vendorId).isNull()
    }

    @Test
    fun `click serialization omits null optional fields from json`() {
        val click = getClickPromoted() // Has null deviceType, channel, page, clickType

        val json = click.toJsonObject()

        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
        assertThat(json.has("page")).isFalse()
        assertThat(json.has("clickType")).isFalse()
    }

    @Test
    fun `impression serialization omits null optional fields from json`() {
        val impression = getImpressionPromoted()

        val json = impression.toJsonObject()

        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
        assertThat(json.has("page")).isFalse()
    }

    @Test
    fun `purchase serialization omits null optional fields from json`() {
        val purchase = getRandomPurchase()

        val json = purchase.toJsonObject()

        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
    }
}
