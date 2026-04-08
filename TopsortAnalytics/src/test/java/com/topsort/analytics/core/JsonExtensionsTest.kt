package com.topsort.analytics.core

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class JsonExtensionsTest {

    @Test
    fun `getStringOrNull returns string when present`() {
        val json = JSONObject("""{"name": "test"}""")

        assertThat(json.getStringOrNull("name")).isEqualTo("test")
    }

    @Test
    fun `getStringOrNull returns null when key missing`() {
        val json = JSONObject("""{"other": "value"}""")

        assertThat(json.getStringOrNull("name")).isNull()
    }

    @Test
    fun `getStringOrNull returns null when value is explicit JSON null`() {
        val json = JSONObject("""{"name": null}""")

        assertThat(json.getStringOrNull("name")).isNull()
    }

    @Test
    fun `getIntOrNull returns int when present`() {
        val json = JSONObject("""{"count": 42}""")

        assertThat(json.getIntOrNull("count")).isEqualTo(42)
    }

    @Test
    fun `getIntOrNull returns null when key missing`() {
        val json = JSONObject("""{"other": 1}""")

        assertThat(json.getIntOrNull("count")).isNull()
    }

    @Test
    fun `getIntOrNull returns null when value is explicit JSON null`() {
        val json = JSONObject("""{"count": null}""")

        assertThat(json.getIntOrNull("count")).isNull()
    }

    @Test
    fun `getStringListOrNull returns list when present`() {
        val json = JSONObject("""{"items": ["a", "b", "c"]}""")

        assertThat(json.getStringListOrNull("items")).containsExactly("a", "b", "c")
    }

    @Test
    fun `getStringListOrNull returns null when key missing`() {
        val json = JSONObject("""{"other": []}""")

        assertThat(json.getStringListOrNull("items")).isNull()
    }

    @Test
    fun `getStringListOrNull returns null when value is explicit JSON null`() {
        val json = JSONObject("""{"items": null}""")

        assertThat(json.getStringListOrNull("items")).isNull()
    }

    @Test
    fun `getStringListOrNull returns empty list for empty array`() {
        val json = JSONObject("""{"items": []}""")

        assertThat(json.getStringListOrNull("items")).isEmpty()
    }
}
