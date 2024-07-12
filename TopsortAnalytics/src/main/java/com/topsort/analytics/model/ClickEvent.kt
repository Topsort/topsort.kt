package com.topsort.analytics.model

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
) {
    fun toJsonObject(): JSONObject {
        return JSONObject()
            .let {
                if (resolvedBidId == null) {
                    it.put("entity", entity?.toJsonObject())
                } else {
                    it.put("resolvedBidId", resolvedBidId)
                }
            }
            .put("additionalAttribution", additionalAttribution)
            .put("placement", placement.toJsonObject())
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
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
        ): Click {
            return Click(
                resolvedBidId = resolvedBidId,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
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
        ): Click {
            return Click(
                entity = entity,
                placement = placement,
                occurredAt = occurredAt,
                opaqueUserId = opaqueUserId,
                id = id,
                additionalAttribution = additionalAttribution,
            )
        }

        fun fromJsonObject(json: JSONObject): Click {
            val resolvedBidId = json.getStringOrNull("resolvedBidId")
            return Click(
                resolvedBidId = resolvedBidId,
                entity = if (resolvedBidId == null) {
                    Entity.fromJsonObject(json.getJSONObject("entity"))
                } else null,
                additionalAttribution = json.getStringOrNull("additionalAttribution"),
                placement = Placement.fromJsonObject(json.getJSONObject("placement")),
                occurredAt = json.getString("occurredAt"),
                opaqueUserId = json.getString("opaqueUserId"),
                id = json.getString("id"),
            )
        }
    }
}

internal data class ClickEventResponse(
    val clickId: String
)
