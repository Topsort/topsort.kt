package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
import org.junit.Test

internal class EntityTest {

    @Test
    fun `toJsonObject serializes product entity`() {
        val entity = Entity(
            id = "product-123",
            type = EntityType.PRODUCT
        )

        val json = entity.toJsonObject()

        assertThat(json.getString("id")).isEqualTo("product-123")
        assertThat(json.getString("type")).isEqualTo("product")
    }

    @Test
    fun `toJsonObject serializes vendor entity`() {
        val entity = Entity(
            id = "vendor-456",
            type = EntityType.VENDOR
        )

        val json = entity.toJsonObject()

        assertThat(json.getString("id")).isEqualTo("vendor-456")
        assertThat(json.getString("type")).isEqualTo("vendor")
    }

    @Test
    fun `fromJsonObject deserializes product entity`() {
        val json = JSONObject("""
            {
                "id": "product-789",
                "type": "product"
            }
        """.trimIndent())

        val entity = Entity.fromJsonObject(json)

        assertThat(entity.id).isEqualTo("product-789")
        assertThat(entity.type).isEqualTo(EntityType.PRODUCT)
    }

    @Test
    fun `fromJsonObject deserializes vendor entity`() {
        val json = JSONObject("""
            {
                "id": "vendor-abc",
                "type": "vendor"
            }
        """.trimIndent())

        val entity = Entity.fromJsonObject(json)

        assertThat(entity.id).isEqualTo("vendor-abc")
        assertThat(entity.type).isEqualTo(EntityType.VENDOR)
    }

    @Test
    fun `fromJsonObject handles uppercase type`() {
        val json = JSONObject("""
            {
                "id": "p1",
                "type": "PRODUCT"
            }
        """.trimIndent())

        val entity = Entity.fromJsonObject(json)

        assertThat(entity.type).isEqualTo(EntityType.PRODUCT)
    }

    @Test
    fun `fromJsonObject handles mixed case type`() {
        val json = JSONObject("""
            {
                "id": "v1",
                "type": "Vendor"
            }
        """.trimIndent())

        val entity = Entity.fromJsonObject(json)

        assertThat(entity.type).isEqualTo(EntityType.VENDOR)
    }

    @Test
    fun `roundtrip serialization preserves data`() {
        val original = Entity(
            id = "test-entity-id",
            type = EntityType.PRODUCT
        )

        val json = original.toJsonObject()
        val deserialized = Entity.fromJsonObject(json)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `roundtrip with vendor type preserves data`() {
        val original = Entity(
            id = "vendor-test",
            type = EntityType.VENDOR
        )

        val json = original.toJsonObject()
        val deserialized = Entity.fromJsonObject(json)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `fromJsonObject throws for missing id`() {
        val json = JSONObject("""
            {
                "type": "product"
            }
        """.trimIndent())

        assertThatThrownBy {
            Entity.fromJsonObject(json)
        }.isInstanceOf(Exception::class.java)
    }

    @Test
    fun `fromJsonObject throws for missing type`() {
        val json = JSONObject("""
            {
                "id": "p1"
            }
        """.trimIndent())

        assertThatThrownBy {
            Entity.fromJsonObject(json)
        }.isInstanceOf(Exception::class.java)
    }

    @Test
    fun `fromJsonObject throws for invalid type`() {
        val json = JSONObject("""
            {
                "id": "p1",
                "type": "invalid_type"
            }
        """.trimIndent())

        assertThatThrownBy {
            Entity.fromJsonObject(json)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `entity equality works correctly`() {
        val entity1 = Entity(id = "p1", type = EntityType.PRODUCT)
        val entity2 = Entity(id = "p1", type = EntityType.PRODUCT)
        val entity3 = Entity(id = "p1", type = EntityType.VENDOR)

        assertThat(entity1).isEqualTo(entity2)
        assertThat(entity1).isNotEqualTo(entity3)
    }

    @Test
    fun `entityType enum values`() {
        assertThat(EntityType.values()).containsExactly(EntityType.PRODUCT, EntityType.VENDOR)
    }
}
