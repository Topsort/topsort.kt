package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

internal class PurchaseEventTest {

    @Test
    fun `purchase with minimal fields`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "order-456",
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1)
            )
        )

        assertThat(purchase.id).isEqualTo("order-456")
        assertThat(purchase.items).hasSize(1)
        assertThat(purchase.deviceType).isNull()
        assertThat(purchase.channel).isNull()
    }

    @Test
    fun `purchase with all optional fields`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-123",
            id = "order-456",
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1)
            ),
            deviceType = "mobile",
            channel = "onsite"
        )

        assertThat(purchase.deviceType).isEqualTo("mobile")
        assertThat(purchase.channel).isEqualTo("onsite")
    }

    @Test
    fun `purchasedItem with minimal fields`() {
        val item = PurchasedItem(
            productId = "product-123",
            quantity = 2
        )

        assertThat(item.productId).isEqualTo("product-123")
        assertThat(item.quantity).isEqualTo(2)
        assertThat(item.unitPrice).isNull()
        assertThat(item.resolvedBidId).isNull()
        assertThat(item.vendorId).isNull()
    }

    @Test
    fun `purchasedItem with all fields`() {
        val item = PurchasedItem(
            productId = "product-123",
            quantity = 3,
            unitPrice = 1295,
            resolvedBidId = "bid-456",
            vendorId = "vendor-789"
        )

        assertThat(item.productId).isEqualTo("product-123")
        assertThat(item.quantity).isEqualTo(3)
        assertThat(item.unitPrice).isEqualTo(1295)
        assertThat(item.resolvedBidId).isEqualTo("bid-456")
        assertThat(item.vendorId).isEqualTo("vendor-789")
    }

    @Test
    fun `toJsonObject serializes purchase correctly`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "order-1",
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1, unitPrice = 1000)
            ),
            deviceType = "desktop",
            channel = "offsite"
        )

        val json = purchase.toJsonObject()

        assertThat(json.getString("occurredAt")).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(json.getString("opaqueUserId")).isEqualTo("user-1")
        assertThat(json.getString("id")).isEqualTo("order-1")
        assertThat(json.getString("deviceType")).isEqualTo("desktop")
        assertThat(json.getString("channel")).isEqualTo("offsite")
        assertThat(json.getJSONArray("items").length()).isEqualTo(1)
    }

    @Test
    fun `toJsonObject omits null optional fields`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "order-1",
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1)
            )
        )

        val json = purchase.toJsonObject()

        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
    }

    @Test
    fun `purchasedItem toJsonObject serializes correctly`() {
        val item = PurchasedItem(
            productId = "p1",
            quantity = 2,
            unitPrice = 1500,
            resolvedBidId = "bid-1",
            vendorId = "vendor-1"
        )

        val json = item.toJsonObject()

        assertThat(json.getString("productId")).isEqualTo("p1")
        assertThat(json.getInt("quantity")).isEqualTo(2)
        assertThat(json.getInt("unitPrice")).isEqualTo(1500)
        assertThat(json.getString("resolvedBidId")).isEqualTo("bid-1")
        assertThat(json.getString("vendorId")).isEqualTo("vendor-1")
    }

    @Test
    fun `fromJsonObject deserializes purchase`() {
        val json = JSONObject("""
            {
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-test",
                "id": "order-test",
                "items": [
                    {"productId": "p1", "quantity": 1}
                ]
            }
        """.trimIndent())

        val purchase = Purchase.fromJsonObject(json)

        assertThat(purchase.occurredAt).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(purchase.opaqueUserId).isEqualTo("user-test")
        assertThat(purchase.id).isEqualTo("order-test")
        assertThat(purchase.items).hasSize(1)
    }

    @Test
    fun `fromJsonObject deserializes purchase with all fields`() {
        val json = JSONObject("""
            {
                "occurredAt": "2024-01-15T10:30:00Z",
                "opaqueUserId": "user-test",
                "id": "order-test",
                "items": [
                    {"productId": "p1", "quantity": 1}
                ],
                "deviceType": "mobile",
                "channel": "instore"
            }
        """.trimIndent())

        val purchase = Purchase.fromJsonObject(json)

        assertThat(purchase.deviceType).isEqualTo("mobile")
        assertThat(purchase.channel).isEqualTo("instore")
    }

    @Test
    fun `purchasedItem fromJsonObject deserializes correctly`() {
        val json = JSONObject("""
            {
                "productId": "product-abc",
                "quantity": 5,
                "unitPrice": 2500,
                "resolvedBidId": "bid-xyz",
                "vendorId": "vendor-123"
            }
        """.trimIndent())

        val item = PurchasedItem.fromJsonObject(json)

        assertThat(item.productId).isEqualTo("product-abc")
        assertThat(item.quantity).isEqualTo(5)
        assertThat(item.unitPrice).isEqualTo(2500)
        assertThat(item.resolvedBidId).isEqualTo("bid-xyz")
        assertThat(item.vendorId).isEqualTo("vendor-123")
    }

    @Test
    fun `purchasedItem fromJsonObject handles missing optional fields`() {
        val json = JSONObject("""
            {
                "productId": "p1",
                "quantity": 1
            }
        """.trimIndent())

        val item = PurchasedItem.fromJsonObject(json)

        assertThat(item.unitPrice).isNull()
        assertThat(item.resolvedBidId).isNull()
        assertThat(item.vendorId).isNull()
    }

    @Test
    fun `roundtrip purchase serialization`() {
        val original = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-roundtrip",
            id = "order-roundtrip",
            items = listOf(
                PurchasedItem(
                    productId = "p-rt",
                    quantity = 3,
                    unitPrice = 1999,
                    resolvedBidId = "bid-rt",
                    vendorId = "vendor-rt"
                )
            ),
            deviceType = "desktop",
            channel = "onsite"
        )

        val json = original.toJsonObject()
        val deserialized = Purchase.fromJsonObject(json)

        assertThat(deserialized.occurredAt).isEqualTo(original.occurredAt)
        assertThat(deserialized.opaqueUserId).isEqualTo(original.opaqueUserId)
        assertThat(deserialized.id).isEqualTo(original.id)
        assertThat(deserialized.deviceType).isEqualTo(original.deviceType)
        assertThat(deserialized.channel).isEqualTo(original.channel)
        assertThat(deserialized.items).hasSize(1)
        assertThat(deserialized.items[0].productId).isEqualTo(original.items[0].productId)
        assertThat(deserialized.items[0].vendorId).isEqualTo(original.items[0].vendorId)
    }

    @Test
    fun `purchaseEvent serialization with multiple purchases`() {
        val purchases = listOf(
            Purchase(
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-1",
                id = "order-1",
                items = listOf(PurchasedItem(productId = "p1", quantity = 1))
            ),
            Purchase(
                occurredAt = "2024-01-15T10:31:00Z",
                opaqueUserId = "user-2",
                id = "order-2",
                items = listOf(PurchasedItem(productId = "p2", quantity = 2))
            )
        )
        val event = PurchaseEvent(purchases)

        val json = event.toJsonObject()

        assertThat(json.getJSONArray("purchases").length()).isEqualTo(2)
    }

    @Test
    fun `purchaseEvent roundtrip serialization`() {
        val purchases = listOf(
            Purchase(
                occurredAt = "2024-01-15T10:30:00Z",
                opaqueUserId = "user-rt",
                id = "order-rt",
                items = listOf(
                    PurchasedItem(productId = "p-rt", quantity = 1, unitPrice = 500)
                )
            )
        )
        val original = PurchaseEvent(purchases)

        val jsonString = original.toJsonObject().toString()
        val deserialized = PurchaseEvent.fromJson(jsonString)

        assertThat(deserialized).isNotNull
        assertThat(deserialized!!.purchases).hasSize(1)
        assertThat(deserialized.purchases[0].id).isEqualTo("order-rt")
        assertThat(deserialized.purchases[0].items[0].productId).isEqualTo("p-rt")
    }

    @Test
    fun `purchaseEvent fromJson returns null for null input`() {
        val result = PurchaseEvent.fromJson(null)

        assertThat(result).isNull()
    }

    @Test
    fun `purchase with multiple items serializes correctly`() {
        val purchase = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "order-multi",
            items = listOf(
                PurchasedItem(productId = "p1", quantity = 1, unitPrice = 1000),
                PurchasedItem(productId = "p2", quantity = 2, unitPrice = 1500),
                PurchasedItem(productId = "p3", quantity = 3, unitPrice = 2000, vendorId = "v1")
            )
        )

        val json = purchase.toJsonObject()
        val itemsArray = json.getJSONArray("items")

        assertThat(itemsArray.length()).isEqualTo(3)
        assertThat(itemsArray.getJSONObject(0).getString("productId")).isEqualTo("p1")
        assertThat(itemsArray.getJSONObject(1).getString("productId")).isEqualTo("p2")
        assertThat(itemsArray.getJSONObject(2).getString("productId")).isEqualTo("p3")
        assertThat(itemsArray.getJSONObject(2).getString("vendorId")).isEqualTo("v1")
    }

    @Test
    fun `fromJsonArray deserializes list of purchases`() {
        val jsonArray = org.json.JSONArray("""
            [
                {
                    "occurredAt": "2024-01-15T10:30:00Z",
                    "opaqueUserId": "user-1",
                    "id": "order-1",
                    "items": [{"productId": "p1", "quantity": 1}]
                },
                {
                    "occurredAt": "2024-01-15T10:31:00Z",
                    "opaqueUserId": "user-2",
                    "id": "order-2",
                    "items": [{"productId": "p2", "quantity": 2}]
                }
            ]
        """.trimIndent())

        val purchases = Purchase.fromJsonArray(jsonArray)

        assertThat(purchases).hasSize(2)
        assertThat(purchases[0].id).isEqualTo("order-1")
        assertThat(purchases[1].id).isEqualTo("order-2")
    }

    @Test
    fun `purchase data class equality`() {
        val items = listOf(PurchasedItem(productId = "p1", quantity = 1))
        val purchase1 = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "order-1",
            items = items
        )
        val purchase2 = Purchase(
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "order-1",
            items = items
        )

        assertThat(purchase1).isEqualTo(purchase2)
    }

    @Test
    fun `purchasedItem data class equality`() {
        val item1 = PurchasedItem(productId = "p1", quantity = 2, unitPrice = 1000)
        val item2 = PurchasedItem(productId = "p1", quantity = 2, unitPrice = 1000)
        val item3 = PurchasedItem(productId = "p1", quantity = 3, unitPrice = 1000)

        assertThat(item1).isEqualTo(item2)
        assertThat(item1).isNotEqualTo(item3)
    }
}
