package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class ImpressionEventTest {

    private fun createTestPlacement() = Placement(
        path = "/test/path",
        position = 1,
        page = 1,
        pageSize = 20
    )

    @Test
    fun `buildPromoted creates impression with resolvedBidId`() {
        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-123",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-456",
            id = "imp-789"
        )

        assertThat(impression.resolvedBidId).isEqualTo("bid-123")
        assertThat(impression.entity).isNull()
        assertThat(impression.opaqueUserId).isEqualTo("user-456")
        assertThat(impression.id).isEqualTo("imp-789")
    }

    @Test
    fun `buildOrganic creates impression with entity`() {
        val entity = Entity(id = "product-123", type = EntityType.PRODUCT)
        val impression = Impression.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-456",
            id = "imp-789"
        )

        assertThat(impression.entity).isEqualTo(entity)
        assertThat(impression.resolvedBidId).isNull()
    }

    @Test
    fun `toJsonObject for promoted impression contains resolvedBidId`() {
        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-abc",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "imp-1"
        )

        val json = impression.toJsonObject()

        assertThat(json.getString("resolvedBidId")).isEqualTo("bid-abc")
        assertThat(json.has("entity")).isFalse()
    }

    @Test
    fun `toJsonObject for organic impression contains entity`() {
        val entity = Entity(id = "vendor-123", type = EntityType.VENDOR)
        val impression = Impression.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "imp-1"
        )

        val json = impression.toJsonObject()

        assertThat(json.has("resolvedBidId")).isFalse()
        assertThat(json.getJSONObject("entity").getString("id")).isEqualTo("vendor-123")
    }

    @Test
    fun `toJsonObject includes all optional fields when present`() {
        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "imp-1",
            additionalAttribution = """{"key": "value"}""",
            deviceType = "mobile",
            channel = "onsite",
            page = Page.Factory.build(type = Page.TYPE_CATEGORY)
        )

        val json = impression.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("mobile")
        assertThat(json.getString("channel")).isEqualTo("onsite")
        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("category")
    }

    @Test
    fun `fromJsonObject deserializes promoted impression`() {
        val json = JSONObject("""
            {
                "resolvedBidId": "bid-test",
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-test",
                "id": "imp-test"
            }
        """.trimIndent())

        val impression = Impression.Factory.fromJsonObject(json)

        assertThat(impression.resolvedBidId).isEqualTo("bid-test")
        assertThat(impression.entity).isNull()
        assertThat(impression.id).isEqualTo("imp-test")
    }

    @Test
    fun `fromJsonObject deserializes organic impression`() {
        val json = JSONObject("""
            {
                "entity": {"id": "p1", "type": "product"},
                "placement": {"path": "/category"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "imp-1"
            }
        """.trimIndent())

        val impression = Impression.Factory.fromJsonObject(json)

        assertThat(impression.entity).isNotNull
        assertThat(impression.entity!!.id).isEqualTo("p1")
        assertThat(impression.resolvedBidId).isNull()
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
                "id": "imp-1"
            }
        """.trimIndent())

        val impression = Impression.Factory.fromJsonObject(json)

        assertThat(impression.additionalAttribution).isEqualTo("custom-data")
        assertThat(impression.additionalAttributionEntity).isNull()
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
                "id": "imp-1"
            }
        """.trimIndent())

        val impression = Impression.Factory.fromJsonObject(json)

        assertThat(impression.additionalAttribution).isNull()
        assertThat(impression.additionalAttributionEntity).isNotNull
        assertThat(impression.additionalAttributionEntity!!.id).isEqualTo("attr-entity")
    }

    @Test
    fun `roundtrip promoted impression preserves data`() {
        val original = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-roundtrip",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-rt",
            id = "imp-rt",
            deviceType = "desktop",
            channel = "offsite"
        )

        val json = original.toJsonObject()
        val deserialized = Impression.Factory.fromJsonObject(json)

        assertThat(deserialized.resolvedBidId).isEqualTo(original.resolvedBidId)
        assertThat(deserialized.opaqueUserId).isEqualTo(original.opaqueUserId)
        assertThat(deserialized.id).isEqualTo(original.id)
        assertThat(deserialized.deviceType).isEqualTo(original.deviceType)
        assertThat(deserialized.channel).isEqualTo(original.channel)
    }

    @Test
    fun `roundtrip organic impression preserves data`() {
        val entity = Entity(id = "product-rt", type = EntityType.PRODUCT)
        val original = Impression.Factory.buildOrganic(
            entity = entity,
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-rt",
            id = "imp-rt"
        )

        val json = original.toJsonObject()
        val deserialized = Impression.Factory.fromJsonObject(json)

        assertThat(deserialized.entity).isEqualTo(original.entity)
        assertThat(deserialized.resolvedBidId).isNull()
    }

    @Test
    fun `impressionEvent serialization with multiple impressions`() {
        val impressions = listOf(
            Impression.Factory.buildPromoted(
                resolvedBidId = "bid-1",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-1",
                id = "imp-1"
            ),
            Impression.Factory.buildOrganic(
                entity = Entity(id = "p1", type = EntityType.PRODUCT),
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:31:00Z",
                opaqueUserId = "user-2",
                id = "imp-2"
            )
        )
        val event = ImpressionEvent(impressions)

        val json = event.toJsonObject()

        assertThat(json.getJSONArray("impressions").length()).isEqualTo(2)
    }

    @Test
    fun `impressionEvent roundtrip serialization`() {
        val impressions = listOf(
            Impression.Factory.buildPromoted(
                resolvedBidId = "bid-rt",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-rt",
                id = "imp-rt"
            )
        )
        val original = ImpressionEvent(impressions)

        val jsonString = original.toJsonObject().toString()
        val deserialized = ImpressionEvent.fromJson(jsonString)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.impressions).hasSize(1)
        assertThat(deserialized.impressions[0].resolvedBidId).isEqualTo("bid-rt")
    }

    @Test
    fun `impressionEvent fromJson returns null for null input`() {
        val result = ImpressionEvent.fromJson(null)

        assertThat(result).isNull()
    }

    @Test
    fun `fromJsonArray deserializes list of impressions`() {
        val jsonArray = org.json.JSONArray("""
            [
                {
                    "resolvedBidId": "bid-1",
                    "placement": {"path": "/test1"},
                    "occurredAt": "2024-01-15T10:30:00Z",
                    "opaqueUserId": "user-1",
                    "id": "imp-1"
                },
                {
                    "resolvedBidId": "bid-2",
                    "placement": {"path": "/test2"},
                    "occurredAt": "2024-01-15T10:31:00Z",
                    "opaqueUserId": "user-2",
                    "id": "imp-2"
                }
            ]
        """.trimIndent())

        val impressions = Impression.Factory.fromJsonArray(jsonArray)

        assertThat(impressions).hasSize(2)
        assertThat(impressions[0].id).isEqualTo("imp-1")
        assertThat(impressions[1].id).isEqualTo("imp-2")
    }

    @Test
    fun `impression with page context serializes correctly`() {
        val impression = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "imp-1",
            page = Page.Factory.buildWithId(type = Page.TYPE_PDP, pageId = "product-page-123")
        )

        val json = impression.toJsonObject()
        val pageJson = json.getJSONObject("page")

        assertThat(pageJson.getString("type")).isEqualTo("PDP")
        assertThat(pageJson.getString("pageId")).isEqualTo("product-page-123")
    }

    @Test
    fun `roundtrip with additionalAttributionEntity preserves data`() {
        val attrEntity = Entity(id = "halo-vendor", type = EntityType.VENDOR)
        val original = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "imp-1",
            additionalAttributionEntity = attrEntity
        )

        val json = original.toJsonObject()
        val deserialized = Impression.Factory.fromJsonObject(json)

        assertThat(deserialized.additionalAttributionEntity).isNotNull
        assertThat(deserialized.additionalAttributionEntity!!.id).isEqualTo("halo-vendor")
        assertThat(deserialized.additionalAttributionEntity!!.type).isEqualTo(EntityType.VENDOR)
    }
}
