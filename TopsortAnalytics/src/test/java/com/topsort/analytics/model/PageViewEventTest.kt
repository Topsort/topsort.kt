package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class PageViewEventTest {

    @Test
    fun `pageview factory build with all fields`() {
        val page = Page.Factory.build(type = Page.TYPE_HOME)
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "event-456",
            deviceType = "mobile",
            channel = "onsite"
        )

        assertThat(pageView.page).isEqualTo(page)
        assertThat(pageView.occurredAt).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(pageView.opaqueUserId).isEqualTo("user-123")
        assertThat(pageView.id).isEqualTo("event-456")
        assertThat(pageView.deviceType).isEqualTo("mobile")
        assertThat(pageView.channel).isEqualTo("onsite")
    }

    @Test
    fun `pageview factory build with minimal fields`() {
        val page = Page.Factory.build(type = Page.TYPE_CART)
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "event-456"
        )

        assertThat(pageView.page).isEqualTo(page)
        assertThat(pageView.deviceType).isNull()
        assertThat(pageView.channel).isNull()
    }

    @Test
    fun `pageview toJsonObject with all fields`() {
        val page = Page.Factory.buildWithId(type = Page.TYPE_PDP, pageId = "prod-1")
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "event-456",
            deviceType = "desktop",
            channel = "offsite"
        )

        val json = pageView.toJsonObject()

        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("PDP")
        assertThat(json.getString("occurredAt")).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(json.getString("opaqueUserId")).isEqualTo("user-123")
        assertThat(json.getString("id")).isEqualTo("event-456")
        assertThat(json.getString("deviceType")).isEqualTo("desktop")
        assertThat(json.getString("channel")).isEqualTo("offsite")
    }

    @Test
    fun `pageview roundtrip serialization`() {
        val page = Page.Factory.buildWithValues(
            type = Page.TYPE_CATEGORY,
            values = listOf("Electronics", "Phones")
        )
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "event-456",
            deviceType = "mobile",
            channel = "instore"
        )

        val serialized = pageView.toJsonObject().toString()
        val deserialized = PageView.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(pageView)
    }

    @Test
    fun `pageview roundtrip with minimal fields`() {
        val page = Page.Factory.build(type = Page.TYPE_HOME)
        val pageView = PageView.Factory.build(
            page = page,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "event-456"
        )

        val serialized = pageView.toJsonObject().toString()
        val deserialized = PageView.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(pageView)
    }

    @Test
    fun `pageViewEvent toJsonObject creates pageviews array`() {
        val page1 = Page.Factory.build(type = Page.TYPE_HOME)
        val page2 = Page.Factory.build(type = Page.TYPE_CART)

        val pageViews = listOf(
            PageView.Factory.build(
                page = page1,
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-123",
                id = "event-1"
            ),
            PageView.Factory.build(
                page = page2,
                occurredAt = "2024-01-15T10:31:00Z",
                opaqueUserId = "user-123",
                id = "event-2"
            )
        )

        val event = PageViewEvent(pageviews = pageViews)
        val json = event.toJsonObject()

        val array = json.getJSONArray("pageviews")
        assertThat(array.length()).isEqualTo(2)
        assertThat(array.getJSONObject(0).getJSONObject("page").getString("type")).isEqualTo("home")
        assertThat(array.getJSONObject(1).getJSONObject("page").getString("type")).isEqualTo("cart")
    }

    @Test
    fun `pageViewEvent roundtrip serialization`() {
        val pageViews = listOf(
            PageView.Factory.build(
                page = Page.Factory.build(type = Page.TYPE_SEARCH),
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-123",
                id = "event-1",
                deviceType = "mobile"
            )
        )

        val event = PageViewEvent(pageviews = pageViews)
        val serialized = event.toJsonObject().toString()
        val deserialized = PageViewEvent.fromJson(serialized)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.pageviews).hasSize(1)
        assertThat(deserialized.pageviews[0]).isEqualTo(pageViews[0])
    }

    @Test
    fun `pageViewEvent fromJson returns null for null input`() {
        val result = PageViewEvent.fromJson(null)
        assertThat(result).isNull()
    }
}
