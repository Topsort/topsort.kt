package com.topsort.analytics.model

import org.json.JSONObject

data class ClickEvent(
    private val eventType: EventType = EventType.Click,
    val clicks: List<Click>,
)

data class Click(

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
            .put("resolvedBidId", resolvedBidId)
            .put("entity", entity?.toJsonObject())
            .put("additionalAttribution", additionalAttribution)
            .put("placement", placement.toJsonObject())
            .put("occurredAt", occurredAt)
            .put("opaqueUserId", opaqueUserId)
            .put("id", id)
    }
}

internal data class ClickEventResponse(
    val clickId: String
)
