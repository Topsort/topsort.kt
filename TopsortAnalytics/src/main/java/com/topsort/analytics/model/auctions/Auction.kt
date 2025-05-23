package com.topsort.analytics.model.auctions

import org.json.JSONObject

data class Auction private constructor(
    val type: String,
    val slots: Int,
    val products: Products? = null,
    val category: Category? = null,
    val searchQuery: String? = null,
    val geoTargeting: GeoTargeting? = null,
    val slotId: String? = null,
    val device: Device? = null,
) {

    fun toJsonObject(): JSONObject {
        try {
            val builder = JSONObject()

            with(builder) {
                put("type", type)
                put("slots", slots)
                if (products != null) {
                    put("products", JSONObject.wrap(products))
                }
                if (category != null) {
                    put("category", category.toJsonObject())
                }
                if (searchQuery != null) {
                    put("searchQuery", searchQuery)
                }
                if (geoTargeting != null) {
                    put("geoTargeting", JSONObject.wrap(geoTargeting))
                }
                if (slotId != null) {
                    put("slotId", slotId)
                }
                if (device != null) {
                    put("device", device.name.lowercase())
                }
            }

            return builder
        } catch (e: Exception) {
            throw AuctionError.SerializationError
        }
    }

    object Factory {

        @JvmOverloads
        fun buildSponsoredListingAuctionProductIds(
            slots: Int,
            ids: List<String>,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            require(ids.isNotEmpty()) { "Product IDs list cannot be empty" }
            
            return Auction(
                type = "listings",
                slots = slots,
                products = Products(ids),
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildSponsoredListingAuctionCategorySingle(
            slots: Int,
            category: String,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            require(!category.isBlank()) { "Category cannot be blank" }
            
            return Auction(
                type = "listings",
                slots = slots,
                category = Category(id = category),
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildSponsoredListingAuctionCategoryMultiple(
            slots: Int,
            categories: List<String>,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            require(categories.isNotEmpty()) { "Categories list cannot be empty" }
            
            return Auction(
                type = "listings",
                slots = slots,
                category = Category(ids = categories),
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildSponsoredListingAuctionCategoryDisjunctions(
            slots: Int,
            disjunctions: List<List<String>>,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            require(disjunctions.isNotEmpty()) { "Disjunctions list cannot be empty" }
            
            return Auction(
                type = "listings",
                slots = slots,
                category = Category(disjunctions = disjunctions),
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildSponsoredListingAuctionKeyword(
            slots: Int,
            keyword: String,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            require(!keyword.isBlank()) { "Keyword cannot be blank" }
            
            return Auction(
                type = "listings",
                slots = slots,
                searchQuery = keyword,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildBannerAuctionLandingPage(
            slots: Int,
            slotId: String,
            ids: List<String>,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)
            require(ids.isNotEmpty()) { "Product IDs list cannot be empty" }
            
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                products = Products(ids),
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategorySingle(
            slots: Int,
            slotId: String,
            category: String,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)
            require(!category.isBlank()) { "Category cannot be blank" }
            
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                category = Category(id = category),
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategoryMultiple(
            slots: Int,
            slotId: String,
            categories: List<String>,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)
            require(categories.isNotEmpty()) { "Categories list cannot be empty" }
            
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                category = Category(ids = categories),
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategoryDisjunctions(
            slots: Int,
            slotId: String,
            disjunctions: List<List<String>>,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)
            require(disjunctions.isNotEmpty()) { "Disjunctions list cannot be empty" }
            
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                category = Category(disjunctions = disjunctions),
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }

        @JvmOverloads
        fun buildBannerAuctionKeywords(
            slots: Int,
            slotId: String,
            keyword: String,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)
            require(!keyword.isBlank()) { "Keyword cannot be blank" }
            
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                searchQuery = keyword,
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }
        
        private fun validateSlots(slots: Int) {
            require(slots > 0) { "Number of slots must be positive" }
        }
        
        private fun validateSlotId(slotId: String) {
            require(!slotId.isBlank()) { "Slot ID cannot be blank" }
        }
    }

    data class Products(
        val ids: List<String>,
    )

    data class Category(
        val id: String? = null,
        val ids: List<String>? = null,
        val disjunctions: List<List<String>>? = null,
    ) {
        fun toJsonObject(): JSONObject {
            try {
                val builder = JSONObject()
                with(builder) {
                    if (id != null) {
                        put("id", id)
                    }
                    if (ids != null) {
                        put("ids", JSONObject.wrap(ids))
                    }
                    if (disjunctions != null) {
                        put("disjunctions", JSONObject.wrap(disjunctions))
                    }
                }
                return builder
            } catch (e: Exception) {
                throw AuctionError.SerializationError
            }
        }
    }

    data class GeoTargeting(
        val location: String,
    )
}
