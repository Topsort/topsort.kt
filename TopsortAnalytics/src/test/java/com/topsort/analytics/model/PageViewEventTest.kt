package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

internal class PageViewEventTest {

    @Test
    fun `PageView Factory build creates basic page view`() {
        val page = Page.Factory.build(type = "home")
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "pv-456"
        )

        assertThat(pageView.page).isEqualTo(page)
        assertThat(pageView.occurredAt).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(pageView.opaqueUserId).isEqualTo("user-123")
        assertThat(pageView.id).isEqualTo("pv-456")
        assertThat(pageView.deviceType).isNull()
        assertThat(pageView.channel).isNull()
    }

    @Test
    fun `PageView Factory build creates page view with optional fields`() {
        val page = Page.Factory.buildWithId(type = "PDP", pageId = "prod-1")
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "pv-789",
            deviceType = "mobile",
            channel = "onsite"
        )

        assertThat(pageView.deviceType).isEqualTo("mobile")
        assertThat(pageView.channel).isEqualTo("onsite")
    }

    @Test
    fun `PageView toJsonObject serializes required fields`() {
        val page = Page.Factory.build(type = "cart")
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-abc",
            id = "pv-def"
        )

        val json = pageView.toJsonObject()

        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("cart")
        assertThat(json.getString("occurredAt")).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(json.getString("opaqueUserId")).isEqualTo("user-abc")
        assertThat(json.getString("id")).isEqualTo("pv-def")
        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
    }

    @Test
    fun `PageView toJsonObject serializes optional fields when present`() {
        val page = Page.Factory.build(type = "home")
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "pv-123",
            deviceType = "desktop",
            channel = "offsite"
        )

        val json = pageView.toJsonObject()

        assertThat(json.getString("deviceType")).isEqualTo("desktop")
        assertThat(json.getString("channel")).isEqualTo("offsite")
    }

    @Test
    fun `PageView fromJsonObject parses required fields`() {
        val json = JSONObject("""
            {
                "page": {"type": "search"},
                "occurredAt": "2024-02-20T15:45:00Z",
                "opaqueUserId": "user-xyz",
                "id": "pv-abc"
            }
        """.trimIndent())

        val pageView = PageView.Factory.fromJsonObject(json)

        assertThat(pageView.page.type).isEqualTo("search")
        assertThat(pageView.occurredAt).isEqualTo("2024-02-20T15:45:00Z")
        assertThat(pageView.opaqueUserId).isEqualTo("user-xyz")
        assertThat(pageView.id).isEqualTo("pv-abc")
        assertThat(pageView.deviceType).isNull()
        assertThat(pageView.channel).isNull()
    }

    @Test
    fun `PageView fromJsonObject parses optional fields`() {
        val json = JSONObject("""
            {
                "page": {"type": "PDP", "pageId": "prod-99"},
                "occurredAt": "2024-02-20T15:45:00Z",
                "opaqueUserId": "user-xyz",
                "id": "pv-abc",
                "deviceType": "mobile",
                "channel": "instore"
            }
        """.trimIndent())

        val pageView = PageView.Factory.fromJsonObject(json)

        assertThat(pageView.page.pageId).isEqualTo("prod-99")
        assertThat(pageView.deviceType).isEqualTo("mobile")
        assertThat(pageView.channel).isEqualTo("instore")
    }

    @Test
    fun `PageView roundtrip serialization`() {
        val page = Page.Factory.buildWithId(type = "category", pageId = "cat-1")
        val original = PageView.Factory.build(
            page = page,
            occurredAt = "2024-03-01T12:00:00Z",
            opaqueUserId = "user-roundtrip",
            id = "pv-roundtrip",
            deviceType = "mobile",
            channel = "onsite"
        )

        val serialized = original.toJsonObject().toString()
        val deserialized = PageView.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `PageView fromJsonArray parses multiple page views`() {
        val jsonArray = JSONArray().apply {
            put(JSONObject("""
                {
                    "page": {"type": "home"},
                    "occurredAt": "2024-01-01T00:00:00Z",
                    "opaqueUserId": "user-1",
                    "id": "pv-1"
                }
            """.trimIndent()))
            put(JSONObject("""
                {
                    "page": {"type": "cart"},
                    "occurredAt": "2024-01-01T00:01:00Z",
                    "opaqueUserId": "user-1",
                    "id": "pv-2"
                }
            """.trimIndent()))
        }

        val pageViews = PageView.Factory.fromJsonArray(jsonArray)

        assertThat(pageViews).hasSize(2)
        assertThat(pageViews[0].page.type).isEqualTo("home")
        assertThat(pageViews[1].page.type).isEqualTo("cart")
    }

    @Test
    fun `PageViewEvent toJsonObject serializes pageviews array`() {
        val pageView1 = PageView.Factory.build(
            page = Page.Factory.build(type = "home"),
            occurredAt = "2024-01-01T00:00:00Z",
            opaqueUserId = "user-1",
            id = "pv-1"
        )
        val pageView2 = PageView.Factory.build(
            page = Page.Factory.build(type = "PDP"),
            occurredAt = "2024-01-01T00:05:00Z",
            opaqueUserId = "user-1",
            id = "pv-2"
        )

        val event = PageViewEvent(pageviews = listOf(pageView1, pageView2))
        val json = event.toJsonObject()

        val pageviewsArray = json.getJSONArray("pageviews")
        assertThat(pageviewsArray.length()).isEqualTo(2)
        assertThat(pageviewsArray.getJSONObject(0).getString("id")).isEqualTo("pv-1")
        assertThat(pageviewsArray.getJSONObject(1).getString("id")).isEqualTo("pv-2")
    }

    @Test
    fun `PageViewEvent fromJson returns null for null input`() {
        val result = PageViewEvent.fromJson(null)

        assertThat(result).isNull()
    }

    @Test
    fun `PageViewEvent fromJson parses valid JSON`() {
        val json = """
            {
                "pageviews": [
                    {
                        "page": {"type": "search", "value": "shoes"},
                        "occurredAt": "2024-05-10T08:00:00Z",
                        "opaqueUserId": "user-search",
                        "id": "pv-search"
                    }
                ]
            }
        """.trimIndent()

        val event = PageViewEvent.fromJson(json)

        assertThat(event).isNotNull
        assertThat(event!!.pageviews).hasSize(1)
        assertThat(event.pageviews[0].page.type).isEqualTo("search")
        assertThat(event.pageviews[0].page.value).isEqualTo("shoes")
    }

    @Test
    fun `PageViewEvent roundtrip serialization`() {
        val pageView = PageView.Factory.build(
            page = Page.Factory.buildWithValues(
                type = "category",
                values = listOf("Electronics", "Phones")
            ),
            occurredAt = "2024-06-15T14:30:00Z",
            opaqueUserId = "user-rt",
            id = "pv-rt",
            deviceType = "desktop",
            channel = "onsite"
        )

        val original = PageViewEvent(pageviews = listOf(pageView))
        val serialized = original.toJsonObject().toString()
        val deserialized = PageViewEvent.fromJson(serialized)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.pageviews).hasSize(1)
        assertThat(deserialized.pageviews[0]).isEqualTo(pageView)
    }

    @Test
    fun `PageViewEvent with empty pageviews list`() {
        val event = PageViewEvent(pageviews = emptyList())
        val json = event.toJsonObject()

        val pageviewsArray = json.getJSONArray("pageviews")
        assertThat(pageviewsArray.length()).isEqualTo(0)
    }
}
