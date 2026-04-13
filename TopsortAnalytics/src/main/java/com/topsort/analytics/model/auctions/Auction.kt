package com.topsort.analytics.model.auctions

import org.json.JSONArray
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
    /**
     * The opaque user ID for targeting context.
     */
    val opaqueUserId: String? = null,
    /**
     * Experiment bucket (1-8) for A/B testing.
     */
    val placementId: Int? = null,
) {
    init {
        require(slots > 0) { "Number of slots must be positive" }
        placementId?.let {
            require(it in ApiConstants.MIN_PLACEMENT_ID..ApiConstants.MAX_PLACEMENT_ID) {
                "placementId must be between ${ApiConstants.MIN_PLACEMENT_ID} and ${ApiConstants.MAX_PLACEMENT_ID}"
            }
        }
    }

    fun toJsonObject(): JSONObject {
        try {
            val builder = JSONObject()

            with(builder) {
                put("type", type)
                put("slots", slots)
                if (products != null) {
                    put("products", products.toJsonObject())
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
                    put("device", device.value)
                }
                if (opaqueUserId != null) {
                    put("opaqueUserId", opaqueUserId)
                }
                if (placementId != null) {
                    put("placementId", placementId)
                }
            }

            return builder
        } catch (e: Exception) {
            throw AuctionError.SerializationError
        }
    }

    companion object {
        /**
         * Builds an [Auction] from an [AuctionConfig] sealed class instance.
         *
         * This is the preferred way to create sponsored listing auctions:
         * ```
         * val config = AuctionConfig.ProductIds(numSlots = 1, ids = listOf("p1", "p2"))
         * val auction = Auction.fromConfig(config)
         * ```
         */
        fun fromConfig(config: AuctionConfig): Auction {
            val validatedPlacementId = config.validatedPlacementId()
            return when (config) {
                is AuctionConfig.ProductIds -> Auction(
                    type = "listings",
                    slots = config.slots,
                    products = Products(config.ids, config.validatedQualityScores()),
                    geoTargeting = config.geoTargeting?.let { GeoTargeting(it) },
                    opaqueUserId = config.opaqueUserId,
                    placementId = validatedPlacementId,
                )
                is AuctionConfig.CategorySingle -> Auction(
                    type = "listings",
                    slots = config.slots,
                    category = Category(id = config.category),
                    geoTargeting = config.geoTargeting?.let { GeoTargeting(it) },
                    opaqueUserId = config.opaqueUserId,
                    placementId = validatedPlacementId,
                )
                is AuctionConfig.CategoryMultiple -> Auction(
                    type = "listings",
                    slots = config.slots,
                    category = Category(ids = config.categories),
                    geoTargeting = config.geoTargeting?.let { GeoTargeting(it) },
                    opaqueUserId = config.opaqueUserId,
                    placementId = validatedPlacementId,
                )
                is AuctionConfig.CategoryDisjunctions -> Auction(
                    type = "listings",
                    slots = config.slots,
                    category = Category(disjunctions = config.disjunctions),
                    geoTargeting = config.geoTargeting?.let { GeoTargeting(it) },
                    opaqueUserId = config.opaqueUserId,
                    placementId = validatedPlacementId,
                )
                is AuctionConfig.Keyword -> Auction(
                    type = "listings",
                    slots = config.slots,
                    searchQuery = config.keyword,
                    geoTargeting = config.geoTargeting?.let { GeoTargeting(it) },
                    opaqueUserId = config.opaqueUserId,
                    placementId = validatedPlacementId,
                )
            }
        }
    }

    object Factory {

        @Deprecated(
            "Use AuctionConfig.ProductIds with Auction.fromConfig() instead",
            ReplaceWith("Auction.fromConfig(AuctionConfig.ProductIds(slots, ids, geoTargeting))")
        )
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

        @Deprecated(
            "Use AuctionConfig.CategorySingle with Auction.fromConfig() instead",
            ReplaceWith("Auction.fromConfig(AuctionConfig.CategorySingle(slots, category, geoTargeting))")
        )
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

        @Deprecated(
            "Use AuctionConfig.CategoryMultiple with Auction.fromConfig() instead",
            ReplaceWith("Auction.fromConfig(AuctionConfig.CategoryMultiple(slots, categories, geoTargeting))")
        )
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

        @Deprecated(
            "Use AuctionConfig.CategoryDisjunctions with Auction.fromConfig() instead",
            ReplaceWith("Auction.fromConfig(AuctionConfig.CategoryDisjunctions(slots, disjunctions, geoTargeting))")
        )
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

        @Deprecated(
            "Use AuctionConfig.Keyword with Auction.fromConfig() instead",
            ReplaceWith("Auction.fromConfig(AuctionConfig.Keyword(slots, keyword, geoTargeting))")
        )
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
            opaqueUserId: String? = null,
            placementId: Int? = null,
            qualityScores: List<Double>? = null,
        ): Auction {
            validateSlots(slots)
            validateSlotId(slotId)

            val validScores = validatedQualityScores(ids, qualityScores)
            val products = if (ids.isNotEmpty()) Products(ids, validScores) else null

            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                products = products,
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
                opaqueUserId = opaqueUserId,
                placementId = validatedPlacementId(placementId),
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategorySingle(
            slots: Int,
            slotId: String,
            category: String,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
            opaqueUserId: String? = null,
            placementId: Int? = null,
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
                opaqueUserId = opaqueUserId,
                placementId = validatedPlacementId(placementId),
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategoryMultiple(
            slots: Int,
            slotId: String,
            categories: List<String>,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
            opaqueUserId: String? = null,
            placementId: Int? = null,
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
                opaqueUserId = opaqueUserId,
                placementId = validatedPlacementId(placementId),
            )
        }

        @JvmOverloads
        fun buildBannerAuctionCategoryDisjunctions(
            slots: Int,
            slotId: String,
            disjunctions: List<List<String>>,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
            opaqueUserId: String? = null,
            placementId: Int? = null,
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
                opaqueUserId = opaqueUserId,
                placementId = validatedPlacementId(placementId),
            )
        }

        @JvmOverloads
        fun buildBannerAuctionKeywords(
            slots: Int,
            slotId: String,
            keyword: String,
            device: Device = Device.MOBILE,
            geoTargeting: String? = null,
            opaqueUserId: String? = null,
            placementId: Int? = null,
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
                opaqueUserId = opaqueUserId,
                placementId = validatedPlacementId(placementId),
            )
        }

        private fun validateSlots(slots: Int) {
            require(slots > 0) { "Number of slots must be positive" }
        }

        private fun validateSlotId(slotId: String) {
            require(!slotId.isBlank()) { "Slot ID cannot be blank" }
        }

        /**
         * Returns placementId if valid (1-8), null otherwise.
         * Follows "never crash host app" principle.
         */
        private fun validatedPlacementId(placementId: Int?): Int? {
            return placementId?.takeIf {
                it in ApiConstants.MIN_PLACEMENT_ID..ApiConstants.MAX_PLACEMENT_ID
            }
        }

        /**
         * Returns qualityScores if size matches ids, null otherwise.
         * Follows "never crash host app" principle.
         */
        private fun validatedQualityScores(ids: List<String>, qualityScores: List<Double>?): List<Double>? {
            return qualityScores?.takeIf { it.size == ids.size }
        }
    }

    data class Products(
        val ids: List<String>,
        /**
         * Quality scores for the products. If size doesn't match [ids], scores are
         * silently ignored during serialization (graceful degradation).
         */
        val qualityScores: List<Double>? = null,
    ) {
        /**
         * Returns qualityScores only if size matches ids, null otherwise.
         */
        private fun validatedQualityScores(): List<Double>? {
            return qualityScores?.takeIf { it.size == ids.size }
        }

        fun toJsonObject(): JSONObject {
            return JSONObject().apply {
                put("ids", JSONArray(ids))
                validatedQualityScores()?.let { put("qualityScores", JSONArray(it)) }
            }
        }
    }

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
