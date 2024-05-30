package com.topsort.analytics.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val sessionId: String
)
