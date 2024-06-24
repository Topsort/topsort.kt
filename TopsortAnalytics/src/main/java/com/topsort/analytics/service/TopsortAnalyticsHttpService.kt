package com.topsort.analytics.service

import com.google.gson.Gson
import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PurchaseEvent

//private const val apiUrl = "https://api.topsort.com/v1/events"
private const val apiUrl = "localhost:8083"

internal object TopsortAnalyticsHttpService {

    val httpClient : HttpClient = HttpClient(apiUrl)

    val service: Service = buildService()

    private val gson = Gson()

    private fun buildService(): Service {
        return object : Service{
            private fun reportEvent(event: Any) {
                val json = gson.toJson(event)
                httpClient.post(json, Cache.token)
            }

            override fun reportImpression(impressionEvent: ImpressionEvent) {
                reportEvent(impressionEvent)
            }

            override fun reportClick(clickEvent: ClickEvent) {
                reportEvent(clickEvent)
            }

            override fun reportPurchase(purchaseEvent: PurchaseEvent) {
                reportEvent(purchaseEvent)
            }
        }

    }

    interface Service {
        fun reportImpression(impressionEvent: ImpressionEvent)

        fun reportClick(clickEvent: ClickEvent)

        fun reportPurchase(purchaseEvent: PurchaseEvent)
    }
}