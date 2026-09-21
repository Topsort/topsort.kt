package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.auctions.Device
import org.json.JSONArray
import org.json.JSONObject

data class RenderEvent(
    val renders: List<Render>,
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        renders.indices.map {
            array.put(it, renders[it].toJsonObject())
        }
        return JSONObject().put("renders", array)
    }

    companion object {
        fun fromJson(json: String?): RenderEvent? {
            if (json == null) return null
            val array = JSONObject(json).getJSONArray("renders")
            val renders = Render.Factory.fromJsonArray(array)

            return RenderEvent(renders = renders)
        }
    }
}

data class Render private constructor(

    /**
     * The ID for the auction the ad won. Renders are sponsored ads only - unlike Impression and
     * Click, there is no organic variant and no entity field.
     */
    val resolvedBidId: String,

    val placement: Placement,

    /**
     * RFC3339 formatted timestamp including UTC offset.
     */
    val occurredAt: String,

    /**
     * The opaque user ID which allows correlating user activity.
     */
    val opaqueUserId: String,

    /**
     * The marketplace's unique ID for the render.
     */
    val id: String,

    /**
     * The device type where the render occurred.
     */
    val deviceType: Device? = null,

    /**
     * The channel where the render occurred.
     */
    val channel: Channel? = null,

    /**
     * The page context where the render occurred.
     */
    val page: Page? = null,
) : JsonSerializable {
    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("resolvedBidId", resolvedBidId)
            .put("placement", placement.toJsonObject())
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .apply {
                deviceType?.let { put("deviceType", it.value) }
                channel?.let { put("channel", it.value) }
                page?.let { put("page", it.toJsonObject()) }
            }
    }

    object Factory {

        @JvmOverloads
        fun build(
            resolvedBidId: String,
            placement: Placement,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            deviceType: Device? = null,
            channel: Channel? = null,
            page: Page? = null,
        ): Render {
            return Render(
                resolvedBidId = resolvedBidId,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        fun fromJsonObject(json: JSONObject): Render {
            return Render(
                resolvedBidId = json.getString("resolvedBidId"),
                placement = Placement.fromJsonObject(json.getJSONObject("placement")),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                deviceType = Device.fromValue(json.getStringOrNull("deviceType")),
                channel = Channel.fromValue(json.getStringOrNull("channel")),
                page = json.optJSONObject("page")?.let { Page.Factory.fromJsonObject(it) },
            )
        }

        fun fromJsonArray(array: JSONArray): List<Render> =
            getListFromJsonArray(
                array
            ) {
                fromJsonObject(it)
            }
    }
}
