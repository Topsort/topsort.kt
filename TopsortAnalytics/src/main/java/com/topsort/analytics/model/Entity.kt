package com.topsort.analytics.model

enum class EntityType {

    Product,
    Vendor,
}

data class Entity(
    val id: Session,
    val type: EntityType,
)