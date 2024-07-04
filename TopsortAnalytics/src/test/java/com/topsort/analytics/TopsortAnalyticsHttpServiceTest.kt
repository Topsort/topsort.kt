package com.topsort.analytics

import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.model.PurchasedItem
import com.topsort.analytics.model.Session
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.Before
import org.junit.Test

internal class TopsortAnalyticsHttpServiceTest {

    private lateinit var analytics: TopsortAnalytics
    private lateinit var service: TopsortAnalyticsHttpService
    private lateinit var session: Session

    @Before
    fun setup() {
        analytics = Analytics
        service = TopsortAnalyticsHttpService
        session = Session("s_sessionId")
    }

    @Test
    fun `send impressions`() {
        val resp = service.service.reportImpression(
            ImpressionEvent(
                impressions = listOf(
                    Impression(
                        placement = Placement(page = 1, path = "test"),
                        entity = Entity(
                            type = EntityType.Product,
                            id = "p_id123qwerty",
                        ),
                        occurredAt = eventNow(),
                        opaqueUserId = session.sessionId,
                        id = "id_marketplace"
                    )
                )
            )
        )

        assert(resp.code == 200)
    }

    @Test
    fun `send clicks`() {
        service.service.reportClick(
            ClickEvent(
                clicks = listOf(
                    Click(
                        placement = Placement(page = 1, path = "test"),
                        entity = Entity(
                            type = EntityType.Product,
                            id = "p_id123qwerty",
                        ),
                        occurredAt = eventNow(),
                        opaqueUserId = session.sessionId,
                        id = "id_marketplace",
                    )
                )
            )
        )
    }

    @Test
    fun `send purchases`() {
        service.service.reportPurchase(
            PurchaseEvent(
                purchases = listOf(
                    Purchase(
                        opaqueUserId = session.sessionId,

                        occurredAt = eventNow(),
                        items = listOf(
                            PurchasedItem(
                                productId = "p_item",
                                quantity = 1,
                            )
                        ),
                        id = "id_order",
                    )
                )
            )
        )
    }

    private fun eventNow(): String {
        return ISODateTimeFormat.dateTime().print(DateTime())
    }
}
