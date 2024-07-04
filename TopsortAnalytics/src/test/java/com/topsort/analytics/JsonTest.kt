package com.topsort.analytics

import com.topsort.analytics.model.Click
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Console

internal class JsonTest {

    @Test
    fun `json click serialization`() {
        val click = getRandomClick()
        val serialized = click.toJsonObject().toString()
        println(serialized)
        val deserialized = Click.fromJsonObject(JSONObject(serialized))

        assertEquals(click.id, deserialized.id)
        assertEquals(1, 2)
        assertEquals(click.occurredAt, deserialized.occurredAt)
        assertEquals(click.opaqueUserId, deserialized.opaqueUserId)
        assertEquals(click.resolvedBidId, deserialized.resolvedBidId)
    }

    @Test
    fun `json impression serialization`() {
        assert(true)
    }

    @Test
    fun `json purchase serialization`() {
        assert(true)
    }
}
