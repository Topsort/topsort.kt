package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
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
     * Mutually exclusive with [additionalAttributionEntity].
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
     * The marketplace assigned ID for the order
     */
    val id: String,

    // --- New fields below (added for binary compatibility) ---

    /**
     * Additional attribution entity for halo attribution.
     * Alternative to [additionalAttribution] string when entity information is needed.
     * Mutually exclusive with [additionalAttribution].
     */
    val additionalAttributionEntity: Entity? = null,

    /**
     * The device type where the impression occurred.
     * Use [DEVICE_DESKTOP] or [DEVICE_MOBILE].
     */
    val deviceType: String? = null,

    /**
     * The channel where the impression occurred.
     * Use [CHANNEL_ONSITE], [CHANNEL_OFFSITE], or [CHANNEL_INSTORE].
     */
    val channel: String? = null,

    /**
     * The page context where the impression occurred.
     */
    val page: Page? = null,
) : JsonSerializable {

    init {
        require(!(additionalAttribution != null && additionalAttributionEntity != null)) {
            "additionalAttribution and additionalAttributionEntity are mutually exclusive"
        }
    }
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
                if (additionalAttributionEntity != null) {
                    put("additionalAttribution", additionalAttributionEntity.toJsonObject())
                } else {
                    additionalAttribution?.let { put("additionalAttribution", it) }
                }
            }
            .put("placement", placement.toJsonObject())
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
            .apply {
                deviceType?.let { put("deviceType", it) }
                channel?.let { put("channel", it) }
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
            additionalAttributionEntity: Entity? = null,
            deviceType: String? = null,
            channel: String? = null,
            page: Page? = null,
        ): Impression {
            return Impression(
                resolvedBidId = resolvedBidId,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
                additionalAttributionEntity = additionalAttributionEntity,
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
            additionalAttributionEntity: Entity? = null,
            deviceType: String? = null,
            channel: String? = null,
            page: Page? = null,
        ): Impression {
            return Impression(
                entity = entity,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
                additionalAttributionEntity = additionalAttributionEntity,
                deviceType = deviceType,
                channel = channel,
                page = page,
            )
        }

        fun fromJsonObject(json: JSONObject): Impression {
            val resolvedBidId = json.getStringOrNull("resolvedBidId")

            // Handle additionalAttribution which can be string or entity object
            val additionalAttrObj = json.opt("additionalAttribution")
            val additionalAttribution: String?
            val additionalAttributionEntity: Entity?
            when (additionalAttrObj) {
                is String -> {
                    additionalAttribution = additionalAttrObj
                    additionalAttributionEntity = null
                }
                is JSONObject -> {
                    additionalAttribution = null
                    additionalAttributionEntity = Entity.fromJsonObject(additionalAttrObj)
                }
                else -> {
                    additionalAttribution = null
                    additionalAttributionEntity = null
                }
            }

            return Impression(
                resolvedBidId = resolvedBidId,
                entity = if (resolvedBidId == null) {
                    Entity.fromJsonObject(json.getJSONObject("entity"))
                } else null,
                additionalAttribution = additionalAttribution,
                additionalAttributionEntity = additionalAttributionEntity,
                placement = Placement.fromJsonObject(json.getJSONObject("placement")),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
                deviceType = json.getStringOrNull("deviceType"),
                channel = json.getStringOrNull("channel"),
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

    companion object {
        /** Device type constants */
        const val DEVICE_DESKTOP = "desktop"
        const val DEVICE_MOBILE = "mobile"

        /** Channel constants */
        const val CHANNEL_ONSITE = "onsite"
        const val CHANNEL_OFFSITE = "offsite"
        const val CHANNEL_INSTORE = "instore"
    }
}

internal data class ImpressionEventResponse(
    val impressions: List<Impression>
)
