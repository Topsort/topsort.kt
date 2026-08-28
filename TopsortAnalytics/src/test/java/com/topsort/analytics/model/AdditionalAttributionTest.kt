package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

/**
 * `additionalAttribution` carries an entity, not an id.
 *
 * The v2 spec types it as `EventEntity` on both Click and Impression - an object with a required
 * `id` and a `type` of `product` or `vendor`, and `additionalProperties: false`. This SDK sent a
 * bare string there from the beginning, which cannot validate against that schema, so the field
 * has never worked. These tests pin the shape that goes on the wire.
 */
class AdditionalAttributionTest {

    private val entity = Entity(id = "attr-entity", type = EntityType.VENDOR)

    @Test
    fun `an impression serialises additionalAttribution as an object`() {
        val json = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = Placement(path = "/search"),
            opaqueUserId = "user-1",
            id = "imp-1",
            occurredAt = "2024-01-15T10:30:00Z",
            additionalAttribution = entity,
        ).toJsonObject()

        val attribution = json.getJSONObject("additionalAttribution")
        assertThat(attribution.getString("id")).isEqualTo("attr-entity")
        assertThat(attribution.getString("type")).isEqualTo("vendor")
    }

    @Test
    fun `a click serialises additionalAttribution as an object`() {
        val json = Click.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = Placement(path = "/search"),
            opaqueUserId = "user-1",
            id = "click-1",
            occurredAt = "2024-01-15T10:30:00Z",
            additionalAttribution = entity,
        ).toJsonObject()

        val attribution = json.getJSONObject("additionalAttribution")
        assertThat(attribution.getString("id")).isEqualTo("attr-entity")
        assertThat(attribution.getString("type")).isEqualTo("vendor")
    }

    @Test
    fun `an impression reads additionalAttribution back as an entity`() {
        val json = JSONObject(
            """
            {
                "resolvedBidId": "bid-1",
                "additionalAttribution": {"id": "attr-entity", "type": "product"},
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "imp-1"
            }
            """.trimIndent(),
        )

        val impression = Impression.Factory.fromJsonObject(json)

        assertThat(impression.additionalAttribution).isNotNull
        assertThat(impression.additionalAttribution!!.id).isEqualTo("attr-entity")
        assertThat(impression.additionalAttribution!!.type).isEqualTo(EntityType.PRODUCT)
    }

    @Test
    fun `a click reads additionalAttribution back as an entity`() {
        val json = JSONObject(
            """
            {
                "resolvedBidId": "bid-1",
                "additionalAttribution": {"id": "attr-entity", "type": "product"},
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "click-1"
            }
            """.trimIndent(),
        )

        val click = Click.Factory.fromJsonObject(json)

        assertThat(click.additionalAttribution).isNotNull
        assertThat(click.additionalAttribution!!.id).isEqualTo("attr-entity")
        assertThat(click.additionalAttribution!!.type).isEqualTo(EntityType.PRODUCT)
    }

    /** Absent is absent - the key must not appear at all rather than as null. */
    @Test
    fun `additionalAttribution is omitted when there is none`() {
        val json = Impression.Factory.buildPromoted(
            resolvedBidId = "bid-1",
            placement = Placement(path = "/search"),
            opaqueUserId = "user-1",
            id = "imp-1",
            occurredAt = "2024-01-15T10:30:00Z",
        ).toJsonObject()

        assertThat(json.has("additionalAttribution")).isFalse()
    }

    /** A string where an object belongs is what the SDK used to send; it must not parse. */
    @Test
    fun `a bare string is not read as an entity`() {
        val json = JSONObject(
            """
            {
                "resolvedBidId": "bid-1",
                "additionalAttribution": "attr-entity",
                "placement": {"path": "/search"},
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-1",
                "id": "imp-1"
            }
            """.trimIndent(),
        )

        assertThat(Impression.Factory.fromJsonObject(json).additionalAttribution).isNull()
    }
}
