package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
import org.junit.Test

internal class PageTest {

    @Test
    fun `page build with type only`() {
        val page = Page.Factory.build(type = Page.TYPE_HOME)

        assertThat(page.type).isEqualTo("home")
        assertThat(page.pageId).isNull()
        assertThat(page.value).isNull()
        assertThat(page.values).isNull()
    }

    @Test
    fun `page buildWithId with value`() {
        val page = Page.Factory.buildWithId(
            type = Page.TYPE_PDP,
            pageId = "product-123",
            value = "SKU-456"
        )

        assertThat(page.type).isEqualTo("PDP")
        assertThat(page.pageId).isEqualTo("product-123")
        assertThat(page.value).isEqualTo("SKU-456")
        assertThat(page.values).isNull()
    }

    @Test
    fun `page buildWithValues with category hierarchy`() {
        val page = Page.Factory.buildWithValues(
            type = Page.TYPE_CATEGORY,
            values = listOf("Electronics", "Phones", "Smartphones"),
            pageId = "cat-123"
        )

        assertThat(page.type).isEqualTo("category")
        assertThat(page.pageId).isEqualTo("cat-123")
        assertThat(page.value).isNull()
        assertThat(page.values).containsExactly("Electronics", "Phones", "Smartphones")
    }

    @Test
    fun `page toJsonObject with single value`() {
        val page = Page.Factory.buildWithId(
            type = Page.TYPE_SEARCH,
            pageId = "search-page",
            value = "running shoes"
        )

        val json = page.toJsonObject()

        assertThat(json.getString("type")).isEqualTo("search")
        assertThat(json.getString("pageId")).isEqualTo("search-page")
        assertThat(json.getString("value")).isEqualTo("running shoes")
    }

    @Test
    fun `page toJsonObject with array values`() {
        val page = Page.Factory.buildWithValues(
            type = Page.TYPE_CATEGORY,
            values = listOf("cat1", "cat2")
        )

        val json = page.toJsonObject()

        assertThat(json.getString("type")).isEqualTo("category")
        val valueArray = json.getJSONArray("value")
        assertThat(valueArray.length()).isEqualTo(2)
        assertThat(valueArray.getString(0)).isEqualTo("cat1")
        assertThat(valueArray.getString(1)).isEqualTo("cat2")
    }

    @Test
    fun `page roundtrip with single value`() {
        val page = Page.Factory.buildWithId(
            type = Page.TYPE_CART,
            pageId = "cart-1",
            value = "checkout"
        )

        val serialized = page.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(page)
    }

    @Test
    fun `page roundtrip with array values`() {
        val page = Page.Factory.buildWithValues(
            type = Page.TYPE_CATEGORY,
            values = listOf("a", "b", "c"),
            pageId = "test-id"
        )

        val serialized = page.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(page)
    }

    @Test
    fun `page roundtrip with minimal fields`() {
        val page = Page.Factory.build(type = Page.TYPE_OTHER)

        val serialized = page.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(page)
    }

    @Test
    fun `page copy with both value and values throws exception`() {
        val page = Page.Factory.buildWithId(
            type = Page.TYPE_SEARCH,
            pageId = "search-1",
            value = "query"
        )

        assertThatThrownBy {
            page.copy(values = listOf("a", "b"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("mutually exclusive")
    }

    @Test
    fun `page copy replacing value with values is valid`() {
        val page = Page.Factory.buildWithId(
            type = Page.TYPE_SEARCH,
            pageId = "search-1",
            value = "query"
        )

        val updated = page.copy(value = null, values = listOf("a", "b"))

        assertThat(updated.value).isNull()
        assertThat(updated.values).containsExactly("a", "b")
    }
}
