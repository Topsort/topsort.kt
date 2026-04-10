package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import com.topsort.analytics.model.auctions.Device
import org.json.JSONArray
import org.json.JSONObject

data class ImpressionEvent (
    val impressions: List<Impression>,
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        impressions.indices.map {
            array.put(it, impressions[it].toJsonObject())
        }
        return JSONObject().put("impressions", array)
    }

    companion object {
        fun fromJson(json: String?): ImpressionEvent? {
            if (json == null) return null
            val array = JSONObject(json).getJSONArray("impressions")
            val impressions = Impression.Factory.fromJsonArray(array)

            return ImpressionEvent(impressions = impressions)
        }
    }
}

data class Impression private constructor(

    /**
     * Required for promoted products. Must be the ID for the auction the product won.
     */
    val resolvedBidId: String? = null,

    /**
     * Entity is meant for reporting organic events, not sponsored or promoted products.
     * It refers to the object involved in the organic interaction.
     */
    val entity: Entity? = null,

    /**
     * Extra attribution if desired by the marketplace.
     * When using this field, the resolvedBidId must also exist in the event body.
     */
    val additionalAttribution: String? = null,

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
     * The marketplace's unique ID for the impression.
     */
    val id: String,

    /**
     * The device type where the impression occurred.
     */
    val deviceType: Device? = null,

    /**
     * The channel where the impression occurred.
     */
    val channel: Channel? = null,

    /**
     * The page context where the impression occurred.
     */
    val page: Page? = null,
) : JsonSerializable {
    override fun toJsonObject(): JSONObject {
        return JSONObject()
            .let {
                if (resolvedBidId == null) {
                    it.put("entity", entity?.toJsonObject())
                } else {
                    it.put("resolvedBidId", resolvedBidId)
                }
            }
            .apply {
                // Consistent null handling: omit keys when value is null
                additionalAttribution?.let { put("additionalAttribution", it) }
            }
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
        fun buildPromoted(
            resolvedBidId: String,
            placement: Placement,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            additionalAttribution: String? = null,
            deviceType: Device? = null,
            channel: Channel? = null,
            page: Page? = null,
        ): Impression {
            return Impression(
                resolvedBidId = resolvedBidId,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        @JvmOverloads
        fun buildOrganic(
            entity: Entity,
            placement: Placement,
            occurredAt: String,
            opaqueUserId: String,
            id: String,
            additionalAttribution: String? = null,
            deviceType: Device? = null,
            channel: Channel? = null,
            page: Page? = null,
        ): Impression {
            return Impression(
                entity = entity,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        fun fromJsonObject(json: JSONObject): Impression {
            val resolvedBidId = json.getStringOrNull("resolvedBidId")
            return Impression(
                resolvedBidId = resolvedBidId,
                entity = if (resolvedBidId == null) {
                    Entity.fromJsonObject(json.getJSONObject("entity"))
                } else null,
                additionalAttribution = json.getStringOrNull("additionalAttribution"),
                placement = Placement.fromJsonObject(json.getJSONObject("placement")),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                deviceType = Device.fromValue(json.getStringOrNull("deviceType")),
                channel = Channel.fromValue(json.getStringOrNull("channel")),
                page = json.optJSONObject("page")?.let { Page.Factory.fromJsonObject(it) },
            )
        }

        fun fromJsonArray(array: JSONArray): List<Impression> =
            getListFromJsonArray(
                array
            ) {
                fromJsonObject(it)
            }
    }
}

internal data class ImpressionEventResponse(
    val impressions: List<Impression>
)
