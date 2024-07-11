package com.topsort.analytics.service

import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.events.ClickEvent
import com.topsort.analytics.model.events.ImpressionEvent
import com.topsort.analytics.model.events.PurchaseEvent
import org.json.JSONObject
import com.topsort.analytics.core.ServiceSettings.baseApiUrl


private const val EVENTS_ENDPOINT = "/v2/events"

internal object TopsortAnalyticsHttpService {

    val httpClient: HttpClient = HttpClient("${baseApiUrl}${EVENTS_ENDPOINT}")

    val service: Service = buildService()

    private fun buildService(): Service {
        return object : Service {
            private fun reportEvent(event: Any): HttpResponse {
                val json = JSONObject.wrap(event)!!.toString()
                return httpClient.post(json, Cache.token.ifEmpty { null })
            }

            override fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse {
                return reportEvent(impressionEvent)
            }

            override fun reportClick(clickEvent: ClickEvent): HttpResponse {
                return reportEvent(clickEvent)
            }

            override fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse {
                return reportEvent(purchaseEvent)
            }
        }
    }

    interface Service {
        fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse

        fun reportClick(clickEvent: ClickEvent): HttpResponse

        fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse
    }
}
