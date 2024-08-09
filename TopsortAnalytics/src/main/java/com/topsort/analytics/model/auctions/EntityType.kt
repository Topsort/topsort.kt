package com.topsort.analytics.model.auctions

enum class EntityType {
    PRODUCT,
    VENDOR,
    BRAND,
    URL;

    companion object {
        fun fromValue(value: String): EntityType = when (value) {
            "product" -> PRODUCT
            "vendor" -> VENDOR
            "brand" -> BRAND
            "url" -> URL
            else -> throw IllegalArgumentException("not valid entity type: $value")
        }
    }
}
