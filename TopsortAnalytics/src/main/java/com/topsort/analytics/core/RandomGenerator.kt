package com.topsort.analytics.core

internal fun randomId(prefix : String = "", size : Int = 32): String {
    val allowedCharacters =   ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val rand = List(size) { allowedCharacters.random() }.joinToString("")
    return "${prefix}${rand}"
}
