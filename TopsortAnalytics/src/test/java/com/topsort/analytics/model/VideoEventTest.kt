package com.topsort.analytics.model

import com.topsort.analytics.model.auctions.Device
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test

internal class VideoEventTest {

    private fun checkpoint(
        checkpoint: VideoCheckpoint,
        placement: Placement? = null,
    ) = VideoEvent.Factory.buildCheckpoint(
        resolvedBidId = "bid-1",
        checkpoint = checkpoint,
        occurredAt = "2024-01-15T10:30:00Z",
        opaqueUserId = "user-1",
        id = "video-1",
        placement = placement,
    )

    private fun error(errorCode: Int) = VideoEvent.Factory.buildError(
        resolvedBidId = "bid-1",
        errorCode = errorCode,
        occurredAt = "2024-01-15T10:30:00Z",
        opaqueUserId = "user-1",
        id = "video-1",
    )

    @Test
    fun `each checkpoint serializes to its VAST event name`() {
        val names = VideoCheckpoint.entries.map {
            checkpoint(it).toJsonObject().getString("eventType")
        }

        assertThat(names).containsExactly(
            "start", "firstQuartile", "midpoint", "thirdQuartile", "complete",
        )
    }

    @Test
    fun `a checkpoint never sends an errorCode`() {
        VideoCheckpoint.entries.forEach {
            assertThat(checkpoint(it).toJsonObject().has("errorCode")).isFalse()
        }
    }

    @Test
    fun `an error serializes its eventType and errorCode`() {
        val json = error(405).toJsonObject()

        assertThat(json.getString("eventType")).isEqualTo("error")
        assertThat(json.getInt("errorCode")).isEqualTo(405)
    }

    @Test
    fun `required fields are serialized`() {
        val json = checkpoint(VideoCheckpoint.START).toJsonObject()

        assertThat(json.getString("resolvedBidId")).isEqualTo("bid-1")
        assertThat(json.getString("occurredAt")).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(json.getString("opaqueUserId")).isEqualTo("user-1")
        assertThat(json.getString("id")).isEqualTo("video-1")
    }

    @Test
    fun `null optional fields are omitted from json`() {
        val json = checkpoint(VideoCheckpoint.START).toJsonObject()

        assertThat(json.has("placement")).isFalse()
        assertThat(json.has("deviceType")).isFalse()
        assertThat(json.has("channel")).isFalse()
        assertThat(json.has("page")).isFalse()
    }

    @Test
    fun `optional fields are serialized when present`() {
        val event = VideoEvent.Factory.buildCheckpoint(
            resolvedBidId = "bid-1",
            checkpoint = VideoCheckpoint.MIDPOINT,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "video-1",
            placement = Placement(path = "/categories/dairy", position = 1),
            deviceType = Device.MOBILE,
            channel = Channel.ONSITE,
            page = Page.Factory.build(type = PageType.CATEGORY),
        )

        val json = event.toJsonObject()

        assertThat(json.getJSONObject("placement").getString("path")).isEqualTo("/categories/dairy")
        assertThat(json.getString("deviceType")).isEqualTo("mobile")
        assertThat(json.getString("channel")).isEqualTo("onsite")
        assertThat(json.getJSONObject("page").getString("type")).isEqualTo("category")
    }

    @Test
    fun `every checkpoint survives a roundtrip`() {
        VideoCheckpoint.entries.forEach {
            val original = checkpoint(it, placement = Placement(path = "/search"))

            val deserialized = VideoEvent.Factory.fromJsonObject(original.toJsonObject())

            assertThat(deserialized).isEqualTo(original)
        }
    }

    @Test
    fun `an error survives a roundtrip`() {
        val original = error(402)

        val deserialized = VideoEvent.Factory.fromJsonObject(original.toJsonObject())

        assertThat(deserialized).isEqualTo(original)
        assertThat(deserialized.eventType).isEqualTo(VideoEventType.Error(402))
    }

    @Test
    fun `an unknown eventType fails to deserialize`() {
        val json = checkpoint(VideoCheckpoint.START).toJsonObject().put("eventType", "pause")

        assertThatThrownBy { VideoEvent.Factory.fromJsonObject(json) }
            .isInstanceOf(JSONException::class.java)
    }

    @Test
    fun `an error without an errorCode fails to deserialize`() {
        val json = error(405).toJsonObject().apply { remove("errorCode") }

        assertThatThrownBy { VideoEvent.Factory.fromJsonObject(json) }
            .isInstanceOf(JSONException::class.java)
    }

    @Test
    fun `checkpoint fromValue parses known values and rejects others`() {
        assertThat(VideoCheckpoint.fromValue("thirdQuartile")).isEqualTo(VideoCheckpoint.THIRD_QUARTILE)
        assertThat(VideoCheckpoint.fromValue("error")).isNull()
        assertThat(VideoCheckpoint.fromValue(null)).isNull()
    }

    @Test
    fun `batch serializes under videoEvents`() {
        val batch = VideoEventBatch(listOf(checkpoint(VideoCheckpoint.START), error(405)))

        val json = batch.toJsonObject()

        assertThat(json.keys().asSequence().toList()).containsExactly("videoEvents")
        assertThat(json.getJSONArray("videoEvents").length()).isEqualTo(2)
    }

    @Test
    fun `batch roundtrip preserves every event`() {
        val original = VideoEventBatch(listOf(checkpoint(VideoCheckpoint.COMPLETE), error(405)))

        val deserialized = VideoEventBatch.fromJson(original.toJsonObject().toString())

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `batch fromJson returns null for null input`() {
        assertThat(VideoEventBatch.fromJson(null)).isNull()
    }

    @Test
    fun `fromJsonArray deserializes the spec's example payload`() {
        val array = JSONArray(
            """
            [
                {
                    "id": "5c0e7d2a-91b4-4f3e-8a6d-2e1f0c9b8a71",
                    "occurredAt": "2019-01-01T12:59:59-05:00",
                    "opaqueUserId": "71303ce0-de89-496d-8270-6434589615e8",
                    "resolvedBidId": "bid",
                    "eventType": "firstQuartile",
                    "placement": {"path": "/categories/dairy", "position": 1},
                    "deviceType": "mobile",
                    "channel": "onsite"
                },
                {
                    "id": "9a4f1b3c-6d2e-4c8b-b7a0-3f5e6d7c8b92",
                    "occurredAt": "2019-01-01T13:00:03-05:00",
                    "opaqueUserId": "71303ce0-de89-496d-8270-6434589615e8",
                    "resolvedBidId": "bid",
                    "eventType": "error",
                    "errorCode": 405,
                    "deviceType": "mobile",
                    "channel": "onsite"
                }
            ]
            """.trimIndent()
        )

        val events = VideoEvent.Factory.fromJsonArray(array)

        assertThat(events.map { it.eventType }).containsExactly(
            VideoEventType.Checkpoint(VideoCheckpoint.FIRST_QUARTILE),
            VideoEventType.Error(405),
        )
        assertThat(events[0].placement?.position).isEqualTo(1)
        assertThat(events[1].placement).isNull()
    }

    @Test
    fun `serialized keys stay within the VideoEvent schema`() {
        val allowed = setOf(
            "resolvedBidId", "eventType", "errorCode", "occurredAt", "opaqueUserId", "id",
            "placement", "page", "deviceType", "channel",
        )
        val event = VideoEvent.Factory.buildError(
            resolvedBidId = "bid-1",
            errorCode = 405,
            occurredAt = "2024-01-15T10:30:00Z",
            opaqueUserId = "user-1",
            id = "video-1",
            placement = Placement(path = "/search"),
            deviceType = Device.DESKTOP,
            channel = Channel.OFFSITE,
            page = Page.Factory.build(type = PageType.SEARCH),
        )

        val keys = JSONObject(event.toJsonObject().toString()).keys().asSequence().toSet()

        // The schema sets additionalProperties: false, so any other key is a 400.
        assertThat(allowed).containsAll(keys)
    }
}
