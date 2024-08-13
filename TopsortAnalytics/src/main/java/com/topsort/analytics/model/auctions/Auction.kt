package com.topsort.analytics.model.auctions

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

    object Factory {

        @JvmOverloads
        fun buildSponsoredListingAuctionProductIds(
            slots: Int,
            ids: List<String>,
            geoTargeting: String? = null,
        ): Auction {
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
            device: Device = Device.mobile,
            geoTargeting: String? = null,
        ): Auction {
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
            device: Device = Device.mobile,
            geoTargeting: String? = null,
        ): Auction {
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
            device: Device = Device.mobile,
            geoTargeting: String? = null,
        ): Auction {
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
            device: Device = Device.mobile,
            geoTargeting: String? = null,
        ): Auction {
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
            device: Device = Device.mobile,
            geoTargeting: String? = null,
        ): Auction {
            return Auction(
                type = "banners",
                slots = slots,
                slotId = slotId,
                searchQuery = keyword,
                device = device,
                geoTargeting = geoTargeting?.let { GeoTargeting(it) },
            )
        }
    }

    data class Products(
        val ids: List<String>,
    )

    data class Category(
        val id: String? = null,
        val ids: List<String>? = null,
        val disjunctions: List<List<String>>? = null,
    )

    data class GeoTargeting(
        val location: String,
    )
}
