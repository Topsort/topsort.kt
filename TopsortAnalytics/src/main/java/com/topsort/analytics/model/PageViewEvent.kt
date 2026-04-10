package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.auctions.Device
import org.json.JSONArray
import org.json.JSONObject

/**
 * Event representing a page view.
 */
internal data class PageViewEvent(
    val pageviews: List<PageView>,
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        pageviews.indices.map {
            array.put(it, pageviews[it].toJsonObject())
        }
        return JSONObject().put("pageviews", array)
    }

    companion object {
        fun fromJson(json: String?): PageViewEvent? {
            if (json == null) return null
            val array = JSONObject(json).getJSONArray("pageviews")
            val pageviews = PageView.Factory.fromJsonArray(array)

            return PageViewEvent(pageviews = pageviews)
        }
    }
}

/**
 * Represents a single page view event.
 */
internal data class PageView private constructor(

    /**
     * The page being viewed.
     */
    val page: Page,

    /**
     * RFC3339 formatted timestamp including UTC offset.
     */
    val occurredAt: String,

    /**
     * The opaque user ID which allows correlating user activity.
     */
    val opaqueUserId: String,

    /**
     * The marketplace's unique ID for this page view event.
     */
    val id: String,

    /**
     * The device type where the page view occurred.
     */
    val deviceType: Device? = null,

    /**
     * The channel where the page view occurred.
     */
    val channel: Channel? = null,
) : JsonSerializable {

    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("page", page.toJsonObject())
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .apply {
                deviceType?.let { put("deviceType", it.value) }
                channel?.let { put("channel", it.value) }
            }
    }

    object Factory {

        /**
         * Build a page view event.
         *
         * @param page The page being viewed
         * @param occurredAt RFC3339 formatted timestamp
         * @param opaqueUserId The opaque user ID
         * @param id The marketplace's unique ID for this event
         * @param deviceType Optional device type
         * @param channel Optional channel
         */
        @JvmOverloads
        fun build(
            page: Page,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            deviceType: Device? = null,
            channel: Channel? = null,
        ): PageView {
            return PageView(
                page = page,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                deviceType = deviceType,
                channel = channel,
            )
        }

        fun fromJsonObject(json: JSONObject): PageView {
            return PageView(
                page = Page.Factory.fromJsonObject(json.getJSONObject("page")),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                deviceType = Device.fromValue(json.getStringOrNull("deviceType")),
                channel = Channel.fromValue(json.getStringOrNull("channel")),
            )
        }

        fun fromJsonArray(array: JSONArray): List<PageView> =
            getListFromJsonArray(array) {
                fromJsonObject(it)
            }
    }
}
