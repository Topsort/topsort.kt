package com.topsort.analytics.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class EventTimestampTest {

    @Test
    fun `formats RFC3339 with milliseconds and a colon offset`() {
        assertThat(rfc3339(Date(1_700_000_000_123), TimeZone.getTimeZone("GMT-03:00")))
            .isEqualTo("2023-11-14T19:13:20.123-03:00")
    }

    @Test
    fun `prints Z for UTC`() {
        assertThat(rfc3339(Date(0), TimeZone.getTimeZone("UTC"))).isEqualTo("1970-01-01T00:00:00.000Z")
    }
}
