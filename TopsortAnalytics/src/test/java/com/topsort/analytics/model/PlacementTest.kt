package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class PlacementTest {

    @Test
    fun `toJsonObject serializes minimal placement`() {
        val placement = Placement(path = "/search/results")

        val json = placement.toJsonObject()

        assertThat(json.getString("path")).isEqualTo("/search/results")
        assertThat(json.isNull("position")).isTrue()
        assertThat(json.isNull("page")).isTrue()
        assertThat(json.isNull("pageSize")).isTrue()
    }

    @Test
    fun `toJsonObject serializes all fields`() {
        val placement = Placement(
            path = "/category/electronics",
            position = 5,
            page = 2,
            pageSize = 20,
            productId = "prod-123",
            categoryIds = listOf("cat1", "cat2", "cat3"),
            searchQuery = "laptop",
            location = "top-banner"
        )

        val json = placement.toJsonObject()

        assertThat(json.getString("path")).isEqualTo("/category/electronics")
        assertThat(json.getInt("position")).isEqualTo(5)
        assertThat(json.getInt("page")).isEqualTo(2)
        assertThat(json.getInt("pageSize")).isEqualTo(20)
        assertThat(json.getString("productId")).isEqualTo("prod-123")
        assertThat(json.getJSONArray("categoryIds").length()).isEqualTo(3)
        assertThat(json.getJSONArray("categoryIds").getString(0)).isEqualTo("cat1")
        assertThat(json.getString("searchQuery")).isEqualTo("laptop")
        assertThat(json.getString("location")).isEqualTo("top-banner")
    }

    @Test
    fun `fromJsonObject deserializes minimal placement`() {
        val json = JSONObject("""
            {
                "path": "/home"
            }
        """.trimIndent())

        val placement = Placement.fromJsonObject(json)

        assertThat(placement.path).isEqualTo("/home")
        assertThat(placement.position).isNull()
        assertThat(placement.page).isNull()
        assertThat(placement.pageSize).isNull()
        assertThat(placement.productId).isNull()
        assertThat(placement.categoryIds).isNull()
        assertThat(placement.searchQuery).isNull()
        assertThat(placement.location).isNull()
    }

    @Test
    fun `fromJsonObject deserializes all fields`() {
        val json = JSONObject("""
            {
                "path": "/search",
                "position": 10,
                "page": 3,
                "pageSize": 50,
                "productId": "p123",
                "categoryIds": ["electronics", "phones"],
                "searchQuery": "smartphone",
                "location": "sidebar"
            }
        """.trimIndent())

        val placement = Placement.fromJsonObject(json)

        assertThat(placement.path).isEqualTo("/search")
        assertThat(placement.position).isEqualTo(10)
        assertThat(placement.page).isEqualTo(3)
        assertThat(placement.pageSize).isEqualTo(50)
        assertThat(placement.productId).isEqualTo("p123")
        assertThat(placement.categoryIds).containsExactly("electronics", "phones")
        assertThat(placement.searchQuery).isEqualTo("smartphone")
        assertThat(placement.location).isEqualTo("sidebar")
    }

    @Test
    fun `roundtrip serialization preserves all data`() {
        val original = Placement(
            path = "/products/view",
            position = 1,
            page = 1,
            pageSize = 10,
            productId = "product-abc",
            categoryIds = listOf("cat-a", "cat-b"),
            searchQuery = "test query",
            location = "main-content"
        )

        val json = original.toJsonObject()
        val deserialized = Placement.fromJsonObject(json)

        assertThat(deserialized.path).isEqualTo(original.path)
        assertThat(deserialized.position).isEqualTo(original.position)
        assertThat(deserialized.page).isEqualTo(original.page)
        assertThat(deserialized.pageSize).isEqualTo(original.pageSize)
        assertThat(deserialized.productId).isEqualTo(original.productId)
        assertThat(deserialized.categoryIds).isEqualTo(original.categoryIds)
        assertThat(deserialized.searchQuery).isEqualTo(original.searchQuery)
        assertThat(deserialized.location).isEqualTo(original.location)
    }

    @Test
    fun `roundtrip with minimal fields`() {
        val original = Placement(path = "/minimal")

        val json = original.toJsonObject()
        val deserialized = Placement.fromJsonObject(json)

        assertThat(deserialized.path).isEqualTo(original.path)
        assertThat(deserialized.position).isNull()
        assertThat(deserialized.categoryIds).isNull()
    }

    @Test
    fun `build factory method creates placement`() {
        val placement = Placement.build(
            path = "/factory/test",
            position = 3,
            page = 1
        )

        assertThat(placement.path).isEqualTo("/factory/test")
        assertThat(placement.position).isEqualTo(3)
        assertThat(placement.page).isEqualTo(1)
    }

    @Test
    fun `build factory with all parameters`() {
        val placement = Placement.build(
            path = "/full",
            position = 1,
            page = 2,
            pageSize = 25,
            productId = "p1",
            categoryIds = listOf("c1"),
            searchQuery = "query",
            location = "loc"
        )

        assertThat(placement.path).isEqualTo("/full")
        assertThat(placement.position).isEqualTo(1)
        assertThat(placement.page).isEqualTo(2)
        assertThat(placement.pageSize).isEqualTo(25)
        assertThat(placement.productId).isEqualTo("p1")
        assertThat(placement.categoryIds).containsExactly("c1")
        assertThat(placement.searchQuery).isEqualTo("query")
        assertThat(placement.location).isEqualTo("loc")
    }

    @Test
    fun `placement with empty categoryIds list`() {
        val placement = Placement(
            path = "/test",
            categoryIds = emptyList()
        )

        val json = placement.toJsonObject()

        assertThat(json.getJSONArray("categoryIds").length()).isEqualTo(0)
    }

    @Test
    fun `fromJsonObject with empty categoryIds array`() {
        val json = JSONObject("""
            {
                "path": "/test",
                "categoryIds": []
            }
        """.trimIndent())

        val placement = Placement.fromJsonObject(json)

        assertThat(placement.categoryIds).isEmpty()
    }

    @Test
    fun `placement with zero values for integers`() {
        val placement = Placement(
            path = "/test",
            position = 0,
            page = 0,
            pageSize = 0
        )

        val json = placement.toJsonObject()
        val deserialized = Placement.fromJsonObject(json)

        assertThat(deserialized.position).isEqualTo(0)
        assertThat(deserialized.page).isEqualTo(0)
        assertThat(deserialized.pageSize).isEqualTo(0)
    }

    @Test
    fun `placement with special characters in strings`() {
        val placement = Placement(
            path = "/search?q=test&page=1",
            searchQuery = "laptop \"gaming\" 15-inch",
            location = "section/subsection"
        )

        val json = placement.toJsonObject()
        val deserialized = Placement.fromJsonObject(json)

        assertThat(deserialized.path).isEqualTo("/search?q=test&page=1")
        assertThat(deserialized.searchQuery).isEqualTo("laptop \"gaming\" 15-inch")
        assertThat(deserialized.location).isEqualTo("section/subsection")
    }

    @Test
    fun `placement data class equality`() {
        val placement1 = Placement(path = "/test", position = 1)
        val placement2 = Placement(path = "/test", position = 1)
        val placement3 = Placement(path = "/test", position = 2)

        assertThat(placement1).isEqualTo(placement2)
        assertThat(placement1).isNotEqualTo(placement3)
    }
}
