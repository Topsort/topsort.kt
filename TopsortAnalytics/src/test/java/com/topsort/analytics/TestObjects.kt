package com.topsort.analytics

import com.topsort.analytics.core.eventNow
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.Channel
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickType
import com.topsort.analytics.model.auctions.Device
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Page
import com.topsort.analytics.model.PageType
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

// Test objects with new enhanced event context fields using enums

fun getClickPromotedWithContext() : Click {
    return Click.Factory.buildPromoted(
        placement = getTestPlacement(),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        resolvedBidId = randomId("resolvedBid_"),
        additionalAttribution = "{\"additional\":\"attribution click\"}",
        deviceType = Device.MOBILE,
        channel = Channel.ONSITE,
        page = Page.Factory.build(type = PageType.SEARCH),
        clickType = ClickType.ADD_TO_CART,
    )
}

fun getClickOrganicWithContext() : Click {
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
        deviceType = Device.DESKTOP,
        channel = Channel.OFFSITE,
        page = Page.Factory.buildWithId(type = PageType.PDP, pageId = "product-123"),
        clickType = ClickType.PRODUCT,
    )
}

fun getImpressionPromotedWithContext() : Impression {
    return Impression.Factory.buildPromoted(
        placement = getTestPlacement(),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        resolvedBidId = randomId("resolvedBid_"),
        additionalAttribution = "{\"additional\":\"attribution impression\"}",
        deviceType = Device.MOBILE,
        channel = Channel.INSTORE,
        page = Page.Factory.build(type = PageType.CATEGORY),
    )
}

fun getImpressionOrganicWithContext() : Impression {
    return Impression.Factory.buildOrganic(
        placement = getTestPlacement(),
        entity = Entity(
            type = EntityType.PRODUCT,
            id = randomId("product_"),
        ),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        additionalAttribution = "{\"additional\":\"attribution impression\"}",
        deviceType = Device.DESKTOP,
        channel = Channel.ONSITE,
        page = Page.Factory.buildWithValues(type = PageType.HOME, values = listOf("home-1", "home-2")),
    )
}

fun getRandomPurchaseWithContext() : Purchase {
    return Purchase(
        opaqueUserId = randomId("oId_"),
        occurredAt = eventNow(),
        items = listOf(
            PurchasedItem(
                productId = randomId("p_"),
                quantity = 1,
                unitPrice = 100,
                resolvedBidId = randomId("resolvedBid_"),
                vendorId = randomId("vendor_"),
            )
        ),
        id = randomId("orderId_"),
        deviceType = Device.MOBILE,
        channel = Channel.OFFSITE,
        page = Page.Factory.build(type = PageType.CART),
    )
}
