package com.topsort.analytics.service

import com.topsort.analytics.model.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val baseUrl = "https://api.topsort.com/v1/"

internal object TopsortAnalyticsService {

    val service: Service = buildService()

    private fun buildService(): Service {
        val httpClient: OkHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor(BearerTokenInterceptor())
                .build()

        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(
                Json.asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()))
            .build()
            .create(Service::class.java)
    }

    interface Service {
        @POST("events")
        fun reportImpression(@Body impressionEvent: ImpressionEvent): Call<ImpressionEventResponse?>

        @POST("events")
        fun reportClick(@Body clickEvent: ClickEvent): Call<ClickEventResponse?>

        @POST("events")
        fun reportPurchase(@Body purchaseEvent: PurchaseEvent): Call<PurchaseEventResponse?>
    }
}
