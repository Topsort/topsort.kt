package com.topsort.analytics.model.auctions

/**
 * Sealed class representing the configuration for a sponsored listing auction.
 *
 * Use one of the subclasses to specify the auction type, then pass it to
 * [Auction.fromConfig] to build the low-level [Auction] object.
 *
 * @property slots number of ad slots to fill (must be positive)
 * @property geoTargeting optional location string for geo-targeted auctions
 * @property opaqueUserId optional opaque user ID for targeting context
 * @property placementId optional experiment bucket (1-8) for A/B testing
 */
sealed class AuctionConfig(
    val slots: Int,
    val geoTargeting: String? = null,
) {
    /** Optional opaque user ID for targeting context. */
    abstract val opaqueUserId: String?

    /** Optional experiment bucket (1-8) for A/B testing. */
    abstract val placementId: Int?

    init {
        require(slots > 0) { "Number of slots must be positive" }
    }

    protected fun validatePlacementId() {
        placementId?.let {
            require(it in ApiConstants.MIN_PLACEMENT_ID..ApiConstants.MAX_PLACEMENT_ID) {
                "placementId must be between ${ApiConstants.MIN_PLACEMENT_ID} and ${ApiConstants.MAX_PLACEMENT_ID}"
            }
        }
    }

    /**
     * Auction targeting specific product IDs.
     *
     * @property ids list of product IDs to target (must not be empty)
     * @property qualityScores optional quality scores for products (must match ids size if provided)
     */
    data class ProductIds(
        val numSlots: Int,
        val ids: List<String>,
        val geo: String? = null,
        override val opaqueUserId: String? = null,
        override val placementId: Int? = null,
        val qualityScores: List<Double>? = null,
    ) : AuctionConfig(numSlots, geo) {
        init {
            validatePlacementId()
            require(ids.isNotEmpty()) { "Product IDs list cannot be empty" }
            qualityScores?.let {
                require(it.size == ids.size) {
                    "qualityScores size (${it.size}) must match ids size (${ids.size})"
                }
            }
        }
    }

    /**
     * Auction targeting a single category.
     *
     * @property category the category to target (must not be blank)
     */
    data class CategorySingle(
        val numSlots: Int,
        val category: String,
        val geo: String? = null,
        override val opaqueUserId: String? = null,
        override val placementId: Int? = null,
    ) : AuctionConfig(numSlots, geo) {
        init {
            validatePlacementId()
            require(category.isNotBlank()) { "Category cannot be blank" }
        }
    }

    /**
     * Auction targeting multiple categories.
     *
     * @property categories list of categories to target (must not be empty)
     */
    data class CategoryMultiple(
        val numSlots: Int,
        val categories: List<String>,
        val geo: String? = null,
        override val opaqueUserId: String? = null,
        override val placementId: Int? = null,
    ) : AuctionConfig(numSlots, geo) {
        init {
            validatePlacementId()
            require(categories.isNotEmpty()) { "Categories list cannot be empty" }
        }
    }

    /**
     * Auction targeting category disjunctions.
     *
     * @property disjunctions list of category disjunctions (must not be empty)
     */
    data class CategoryDisjunctions(
        val numSlots: Int,
        val disjunctions: List<List<String>>,
        val geo: String? = null,
        override val opaqueUserId: String? = null,
        override val placementId: Int? = null,
    ) : AuctionConfig(numSlots, geo) {
        init {
            validatePlacementId()
            require(disjunctions.isNotEmpty()) { "Disjunctions list cannot be empty" }
        }
    }

    /**
     * Auction targeting a search keyword.
     *
     * @property keyword the keyword to target (must not be blank)
     */
    data class Keyword(
        val numSlots: Int,
        val keyword: String,
        val geo: String? = null,
        override val opaqueUserId: String? = null,
        override val placementId: Int? = null,
    ) : AuctionConfig(numSlots, geo) {
        init {
            validatePlacementId()
            require(keyword.isNotBlank()) { "Keyword cannot be blank" }
        }
    }
}
