package com.topsort.analytics.service

import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent
import org.json.JSONObject

//private const val apiUrl = "https://api.topsort.com/v1/events"
private const val apiUrl = "localhost:8083"

internal object TopsortAnalyticsHttpService {

    val httpClient: HttpClient = HttpClient(apiUrl)

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
