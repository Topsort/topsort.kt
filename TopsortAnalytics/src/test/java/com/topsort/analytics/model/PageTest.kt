package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

internal class PageTest {

    @Test
    fun `Factory build creates page with type only`() {
        val page = Page.Factory.build(type = "home")

        assertThat(page.type).isEqualTo("home")
        assertThat(page.pageId).isNull()
        assertThat(page.value).isNull()
        assertThat(page.values).isNull()
    }

    @Test
    fun `Factory buildWithId creates page with type and pageId`() {
        val page = Page.Factory.buildWithId(
            type = "PDP",
            pageId = "product-123"
        )

        assertThat(page.type).isEqualTo("PDP")
        assertThat(page.pageId).isEqualTo("product-123")
        assertThat(page.value).isNull()
        assertThat(page.values).isNull()
    }

    @Test
    fun `Factory buildWithId creates page with value`() {
        val page = Page.Factory.buildWithId(
            type = "search",
            pageId = "search-page",
            value = "shoes"
        )

        assertThat(page.type).isEqualTo("search")
        assertThat(page.pageId).isEqualTo("search-page")
        assertThat(page.value).isEqualTo("shoes")
        assertThat(page.values).isNull()
    }

    @Test
    fun `Factory buildWithValues creates page with values list`() {
        val page = Page.Factory.buildWithValues(
            type = "category",
            values = listOf("Electronics", "Phones", "Smartphones")
        )

        assertThat(page.type).isEqualTo("category")
        assertThat(page.pageId).isNull()
        assertThat(page.value).isNull()
        assertThat(page.values).containsExactly("Electronics", "Phones", "Smartphones")
    }

    @Test
    fun `Factory buildWithValues creates page with pageId and values`() {
        val page = Page.Factory.buildWithValues(
            type = "category",
            values = listOf("Men", "Shoes"),
            pageId = "cat-123"
        )

        assertThat(page.type).isEqualTo("category")
        assertThat(page.pageId).isEqualTo("cat-123")
        assertThat(page.values).containsExactly("Men", "Shoes")
    }

    @Test
    fun `toJsonObject serializes type only`() {
        val page = Page.Factory.build(type = "home")
        val json = page.toJsonObject()

        assertThat(json.getString("type")).isEqualTo("home")
        assertThat(json.has("pageId")).isFalse()
        assertThat(json.has("value")).isFalse()
    }

    @Test
    fun `toJsonObject serializes pageId when present`() {
        val page = Page.Factory.buildWithId(type = "PDP", pageId = "p-123")
        val json = page.toJsonObject()

        assertThat(json.getString("type")).isEqualTo("PDP")
        assertThat(json.getString("pageId")).isEqualTo("p-123")
    }

    @Test
    fun `toJsonObject serializes single value as string`() {
        val page = Page.Factory.buildWithId(
            type = "search",
            pageId = "s-1",
            value = "query"
        )
        val json = page.toJsonObject()

        assertThat(json.getString("value")).isEqualTo("query")
    }

    @Test
    fun `toJsonObject serializes values as JSON array`() {
        val page = Page.Factory.buildWithValues(
            type = "category",
            values = listOf("A", "B", "C")
        )
        val json = page.toJsonObject()

        val valueArray = json.getJSONArray("value")
        assertThat(valueArray.length()).isEqualTo(3)
        assertThat(valueArray.getString(0)).isEqualTo("A")
        assertThat(valueArray.getString(1)).isEqualTo("B")
        assertThat(valueArray.getString(2)).isEqualTo("C")
    }

    @Test
    fun `fromJsonObject parses type only`() {
        val json = JSONObject("""{"type": "cart"}""")
        val page = Page.Factory.fromJsonObject(json)

        assertThat(page.type).isEqualTo("cart")
        assertThat(page.pageId).isNull()
        assertThat(page.value).isNull()
        assertThat(page.values).isNull()
    }

    @Test
    fun `fromJsonObject parses pageId`() {
        val json = JSONObject("""{"type": "PDP", "pageId": "prod-456"}""")
        val page = Page.Factory.fromJsonObject(json)

        assertThat(page.type).isEqualTo("PDP")
        assertThat(page.pageId).isEqualTo("prod-456")
    }

    @Test
    fun `fromJsonObject parses string value`() {
        val json = JSONObject("""{"type": "search", "value": "laptop"}""")
        val page = Page.Factory.fromJsonObject(json)

        assertThat(page.type).isEqualTo("search")
        assertThat(page.value).isEqualTo("laptop")
        assertThat(page.values).isNull()
    }

    @Test
    fun `fromJsonObject parses array value as values list`() {
        val json = JSONObject().apply {
            put("type", "category")
            put("value", JSONArray(listOf("Cat1", "Cat2")))
        }
        val page = Page.Factory.fromJsonObject(json)

        assertThat(page.type).isEqualTo("category")
        assertThat(page.value).isNull()
        assertThat(page.values).containsExactly("Cat1", "Cat2")
    }

    @Test
    fun `roundtrip serialization with type only`() {
        val original = Page.Factory.build(type = "home")
        val serialized = original.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `roundtrip serialization with all fields`() {
        val original = Page.Factory.buildWithId(
            type = "search",
            pageId = "search-1",
            value = "sneakers"
        )
        val serialized = original.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `roundtrip serialization with values list`() {
        val original = Page.Factory.buildWithValues(
            type = "category",
            values = listOf("Electronics", "Computers"),
            pageId = "cat-99"
        )
        val serialized = original.toJsonObject().toString()
        val deserialized = Page.Factory.fromJsonObject(JSONObject(serialized))

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `type constants are correct`() {
        assertThat(Page.TYPE_HOME).isEqualTo("home")
        assertThat(Page.TYPE_CATEGORY).isEqualTo("category")
        assertThat(Page.TYPE_PDP).isEqualTo("PDP")
        assertThat(Page.TYPE_SEARCH).isEqualTo("search")
        assertThat(Page.TYPE_CART).isEqualTo("cart")
        assertThat(Page.TYPE_OTHER).isEqualTo("other")
    }
}
