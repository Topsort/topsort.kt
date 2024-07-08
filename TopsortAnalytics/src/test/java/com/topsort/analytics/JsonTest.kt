package com.topsort.analytics

import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
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
        val deserialized = Click.fromJsonObject(JSONObject(serialized))

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
        val deserialized = Impression.fromJsonObject(JSONObject(serialized))

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
        val deserialized = Purchase.fromJsonObject(JSONObject(serialized))

        assertEquals(purchase.id, deserialized.id)
        assertEquals(purchase.occurredAt, deserialized.occurredAt)
        assertEquals(purchase.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(purchase.items.size, deserialized.items.size)
        for(i in 0 until purchase.items.size){
            assertPurchasedItemEquals(purchase.items[i], deserialized.items[i])
        }
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
            assertThat(b.categoryIds).isNotNull()
            assertThat(a.categoryIds).containsExactlyInAnyOrderElementsOf(b.categoryIds)
        }
    }

    private fun assertEntityEquals(a: Entity?, b: Entity?) {
        if(a == null){
            assertThat(b).isNull()
        } else {
            assertThat(b).isNotNull()
            assertThat(a.id).isEqualTo(b!!.id)
            assertThat(a.type).isEqualTo(b.type)
        }
    }

    private fun assertPurchasedItemEquals(a: PurchasedItem, b: PurchasedItem){
        assertThat(a.productId).isEqualTo(b.productId)
        assertThat(a.resolvedBidId).isEqualTo(b.resolvedBidId)
        assertThat(a.quantity).isEqualTo(b.quantity)
        assertThat(a.unitPrice).isEqualTo(b.unitPrice)
    }
}
