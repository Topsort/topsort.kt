package com.topsort.analytics.core

import org.json.JSONObject

fun JSONObject.getStringOrNull(name: String): String? {
    return if (has(name)) {
        getString(name)
    } else null
}

fun JSONObject.getIntOrNull(name: String): Int? {
    return if (has(name)) {
        getInt(name)
    } else null
}

fun JSONObject.getStringListOrNull(name: String): List<String>? {
    return if (has(name)) {
        val array = getJSONArray(name)
        return (0 until array.length()).map { array[it].toString() }
    } else null
}
