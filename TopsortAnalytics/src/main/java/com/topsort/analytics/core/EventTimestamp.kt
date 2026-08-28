package com.topsort.analytics.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** RFC3339 with milliseconds and the local UTC offset, e.g. `2026-08-28T15:04:05.123-03:00`. */
internal fun eventNow(): String = rfc3339(Date())

/**
 * A new formatter per call: SimpleDateFormat is not thread-safe, and reports arrive from any
 * thread. `XXX` (colon offset, `Z` for UTC) is available from API 24.
 */
internal fun rfc3339(date: Date, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        .apply { timeZone = zone }
        .format(date)
