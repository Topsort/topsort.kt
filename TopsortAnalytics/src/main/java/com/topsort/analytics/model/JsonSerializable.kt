package com.topsort.analytics.model

import org.json.JSONObject

interface JsonSerializable {
    fun toJsonObject(): JSONObject
}
