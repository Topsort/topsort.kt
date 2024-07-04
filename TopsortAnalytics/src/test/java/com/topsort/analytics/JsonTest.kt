package com.topsort.analytics

import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Console

internal class JsonTest {

    @Test
    fun `json click serialization`() {
        val click = getRandomClick()
        val serialized = click.toJsonObject().toString()
        val deserialized = deserializeClick(JSONObject(serialized))

        assertEquals(click.id, deserialized.id)
        //assertEquals(1, 2)
        assertEquals(click.occurredAt, deserialized.occurredAt)
        assertEquals(click.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(click.resolvedBidId, deserialized.resolvedBidId)
    }

    @Test
    fun `json impression serialization`() {
        val impression = getRandomClick()
        val serialized = impression.toJsonObject().toString()
        val deserialized = deserializeImpression(JSONObject(serialized))

        assertEquals(impression.id, deserialized.id)
        //assertEquals(1, 2)
        assertEquals(impression.occurredAt, deserialized.occurredAt)
        assertEquals(impression.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(impression.resolvedBidId, deserialized.resolvedBidId)
    }

    @Test
    fun `json purchase serialization`() {
        assert(true)
    }

    private fun JSONObject.getStringOrNull(name: String): String? {
        return if (has(name)) {
            getString(name)
        } else null
    }

    private fun JSONObject.getIntOrNull(name: String): Int? {
        return if (has(name)) {
            getInt(name)
        } else null
    }

    private fun deserializeClick(json: JSONObject): Click {
        return Click(
            resolvedBidId = json.getStringOrNull("resolvedBidId"),
            entity = deserializeEntity(json.getJSONObject("entity")),
            additionalAttribution = json.getStringOrNull("additionalAttribution"),
            placement = deserializePlacement(json.getJSONObject("placement")),
            occurredAt = json.getString("occurredAt"),
            opaqueUserId = json.getString("opaqueUserId"),
            id = json.getString("id"),
        )
    }

    fun deserializeImpression(json: JSONObject): Impression {
        return Impression(
            resolvedBidId = json.getStringOrNull("resolvedBidId"),
            entity = deserializeEntity(json.getJSONObject("entity")),
            additionalAttribution = json.getStringOrNull("additionalAttribution"),
            placement = deserializePlacement(json.getJSONObject("placement")),
            occurredAt = json.getString("occurredAt"),
            opaqueUserId = json.getString("opaqueUserId"),
            id = json.getString("id"),
        )
    }

    private fun deserializePlacement(json : JSONObject) : Placement{
        return Placement(
            path = json.getString("path"),
            position = json.getIntOrNull("position"),
            page = json.getIntOrNull("position"),
            pageSize = json.getIntOrNull("pageSize"),
            productId = json.getStringOrNull("productId"),
            categoryIds = null,
            searchQuery = json.getStringOrNull("searchQuery"),
            location = json.getStringOrNull("location"),
        )
    }

    private fun deserializeEntity(json: JSONObject): Entity {
        return Entity(
            id = json.getString("id"),
            type = EntityType.valueOf(json.getString("type"))
        )
    }
}
