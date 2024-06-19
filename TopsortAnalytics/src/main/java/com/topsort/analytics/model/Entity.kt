package com.topsort.analytics.model

enum class EntityType {

    Product,
    Vendor,
}

data class Entity(

    /**
     * The marketplace's ID of the entity associated with the interaction
     */
    val id: Int,

    /**
     * The type of entity associated with the interaction.
     */
    val type: EntityType,
)