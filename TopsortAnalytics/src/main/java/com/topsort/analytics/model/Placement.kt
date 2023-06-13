package com.topsort.analytics.model

data class Placement(
    /**
     * A marketplace assigned name for a page
     */
    val page: String,

    /**
     * A marketplace defined name for a page part
     */
    val location: String? = null
)
