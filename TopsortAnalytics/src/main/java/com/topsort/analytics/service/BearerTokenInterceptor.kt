package com.topsort.analytics.service

import com.topsort.analytics.Cache
import okhttp3.Interceptor
import okhttp3.Response

private const val JSON_CONTENT = "application/json"

internal class BearerTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .addHeader("Accept", JSON_CONTENT)
            .addHeader("content-type", JSON_CONTENT)
            .addHeader("Authorization", "Bearer ${Cache.token}")

        return chain.proceed(builder.build())
    }
}
