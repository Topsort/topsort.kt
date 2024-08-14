package com.topsort.analytics.model

import org.json.JSONObject

enum class EntityType {

    PRODUCT,
    VENDOR,
}

data class Entity(

    /**
     * The marketplace's ID of the entity associated with the interaction
     */
    val id: String,

    /**
     * The type of entity associated with the interaction.
     */
    val type: EntityType,
) {
    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("type", type.name.lowercase())
    }

    companion object{
        fun fromJsonObject(json: JSONObject): Entity {
            return Entity(
                id = json.getString("id"),
                type = EntityType.valueOf(json.getString("type").uppercase())
            )
        }
    }
}
