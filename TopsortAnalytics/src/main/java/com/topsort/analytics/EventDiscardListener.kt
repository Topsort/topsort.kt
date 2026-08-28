package com.topsort.analytics

/**
 * Why cached events were thrown away without being delivered.
 *
 * New reasons may be added in minor releases; keep an `else` branch when matching on this.
 */
enum class DiscardReason {
    /** Evicted to keep the cache under its capacity bound. */
    CACHE_OVER_CAPACITY,

    /** The cached body will not parse back into an event, so nothing can ever send it. */
    UNPARSEABLE_BODY,

    /** The API rejected it with a 4xx; retrying the same body would be rejected again. */
    PERMANENTLY_REJECTED,

    /** The record's event type cannot be determined, so nothing knows where to send it. */
    UNKNOWN_EVENT_TYPE,
}

/**
 * Notified when the SDK discards cached events it will never deliver. Every discard is data the
 * marketplace will not see, so a host that wants to know about loss - a metric, a crash-reporter
 * breadcrumb - registers one via [Analytics.eventDiscardListener].
 *
 * Called on the SDK's worker thread, ahead of the work it was doing, so keep it quick. Held for
 * the life of the process: capture no Activity or Context, and set the listener to null to
 * unregister. Exceptions thrown from it are logged and swallowed.
 */
fun interface EventDiscardListener {
    fun onEventsDiscarded(reason: DiscardReason, count: Int)
}
