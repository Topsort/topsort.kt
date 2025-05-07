package com.topsort.analytics.service

import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.auctions.ApiConstants
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent

internal object TopsortAnalyticsHttpService {

    val httpClient: HttpClient = HttpClient("${ApiConstants.BASE_API_URL}${ApiConstants.EVENTS_ENDPOINT}")

    val service: Service = buildService()

    private fun buildService(): Service {
        return object : Service {
            private fun reportSerializedEvent(json: String): HttpResponse {
                return httpClient.post(json, Cache.token.ifEmpty { null })
            }

            override fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse {
                return reportSerializedEvent(impressionEvent.toJsonObject().toString())
            }

            override fun reportClick(clickEvent: ClickEvent): HttpResponse {
                return reportSerializedEvent(clickEvent.toJsonObject().toString())
            }

            override fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse {
                return reportSerializedEvent(purchaseEvent.toJsonObject().toString())
            }

            override fun reportEvent(event: Event): HttpResponse {
                return reportSerializedEvent(event.toJsonObject().toString())
            }
        }
    }

    interface Service {
        fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse

        fun reportClick(clickEvent: ClickEvent): HttpResponse

        fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse

        fun reportEvent(event: Event): HttpResponse
    }
}
