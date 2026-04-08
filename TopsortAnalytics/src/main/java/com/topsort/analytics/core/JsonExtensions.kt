package com.topsort.analytics.core

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.getStringOrNull(name: String): String? {
    return if (has(name) && !isNull(name)) {
        getString(name)
    } else null
}

internal fun JSONObject.getIntOrNull(name: String): Int? {
    return if (has(name) && !isNull(name)) {
        getInt(name)
    } else null
}

internal fun JSONObject.getStringListOrNull(name: String): List<String>? {
    return if (has(name) && !isNull(name)) {
        val array = getJSONArray(name)
        (0 until array.length()).map { array[it].toString() }
    } else null
}

internal fun <T> getListFromJsonArray(array: JSONArray, jsonDeserializer: (JSONObject) -> T): List<T> {
    return (0 until array.length()).map {
        jsonDeserializer(array.getJSONObject(it))
    }
}
