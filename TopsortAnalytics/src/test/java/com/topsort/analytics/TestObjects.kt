package com.topsort.analytics

import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchasedItem
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


fun getRandomClick() : Click {
    return Click(
        placement = Placement(page = 1, path = "test"),
        entity = Entity(
            type = EntityType.Product,
            id = randomId("product_"),
        ),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
        resolvedBidId = randomId("resolvedBid_"),
    )
}

fun getRandomImpression() : Impression{
    return Impression (
        placement = Placement(page = 1, path = "test"),
        entity = Entity(
            type = EntityType.Product,
            id = randomId("product_"),
        ),
        occurredAt = eventNow(),
        opaqueUserId = randomId("oId_"),
        id = randomId("mktId_"),
    )
}

fun getRandomPurchase() : Purchase{
    return Purchase(
        opaqueUserId = randomId("oId_"),

        occurredAt = eventNow(),
        items = listOf(
            PurchasedItem(
                productId = randomId("p_"),
                quantity = 1,
            )
        ),
        id = randomId("orderId_"),
    )
}




private fun eventNow(): String {
    return ISODateTimeFormat.dateTime().print(DateTime())
}

private fun randomId(prefix : String = "", size : Int = 32): String {
    val allowedCharacters =   ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val rand = List(size) { allowedCharacters.random() }.joinToString("")
    return "${prefix}${rand}"
}

