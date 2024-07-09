package com.topsort.analytics.core

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

fun eventNow(): String {
    return ISODateTimeFormat.dateTime().print(DateTime())
}
