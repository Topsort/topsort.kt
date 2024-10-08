package com.topsort.analytics

import com.topsort.analytics.core.eventNow
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchasedItem

fun getClickPromoted() : Click {
    return Click.Factory.buildPromoted(
        placement = getTestPlacement(),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        resolvedBidId = randomId("resolvedBid_"),
        additionalAttribution = "{\"additional\":\"attribution click\"}",
    )
}

fun getClickOrganic() : Click {
    return Click.Factory.buildOrganic(
        placement = getTestPlacement(),
        entity = Entity(
            type = EntityType.PRODUCT,
            id = randomId("product_"),
        ),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        additionalAttribution = "{\"additional\":\"attribution click\"}",
    )
}

fun getImpressionPromoted() : Impression {
    return Impression.Factory.buildPromoted (
        placement = getTestPlacement(),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        resolvedBidId = randomId("resolvedBid_"),
        additionalAttribution = "{\"additional\":\"attribution impression\"}",
    )
}

fun getImpressionOrganic() : Impression {
    return Impression.Factory.buildOrganic (
        placement = getTestPlacement(),
        entity = Entity(
            type = EntityType.PRODUCT,
            id = randomId("product_"),
        ),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        additionalAttribution = "{\"additional\":\"attribution impression\"}",
    )
}

fun getRandomPurchase() : Purchase {
    return Purchase(
        opaqueUserId = randomId("oId_"),
        occurredAt = eventNow(),
        items = listOf(
            PurchasedItem(
                productId = randomId("p_"),
                quantity = 1,
                unitPrice = 100,
                resolvedBidId = randomId("resolvedBid_"),
            )
        ),
        id = randomId("orderId_"),
    )
}

private fun getTestPlacement() : Placement {
    return Placement(
        path = "test",
        position = 2,
        page = 1,
        pageSize = 20,
        productId = randomId(),
        categoryIds = listOf("cat1", "cat2"),
        searchQuery = "search query",
        location = "gibraltar",
    )
}
