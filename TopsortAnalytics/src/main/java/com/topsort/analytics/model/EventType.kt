package com.topsort.analytics.model

enum class EventType {

    Impression,
    Click,
    Purchase,
    PageView,
    Render,

    // Appended, never inserted: the worker stores a record's type by ordinal in its input data.
    Video,
}
