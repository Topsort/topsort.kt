package com.topsort.analytics.model

import com.topsort.analytics.model.auctions.Device
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class RenderEventTest {

    private fun createTestPlacement() = Placement(
        path = "/test/path",
        position = 1,
        page = 1,
        pageSize = 20
    )

    @Test
    fun `build creates render with resolvedBidId`() {
        val render = Render.Factory.build(
            resolvedBidId = "bid-123",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-456",
            id = "render-789"
        )

        assertThat(render.resolvedBidId).isEqualTo("bid-123")
        assertThat(render.opaqueUserId).isEqualTo("user-456")
        assertThat(render.id).isEqualTo("render-789")
    }

    @Test
    fun `toJsonObject contains resolvedBidId`() {
        val render = Render.Factory.build(
            resolvedBidId = "bid-abc",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "render-1"
        )

        val json = render.toJsonObject()

        assertThat(json.getString("resolvedBidId")).isEqualTo("bid-abc")
        assertThat(json.has("entity")).isFalse()
        assertThat(json.has("additionalAttribution")).isFalse()
    }

    @Test
    fun `toJsonObject includes all optional fields when present`() {
        val render = Render.Factory.build(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "render-1",
            deviceType = Device.MOBILE,
            channel = Channel.ONSITE,
            page = Page.Factory.build(type = PageType.CATEGORY)
        )

        val json = render.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("mobile")
        assertThat(json.getString("channel")).isEqualTo("onsite")
        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("category")
    }

    @Test
    fun `fromJsonObject deserializes render`() {
        val json = JSONObject("""
            {
                "resolvedBidId": "bid-test",
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-test",
                "id": "render-test"
            }
        """.trimIndent())

        val render = Render.Factory.fromJsonObject(json)

        assertThat(render.resolvedBidId).isEqualTo("bid-test")
        assertThat(render.id).isEqualTo("render-test")
    }

    @Test
    fun `roundtrip render preserves data`() {
        val original = Render.Factory.build(
            resolvedBidId = "bid-roundtrip",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-rt",
            id = "render-rt",
            deviceType = Device.DESKTOP,
            channel = Channel.OFFSITE
        )

        val json = original.toJsonObject()
        val deserialized = Render.Factory.fromJsonObject(json)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `renderEvent serialization with multiple renders`() {
        val renders = listOf(
            Render.Factory.build(
                resolvedBidId = "bid-1",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-1",
                id = "render-1"
            ),
            Render.Factory.build(
                resolvedBidId = "bid-2",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:31:00Z",
                opaqueUserId = "user-2",
                id = "render-2"
            )
        )
        val event = RenderEvent(renders)

        val json = event.toJsonObject()

        assertThat(json.getJSONArray("renders").length()).isEqualTo(2)
    }

    @Test
    fun `renderEvent roundtrip serialization`() {
        val renders = listOf(
            Render.Factory.build(
                resolvedBidId = "bid-rt",
                placement = createTestPlacement(),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-rt",
                id = "render-rt"
            )
        )
        val original = RenderEvent(renders)

        val jsonString = original.toJsonObject().toString()
        val deserialized = RenderEvent.fromJson(jsonString)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.renders).hasSize(1)
        assertThat(deserialized.renders[0].resolvedBidId).isEqualTo("bid-rt")
    }

    @Test
    fun `renderEvent fromJson returns null for null input`() {
        val result = RenderEvent.fromJson(null)

        assertThat(result).isNull()
    }

    @Test
    fun `fromJsonArray deserializes list of renders`() {
        val jsonArray = org.json.JSONArray("""
            [
                {
                    "resolvedBidId": "bid-1",
                    "placement": {"path": "/test1"},
                    "occurredAt": "2024-01-15T10:30:00Z",
                    "opaqueUserId": "user-1",
                    "id": "render-1"
                },
                {
                    "resolvedBidId": "bid-2",
                    "placement": {"path": "/test2"},
                    "occurredAt": "2024-01-15T10:31:00Z",
                    "opaqueUserId": "user-2",
                    "id": "render-2"
                }
            ]
        """.trimIndent())

        val renders = Render.Factory.fromJsonArray(jsonArray)

        assertThat(renders).hasSize(2)
        assertThat(renders[0].id).isEqualTo("render-1")
        assertThat(renders[1].id).isEqualTo("render-2")
    }

    @Test
    fun `render with page context serializes correctly`() {
        val render = Render.Factory.build(
            resolvedBidId = "bid-1",
            placement = createTestPlacement(),
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "render-1",
            page = Page.Factory.buildWithId(type = PageType.PDP, pageId = "product-page-123")
        )

        val json = render.toJsonObject()
        val pageJson = json.getJSONObject("page")

        assertThat(pageJson.getString("type")).isEqualTo("PDP")
        assertThat(pageJson.getString("pageId")).isEqualTo("product-page-123")
    }
}
