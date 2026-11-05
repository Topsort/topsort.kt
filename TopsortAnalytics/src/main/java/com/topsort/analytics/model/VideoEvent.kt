package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.auctions.Device
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The API's `eventType` value for a failure; checkpoints carry their own in [VideoCheckpoint]. */
private const val ERROR_EVENT_TYPE = "error"

/**
 * The request body for one batch of video events. Internal: callers report through
 * [com.topsort.analytics.Analytics] and never need the wire wrapper itself.
 */
internal data class VideoEventBatch(
    val videoEvents: List<VideoEvent>,
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        videoEvents.indices.map {
            array.put(it, videoEvents[it].toJsonObject())
        }
        return JSONObject().put("videoEvents", array)
    }

    companion object {
        fun fromJson(json: String?): VideoEventBatch? {
            if (json == null) return null
            val array = JSONObject(json).getJSONArray("videoEvents")
            val videoEvents = VideoEvent.Factory.fromJsonArray(array)

            return VideoEventBatch(videoEvents = videoEvents)
        }
    }
}

/**
 * A playback checkpoint a sponsored video ad reached. The values are the IAB VAST tracking event
 * names, so a player's existing VAST trackers map one to one.
 */
enum class VideoCheckpoint(val value: String) {
    START("start"),
    FIRST_QUARTILE("firstQuartile"),
    MIDPOINT("midpoint"),
    THIRD_QUARTILE("thirdQuartile"),
    COMPLETE("complete");

    companion object {
        /**
         * Parse a checkpoint from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): VideoCheckpoint? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}

/**
 * What a video event reports: a playback checkpoint, or a failure to play.
 *
 * A failure always carries its VAST error code. The API rejects an `error` without one, and
 * modelling the two cases apart means such an event cannot be built in the first place, rather
 * than being caught at runtime.
 */
sealed class VideoEventType {

    /** The API's `eventType` value. */
    abstract val value: String

    data class Checkpoint(val checkpoint: VideoCheckpoint) : VideoEventType() {
        override val value: String get() = checkpoint.value
    }

    /**
     * @property errorCode The VAST error code describing why the video failed, for example `405`
     * for a media file that could not be displayed.
     */
    data class Error(val errorCode: Int) : VideoEventType() {
        override val value: String get() = ERROR_EVENT_TYPE
    }
}

/**
 * A sponsored video ad reached a playback checkpoint or failed to play.
 *
 * Video events are for video ads only: the `resolvedBidId` must come from a video campaign, and
 * the API rejects one from any other ad. They feed engagement and delivery reporting, and are
 * never charged or attributed.
 */
data class VideoEvent private constructor(

    /**
     * The ID for the auction the video ad won.
     */
    val resolvedBidId: String,

    /**
     * The checkpoint reached, or the failure with its error code.
     */
    val eventType: VideoEventType,

    /**
     * RFC3339 formatted timestamp including UTC offset.
     */
    val occurredAt: String,

    /**
     * The opaque user ID which allows correlating user activity.
     */
    val opaqueUserId: String,

    /**
     * The marketplace's unique ID for the video event.
     */
    val id: String,

    val placement: Placement? = null,

    /**
     * The device type where the video played.
     */
    val deviceType: Device? = null,

    /**
     * The channel where the video played.
     */
    val channel: Channel? = null,

    /**
     * The page context where the video played.
     */
    val page: Page? = null,
) : JsonSerializable {
    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("resolvedBidId", resolvedBidId)
            .put("eventType", eventType.value)
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .apply {
                (eventType as? VideoEventType.Error)?.let { put("errorCode", it.errorCode) }
                placement?.let { put("placement", it.toJsonObject()) }
                deviceType?.let { put("deviceType", it.value) }
                channel?.let { put("channel", it.value) }
                page?.let { put("page", it.toJsonObject()) }
            }
    }

    object Factory {

        @JvmOverloads
        fun buildCheckpoint(
            resolvedBidId: String,
            checkpoint: VideoCheckpoint,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            placement: Placement? = null,
            deviceType: Device? = null,
            channel: Channel? = null,
            page: Page? = null,
        ): VideoEvent {
            return VideoEvent(
                resolvedBidId = resolvedBidId,
                eventType = VideoEventType.Checkpoint(checkpoint),
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                placement = placement,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        @JvmOverloads
        fun buildError(
            resolvedBidId: String,
            errorCode: Int,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            placement: Placement? = null,
            deviceType: Device? = null,
            channel: Channel? = null,
            page: Page? = null,
        ): VideoEvent {
            return VideoEvent(
                resolvedBidId = resolvedBidId,
                eventType = VideoEventType.Error(errorCode),
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                placement = placement,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        fun fromJsonObject(json: JSONObject): VideoEvent {
            return VideoEvent(
                resolvedBidId = json.getString("resolvedBidId"),
                eventType = eventTypeFromJsonObject(json),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                placement = json.optJSONObject("placement")?.let { Placement.fromJsonObject(it) },
                deviceType = Device.fromValue(json.getStringOrNull("deviceType")),
                channel = Channel.fromValue(json.getStringOrNull("channel")),
                page = json.optJSONObject("page")?.let { Page.Factory.fromJsonObject(it) },
            )
        }

        fun fromJsonArray(array: JSONArray): List<VideoEvent> =
            getListFromJsonArray(
                array
            ) {
                fromJsonObject(it)
            }

        private fun eventTypeFromJsonObject(json: JSONObject): VideoEventType {
            val value = json.getString("eventType")
            if (value == ERROR_EVENT_TYPE) {
                return VideoEventType.Error(json.getInt("errorCode"))
            }
            val checkpoint = VideoCheckpoint.fromValue(value)
                ?: throw JSONException("Unknown video eventType: $value")
            return VideoEventType.Checkpoint(checkpoint)
        }
    }
}
