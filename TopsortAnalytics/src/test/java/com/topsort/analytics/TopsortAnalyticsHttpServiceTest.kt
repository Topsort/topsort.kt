package com.topsort.analytics

import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.model.PurchasedItem
import com.topsort.analytics.model.Session
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.Before
import org.junit.Test

internal class TopsortAnalyticsHttpServiceTest {

    lateinit var analytics: TopsortAnalytics
    lateinit var service: TopsortAnalyticsHttpService
    lateinit var session: Session

    @Before
    fun setup() {
        analytics = Analytics
        service = TopsortAnalyticsHttpService
        session = Session("s_sessionId")
    }

    @Test
    fun `send impressions`() {
        service.service.reportImpression(
            ImpressionEvent(
                session = session,
                impressions = listOf(
                    Impression(
                        placement = Placement(page = "1"),
                        productId = "p_id123qwerty",
                    )
                )
            )
        )
    }

    @Test
    fun `send clicks`() {
        service.service.reportClick(
            ClickEvent(
                session = session,
                placement = Placement(page = "1"),
                productId = "p_id123qwerty",
            )
        )
    }

    @Test
    fun `send purchases`() {
        service.service.reportPurchase(
            PurchaseEvent(
                session = session,
                purchasedAt = ISODateTimeFormat.dateTime().print(DateTime()),
                items = listOf(PurchasedItem(
                    productId = "p_item",
                    quantity = 1,
                    )
                ),
                id = null,
            )
        )
    }

    @Test
    fun `bearer token authentication`() {
        service.service.reportImpression(
            ImpressionEvent(
                session = session,
                impressions = listOf(
                    Impression(
                        placement = Placement(page = "1"),
                        productId = "p_id123qwerty",
                    )
                )
            )
        )
    }
}