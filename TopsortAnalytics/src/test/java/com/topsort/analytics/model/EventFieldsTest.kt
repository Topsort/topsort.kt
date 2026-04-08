package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class EventFieldsTest {

    private fun getTestPlacement(): Placement {
        return Placement(
            path = "test",
            position = 1,
            page = 1,
            pageSize = 10,
        )
    }

    // Impression tests for new fields

    @Test
    fun `impression buildPromoted with all new fields`() {
        val page = Page.Factory.build(type = Page.TYPE_PDP)
        val additionalEntity = Entity(id = "vendor-123", type = EntityType.VENDOR)

        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "imp-456",
            deviceType = "mobile",
            channel = "onsite",
            page = page,
            additionalAttributionEntity = additionalEntity
        )

        assertThat(impression.deviceType).isEqualTo("mobile")
        assertThat(impression.channel).isEqualTo("onsite")
        assertThat(impression.page).isEqualTo(page)
        assertThat(impression.additionalAttributionEntity).isEqualTo(additionalEntity)
    }

    @Test
    fun `impression buildOrganic with all new fields`() {
        val entity = Entity(id = "product-123", type = EntityType.PRODUCT)
        val page = Page.Factory.buildWithId(type = Page.TYPE_CATEGORY, pageId = "cat-1")

        val impression = Impression.Factory.buildOrganic(
            entity = entity,
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "imp-456",
            deviceType = "desktop",
            channel = "offsite",
            page = page
        )

        assertThat(impression.deviceType).isEqualTo("desktop")
        assertThat(impression.channel).isEqualTo("offsite")
        assertThat(impression.page).isEqualTo(page)
    }

    @Test
    fun `impression toJsonObject includes new fields when present`() {
        val page = Page.Factory.build(type = Page.TYPE_HOME)

        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "imp-456",
            deviceType = "mobile",
            channel = "instore",
            page = page
        )

        val json = impression.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("mobile")
        assertThat(json.getString("channel")).isEqualTo("instore")
        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("home")
    }

    @Test
    fun `impression toJsonObject omits new fields when null`() {
        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "imp-456"
        )

        val json = impression.toJsonObject()

        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
        assertThat(json.has("page")).isFalse()
    }

    @Test
    fun `impression roundtrip with all new fields`() {
        val page = Page.Factory.buildWithValues(
            type = Page.TYPE_SEARCH,
            values = listOf("shoes")
        )
        val additionalEntity = Entity(id = "vendor-1", type = EntityType.VENDOR)

        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "imp-456",
            deviceType = "mobile",
            channel = "onsite",
            page = page,
            additionalAttributionEntity = additionalEntity
        )

        val serialized = impression.toJsonObject().toString()
        val deserialized = Impression.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized.deviceType).isEqualTo("mobile")
        assertThat(deserialized.channel).isEqualTo("onsite")
        assertThat(deserialized.page).isEqualTo(page)
        assertThat(deserialized.additionalAttributionEntity).isEqualTo(additionalEntity)
    }

    // Click tests for new fields

    @Test
    fun `click buildPromoted with all new fields`() {
        val page = Page.Factory.build(type = Page.TYPE_PDP)

        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "click-456",
            deviceType = "mobile",
            channel = "onsite",
            page = page,
            clickType = Click.TYPE_ADD_TO_CART
        )

        assertThat(click.deviceType).isEqualTo("mobile")
        assertThat(click.channel).isEqualTo("onsite")
        assertThat(click.page).isEqualTo(page)
        assertThat(click.clickType).isEqualTo("add-to-cart")
    }

    @Test
    fun `click buildOrganic with clickType`() {
        val entity = Entity(id = "product-123", type = EntityType.PRODUCT)

        val click = Click.Factory.buildOrganic(
            entity = entity,
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "click-456",
            clickType = Click.TYPE_LIKE
        )

        assertThat(click.clickType).isEqualTo("like")
    }

    @Test
    fun `click toJsonObject includes clickType when present`() {
        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "click-456",
            clickType = Click.TYPE_PRODUCT
        )

        val json = click.toJsonObject()

        assertThat(json.getString("clickType")).isEqualTo("product")
    }

    @Test
    fun `click roundtrip with all new fields`() {
        val page = Page.Factory.build(type = Page.TYPE_CART)
        val additionalEntity = Entity(id = "vendor-1", type = EntityType.VENDOR)

        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = getTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "click-456",
            deviceType = "desktop",
            channel = "offsite",
            page = page,
            clickType = Click.TYPE_ADD_TO_CART,
            additionalAttributionEntity = additionalEntity
        )

        val serialized = click.toJsonObject().toString()
        val deserialized = Click.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized.deviceType).isEqualTo("desktop")
        assertThat(deserialized.channel).isEqualTo("offsite")
        assertThat(deserialized.page).isEqualTo(page)
        assertThat(deserialized.clickType).isEqualTo("add-to-cart")
        assertThat(deserialized.additionalAttributionEntity).isEqualTo(additionalEntity)
    }

    // Purchase tests for new fields

    @Test
    fun `purchase with deviceType and channel`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "order-456",
            items = listOf(
                PurchasedItem(productId = "prod-1", quantity = 1)
            ),
            deviceType = "mobile",
            channel = "onsite"
        )

        assertThat(purchase.deviceType).isEqualTo("mobile")
        assertThat(purchase.channel).isEqualTo("onsite")
    }

    @Test
    fun `purchase toJsonObject includes new fields when present`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "order-456",
            items = listOf(
                PurchasedItem(productId = "prod-1", quantity = 1)
            ),
            deviceType = "desktop",
            channel = "instore"
        )

        val json = purchase.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("desktop")
        assertThat(json.getString("channel")).isEqualTo("instore")
    }

    @Test
    fun `purchase roundtrip with new fields`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "order-456",
            items = listOf(
                PurchasedItem(
                    productId = "prod-1",
                    quantity = 2,
                    unitPrice = 1000,
                    vendorId = "vendor-123"
                )
            ),
            deviceType = "mobile",
            channel = "offsite"
        )

        val serialized = purchase.toJsonObject().toString()
        val deserialized = Purchase.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized.deviceType).isEqualTo("mobile")
        assertThat(deserialized.channel).isEqualTo("offsite")
    }

    // PurchasedItem tests for vendorId

    @Test
    fun `purchasedItem with vendorId`() {
        val item = PurchasedItem(
            productId = "prod-123",
            quantity = 1,
            unitPrice = 500,
            resolvedBidId = "bid-456",
            vendorId = "vendor-789"
        )

        assertThat(item.vendorId).isEqualTo("vendor-789")
    }

    @Test
    fun `purchasedItem toJsonObject includes vendorId when present`() {
        val item = PurchasedItem(
            productId = "prod-123",
            quantity = 1,
            vendorId = "vendor-789"
        )

        val json = item.toJsonObject()

        assertThat(json.getString("vendorId")).isEqualTo("vendor-789")
    }

    @Test
    fun `purchasedItem toJsonObject omits vendorId when null`() {
        val item = PurchasedItem(
            productId = "prod-123",
            quantity = 1
        )

        val json = item.toJsonObject()

        assertThat(json.has("vendorId")).isFalse()
    }

    @Test
    fun `purchasedItem roundtrip with vendorId`() {
        val item = PurchasedItem(
            productId = "prod-123",
            quantity = 2,
            unitPrice = 1000,
            resolvedBidId = "bid-456",
            vendorId = "vendor-789"
        )

        val serialized = item.toJsonObject().toString()
        val deserialized = PurchasedItem.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(item)
    }

    // Test additionalAttribution as Entity deserializes correctly

    @Test
    fun `impression deserializes additionalAttribution as entity when json object`() {
        val json = """
            {
                "resolvedBidId": "bid-123",
                "placement": {"path": "test", "position": 1},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-123",
                "id": "imp-456",
                "additionalAttribution": {"id": "entity-123", "type": "vendor"}
            }
        """.trimIndent()

        val impression = Impression.Factory.fromJsonObject(JSONObject(json))

        assertThat(impression.additionalAttribution).isNull()
        assertThat(impression.additionalAttributionEntity).isNotNull
        assertThat(impression.additionalAttributionEntity!!.id).isEqualTo("entity-123")
        assertThat(impression.additionalAttributionEntity!!.type).isEqualTo(EntityType.VENDOR)
    }

    @Test
    fun `impression deserializes additionalAttribution as string when string`() {
        val json = """
            {
                "resolvedBidId": "bid-123",
                "placement": {"path": "test", "position": 1},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-123",
                "id": "imp-456",
                "additionalAttribution": "some-string-value"
            }
        """.trimIndent()

        val impression = Impression.Factory.fromJsonObject(JSONObject(json))

        assertThat(impression.additionalAttribution).isEqualTo("some-string-value")
        assertThat(impression.additionalAttributionEntity).isNull()
    }
}
