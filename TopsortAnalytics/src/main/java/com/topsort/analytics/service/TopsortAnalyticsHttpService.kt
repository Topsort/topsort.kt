package com.topsort.analytics.service

import androidx.annotation.VisibleForTesting
import com.topsort.analytics.Cache
import com.topsort.analytics.core.HttpClient
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.model.auctions.ApiConstants
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent

internal object TopsortAnalyticsHttpService {

    val httpClient: HttpClient = HttpClient("${ApiConstants.BASE_API_URL}${ApiConstants.EVENTS_ENDPOINT}")

    private val defaultService: Service = buildService()

    /**
     * The current service instance, can be replaced for testing
     */
    var service: Service = defaultService
        private set

    /**
     * Sets a mock implementation for testing purposes
     */
    @VisibleForTesting
    fun setMockService(mockService: Service) {
        service = mockService
    }

    /**
     * Resets to the default implementation
     */
    @VisibleForTesting
    fun resetToDefaultService() {
        service = defaultService
    }

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

            override fun reportPageView(pageViewEvent: PageViewEvent): HttpResponse {
                return reportSerializedEvent(pageViewEvent.toJsonObject().toString())
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

        fun reportPageView(pageViewEvent: PageViewEvent): HttpResponse

        fun reportEvent(event: Event): HttpResponse
    }
}
