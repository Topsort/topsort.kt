package com.topsort.analytics.core

object ServiceSettings {
    lateinit var baseApiUrl: String
    lateinit var bearerToken: String

    fun isSetup() : Boolean{
        return this::baseApiUrl.isInitialized
    }
}
