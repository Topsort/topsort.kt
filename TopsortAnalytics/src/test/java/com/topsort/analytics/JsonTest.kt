package com.topsort.analytics

import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchasedItem
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.assertj.core.api.Assertions.*

internal class JsonTest {

    @Test
    fun `json click serialization`() {
        val click = getRandomClick()
        val serialized = click.toJsonObject().toString()
        val deserialized = deserializeClick(JSONObject(serialized))

        assertEquals(click.id, deserialized.id)
        assertPlacementEquals(click.placement, deserialized.placement)
        assertEntityEquals(click.entity, deserialized.entity)
        assertEquals(click.occurredAt, deserialized.occurredAt)
        assertEquals(click.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(click.resolvedBidId, deserialized.resolvedBidId)
        assertEquals(click.additionalAttribution, deserialized.additionalAttribution)
    }

    @Test
    fun `json impression serialization`() {
        val impression = getRandomImpression()
        val serialized = impression.toJsonObject().toString()
        val deserialized = deserializeImpression(JSONObject(serialized))

        assertEquals(impression.id, deserialized.id)
        assertPlacementEquals(impression.placement, deserialized.placement)
        assertEntityEquals(impression.entity, deserialized.entity)
        assertEquals(impression.occurredAt, deserialized.occurredAt)
        assertEquals(impression.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(impression.resolvedBidId, deserialized.resolvedBidId)
    }

    @Test
    fun `json purchase serialization`() {
        val purchase = getRandomPurchase()
        val serialized = purchase.toJsonObject().toString()
        val deserialized = deserializePurchase(JSONObject(serialized))

        assertEquals(purchase.id, deserialized.id)
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

    private fun deserializeImpression(json: JSONObject): Impression {
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

    private fun deserializePurchase(json : JSONObject) : Purchase {
        return Purchase(
            occurredAt = json.getString("occurredAt"),
            opaqueUserId = json.getString("opaqueUserId"),
            id = json.getString("id"),
            items = listOf(),
        )
    }

    private fun deserializePurchasedItem(json: JSONObject): PurchasedItem {
        return PurchasedItem(
            productId = json.getString("productId"),
            quantity = json.getInt("quantity"),
            unitPrice = json.getIntOrNull("unitPrice"),
            resolvedBidId = json.getStringOrNull("resolvedBidId"),
        )
    }

    private fun deserializePlacement(json : JSONObject) : Placement{
        return Placement(
            path = json.getString("path"),
            position = json.getIntOrNull("position"),
            page = json.getIntOrNull("page"),
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

    private fun assertPlacementEquals(a: Placement, b: Placement) {
        assertEquals(a.page, b.page)
        assertEquals(a.path, b.path)
        assertEquals(a.location, b.location)
        assertEquals(a.pageSize, b.pageSize)
        assertEquals(a.position, b.position)
        assertEquals(a.productId, b.productId)
        assertEquals(a.searchQuery, b.searchQuery)
        if(a.categoryIds == null){
            assertThat(b.categoryIds).isNull()
        } else {
            assertThat(a.categoryIds).containsExactlyInAnyOrderElementsOf(b.categoryIds)
        }
    }

    private fun assertEntityEquals(a: Entity?, b: Entity?) {
        if(a == null){
            assertThat(b).isNull()
        } else {
            assertThat(a.id).isEqualTo(b!!.id)
            assertThat(a.type).isEqualTo(b.type)
        }
    }
}
