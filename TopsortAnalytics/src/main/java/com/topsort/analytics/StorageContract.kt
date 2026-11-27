package com.topsort.analytics

import com.topsort.analytics.model.EventType

internal const val PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE"
internal const val ENCRYPTED_PREFERENCES_NAME = "TOPSORT_EVENTS_CACHE_ENCRYPTED"

internal const val KEY_TOKEN = "KEY_TOKEN"
internal const val KEY_SESSION_ID = "KEY_SESSION_ID"
internal const val KEY_RECORD = "KEY_RECORD_%d"
internal const val KEY_RECORD_PREFIX = "KEY_RECORD_"
internal const val KEY_RECENT_RECORD_ID = "KEY_RECORD_ID"

/** Mirrors the top-level key each event model serialises itself under. */
internal val EVENT_TYPE_BY_JSON_KEY = mapOf(
    "impressions" to EventType.Impression,
    "clicks" to EventType.Click,
    "purchases" to EventType.Purchase,
    "pageviews" to EventType.PageView,
    "renders" to EventType.Render,
)

internal const val EMITTER_WORK_NAME = "TopsortAnalyticsReporter"
internal const val EXTRA_RECORD_ID = "EXTRA_RECORD_ID"
internal const val EXTRA_EVENT_TYPE = "EXTRA_EVENT_TYPE"
internal const val SWEEP_WORK_NAME = "TopsortAnalyticsPendingSweep"
