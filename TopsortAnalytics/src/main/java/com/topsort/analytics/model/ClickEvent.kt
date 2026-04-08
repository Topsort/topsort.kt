package com.topsort.analytics.model

import com.topsort.analytics.core.getListFromJsonArray
import com.topsort.analytics.core.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject

data class ClickEvent(
    val clicks: List<Click>,
) {
    fun toJsonObject(): JSONObject {
        val array = JSONArray()
        clicks.indices.map {
            array.put(it, clicks[it].toJsonObject())
        }
        return JSONObject().put("clicks", array)
    }

    companion object{
        fun fromJson(json : String?) : ClickEvent? {
            if(json == null) return null
            val array = JSONObject(json).getJSONArray("clicks")
            val clicks = (0 until array.length()).map {
                Click.Factory.fromJsonObject(array.getJSONObject(it))
            }

            return ClickEvent(clicks = clicks)
        }
    }
}

data class Click private constructor (

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

    /**
     * Additional attribution entity for halo attribution.
     * Alternative to [additionalAttribution] string when entity information is needed.
     */
    val additionalAttributionEntity: Entity? = null,

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
     * The marketplace's ID for the click
     */
    val id: String,

    /**
     * The device type where the click occurred.
     * Typically "desktop" or "mobile".
     */
    val deviceType: String? = null,

    /**
     * The channel where the click occurred.
     * Typically "onsite", "offsite", or "instore".
     */
    val channel: String? = null,

    /**
     * The page context where the click occurred.
     */
    val page: Page? = null,

    /**
     * The type of click action.
     * Typically "product", "like", or "add-to-cart".
     */
    val clickType: String? = null,
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
            .put("additionalAttribution", additionalAttribution)
            .apply {
                additionalAttributionEntity?.let {
                    put("additionalAttribution", it.toJsonObject())
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
                clickType?.let { put("clickType", it) }
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
            clickType: String? = null,
        ): Click {
            return Click(
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
                clickType = clickType,
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
            clickType: String? = null,
        ): Click {
            return Click(
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
                clickType = clickType,
            )
        }

        fun fromJsonObject(json: JSONObject): Click {
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

            return Click(
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
                clickType = json.getStringOrNull("clickType"),
            )
        }

        fun fromJsonArray(array: JSONArray): List<Click> = getListFromJsonArray(
            array
        ) {
            fromJsonObject(it)
        }
    }

    companion object {
        /** Click type constants for convenience */
        const val TYPE_PRODUCT = "product"
        const val TYPE_LIKE = "like"
        const val TYPE_ADD_TO_CART = "add-to-cart"
    }
}

internal data class ClickEventResponse(
    val clickId: String
)
