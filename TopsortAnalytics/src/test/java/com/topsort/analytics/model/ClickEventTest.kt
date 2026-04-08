package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class ClickEventTest {

    private fun createTestPlacement() = Placement(
        path = "/test/path",
        position = 1,
        page = 1,
        pageSize = 20
    )

    @Test
    fun `buildPromoted creates click with resolvedBidId`() {
        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-456",
            id = "click-789"
        )

        assertThat(click.resolvedBidId).isEqualTo("bid-123")
        assertThat(click.entity).isNull()
        assertThat(click.opaqueUserId).isEqualTo("user-456")
        assertThat(click.id).isEqualTo("click-789")
    }

    @Test
    fun `buildOrganic creates click with entity`() {
        val entity = Entity(id = "product-123", type = EntityType.PRODUCT)
        val click = Click.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-456",
            id = "click-789"
        )

        assertThat(click.entity).isEqualTo(entity)
        assertThat(click.resolvedBidId).isNull()
    }

    @Test
    fun `toJsonObject for promoted click contains resolvedBidId`() {
        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-abc",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "click-1"
        )

        val json = click.toJsonObject()

        assertThat(json.getString("resolvedBidId")).isEqualTo("bid-abc")
        assertThat(json.has("entity")).isFalse()
    }

    @Test
    fun `toJsonObject for organic click contains entity`() {
        val entity = Entity(id = "vendor-123", type = EntityType.VENDOR)
        val click = Click.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "click-1"
        )

        val json = click.toJsonObject()

        assertThat(json.has("resolvedBidId")).isFalse()
        assertThat(json.getJSONObject("entity").getString("id")).isEqualTo("vendor-123")
    }

    @Test
    fun `toJsonObject includes all optional fields when present`() {
        val click = Click.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "click-1",
            additionalAttribution = """{"key": "value"}""",
            deviceType = "mobile",
            channel = "onsite",
            page = Page.Factory.build(type = Page.TYPE_SEARCH),
            clickType = Click.TYPE_ADD_TO_CART
        )

        val json = click.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("mobile")
        assertThat(json.getString("channel")).isEqualTo("onsite")
        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("search")
        assertThat(json.getString("clickType")).isEqualTo("add-to-cart")
    }

    @Test
    fun `fromJsonObject deserializes promoted click`() {
        val json = JSONObject("""
            {
                "resolvedBidId": "bid-test",
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-test",
                "id": "click-test"
            }
        """.trimIndent())

        val click = Click.Factory.fromJsonObject(json)

        assertThat(click.resolvedBidId).isEqualTo("bid-test")
        assertThat(click.entity).isNull()
        assertThat(click.id).isEqualTo("click-test")
    }

    @Test
    fun `fromJsonObject deserializes organic click`() {
        val json = JSONObject("""
            {
                "entity": {"id": "p1", "type": "product"},
                "placement": {"path": "/category"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "click-1"
            }
        """.trimIndent())

        val click = Click.Factory.fromJsonObject(json)

        assertThat(click.entity).isNotNull
        assertThat(click.entity!!.id).isEqualTo("p1")
        assertThat(click.resolvedBidId).isNull()
    }

    @Test
    fun `fromJsonObject handles additionalAttribution as string`() {
        val json = JSONObject("""
            {
                "resolvedBidId": "bid-1",
                "additionalAttribution": "custom-data",
                "placement": {"path": "/test"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "click-1"
            }
        """.trimIndent())

        val click = Click.Factory.fromJsonObject(json)

        assertThat(click.additionalAttribution).isEqualTo("custom-data")
        assertThat(click.additionalAttributionEntity).isNull()
    }

    @Test
    fun `fromJsonObject handles additionalAttribution as entity object`() {
        val json = JSONObject("""
            {
                "resolvedBidId": "bid-1",
                "additionalAttribution": {"id": "attr-entity", "type": "vendor"},
                "placement": {"path": "/test"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "click-1"
            }
        """.trimIndent())

        val click = Click.Factory.fromJsonObject(json)

        assertThat(click.additionalAttribution).isNull()
        assertThat(click.additionalAttributionEntity).isNotNull
        assertThat(click.additionalAttributionEntity!!.id).isEqualTo("attr-entity")
    }

    @Test
    fun `roundtrip promoted click preserves data`() {
        val original = Click.Factory.buildPromoted(
            resolvedBidId = "bid-roundtrip",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-rt",
            id = "click-rt",
            deviceType = "desktop",
            channel = "offsite",
            clickType = Click.TYPE_PRODUCT
        )

        val json = original.toJsonObject()
        val deserialized = Click.Factory.fromJsonObject(json)

        assertThat(deserialized.resolvedBidId).isEqualTo(original.resolvedBidId)
        assertThat(deserialized.opaqueUserId).isEqualTo(original.opaqueUserId)
        assertThat(deserialized.id).isEqualTo(original.id)
        assertThat(deserialized.deviceType).isEqualTo(original.deviceType)
        assertThat(deserialized.channel).isEqualTo(original.channel)
        assertThat(deserialized.clickType).isEqualTo(original.clickType)
    }

    @Test
    fun `roundtrip organic click preserves data`() {
        val entity = Entity(id = "product-rt", type = EntityType.PRODUCT)
        val original = Click.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-rt",
            id = "click-rt"
        )

        val json = original.toJsonObject()
        val deserialized = Click.Factory.fromJsonObject(json)

        assertThat(deserialized.entity).isEqualTo(original.entity)
        assertThat(deserialized.resolvedBidId).isNull()
    }

    @Test
    fun `clickEvent serialization with multiple clicks`() {
        val clicks = listOf(
            Click.Factory.buildPromoted(
                resolvedBidId = "bid-1",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-1",
                id = "click-1"
            ),
            Click.Factory.buildOrganic(
                entity = Entity(id = "p1", type = EntityType.PRODUCT),
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:31:00Z",
                opaqueUserId = "user-2",
                id = "click-2"
            )
        )
        val event = ClickEvent(clicks)

        val json = event.toJsonObject()

        assertThat(json.getJSONArray("clicks").length()).isEqualTo(2)
    }

    @Test
    fun `clickEvent roundtrip serialization`() {
        val clicks = listOf(
            Click.Factory.buildPromoted(
                resolvedBidId = "bid-rt",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-rt",
                id = "click-rt"
            )
        )
        val original = ClickEvent(clicks)

        val jsonString = original.toJsonObject().toString()
        val deserialized = ClickEvent.fromJson(jsonString)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.clicks).hasSize(1)
        assertThat(deserialized.clicks[0].resolvedBidId).isEqualTo("bid-rt")
    }

    @Test
    fun `clickEvent fromJson returns null for null input`() {
        val result = ClickEvent.fromJson(null)

        assertThat(result).isNull()
    }

    @Test
    fun `click type constants are correct`() {
        assertThat(Click.TYPE_PRODUCT).isEqualTo("product")
        assertThat(Click.TYPE_LIKE).isEqualTo("like")
        assertThat(Click.TYPE_ADD_TO_CART).isEqualTo("add-to-cart")
    }

    @Test
    fun `fromJsonArray deserializes list of clicks`() {
        val jsonArray = org.json.JSONArray("""
            [
                {
                    "resolvedBidId": "bid-1",
                    "placement": {"path": "/test1"},
                    "occurredAt": "2024-01-15T10:30:00Z",
                    "opaqueUserId": "user-1",
                    "id": "click-1"
                },
                {
                    "resolvedBidId": "bid-2",
                    "placement": {"path": "/test2"},
                    "occurredAt": "2024-01-15T10:31:00Z",
                    "opaqueUserId": "user-2",
                    "id": "click-2"
                }
            ]
        """.trimIndent())

        val clicks = Click.Factory.fromJsonArray(jsonArray)

        assertThat(clicks).hasSize(2)
        assertThat(clicks[0].id).isEqualTo("click-1")
        assertThat(clicks[1].id).isEqualTo("click-2")
    }
}
