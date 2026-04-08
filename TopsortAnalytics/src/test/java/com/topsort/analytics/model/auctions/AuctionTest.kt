package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

internal class AuctionTest {

    // Tests for fromConfig with different AuctionConfig types

    @Test
    fun `fromConfig ProductIds creates listings auction`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 3,
            ids = listOf("p1", "p2", "p3")
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.slots).isEqualTo(3)
        assertThat(auction.products).isNotNull
        assertThat(auction.products!!.ids).containsExactly("p1", "p2", "p3")
        assertThat(auction.category).isNull()
        assertThat(auction.searchQuery).isNull()
    }

    @Test
    fun `fromConfig CategorySingle creates listings auction with category id`() {
        val config = AuctionConfig.CategorySingle(
            numSlots = 2,
            category = "electronics"
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.slots).isEqualTo(2)
        assertThat(auction.category).isNotNull
        assertThat(auction.category!!.id).isEqualTo("electronics")
        assertThat(auction.category!!.ids).isNull()
        assertThat(auction.products).isNull()
    }

    @Test
    fun `fromConfig CategoryMultiple creates listings auction with category ids`() {
        val config = AuctionConfig.CategoryMultiple(
            numSlots = 2,
            categories = listOf("electronics", "phones", "accessories")
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.category).isNotNull
        assertThat(auction.category!!.ids).containsExactly("electronics", "phones", "accessories")
        assertThat(auction.category!!.id).isNull()
    }

    @Test
    fun `fromConfig CategoryDisjunctions creates listings auction with disjunctions`() {
        val config = AuctionConfig.CategoryDisjunctions(
            numSlots = 1,
            disjunctions = listOf(
                listOf("cat1", "cat2"),
                listOf("cat3")
            )
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.category).isNotNull
        assertThat(auction.category!!.disjunctions).hasSize(2)
        assertThat(auction.category!!.disjunctions!![0]).containsExactly("cat1", "cat2")
    }

    @Test
    fun `fromConfig Keyword creates listings auction with searchQuery`() {
        val config = AuctionConfig.Keyword(
            numSlots = 5,
            keyword = "running shoes"
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.searchQuery).isEqualTo("running shoes")
        assertThat(auction.products).isNull()
        assertThat(auction.category).isNull()
    }

    @Test
    fun `fromConfig with geoTargeting includes location`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            geo = "US-NY"
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.geoTargeting).isNotNull
        assertThat(auction.geoTargeting!!.location).isEqualTo("US-NY")
    }

    // Tests for Factory methods (deprecated but still need coverage)

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionProductIds creates valid auction`() {
        val auction = Auction.Factory.buildSponsoredListingAuctionProductIds(
            slots = 2,
            ids = listOf("prod1", "prod2")
        )

        assertThat(auction.type).isEqualTo("listings")
        assertThat(auction.slots).isEqualTo(2)
        assertThat(auction.products!!.ids).containsExactly("prod1", "prod2")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionProductIds throws on empty ids`() {
        assertThatThrownBy {
            Auction.Factory.buildSponsoredListingAuctionProductIds(
                slots = 1,
                ids = emptyList()
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Product IDs list cannot be empty")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionProductIds throws on non-positive slots`() {
        assertThatThrownBy {
            Auction.Factory.buildSponsoredListingAuctionProductIds(
                slots = 0,
                ids = listOf("p1")
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Number of slots must be positive")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionCategorySingle throws on blank category`() {
        assertThatThrownBy {
            Auction.Factory.buildSponsoredListingAuctionCategorySingle(
                slots = 1,
                category = "   "
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Category cannot be blank")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionCategoryMultiple throws on empty categories`() {
        assertThatThrownBy {
            Auction.Factory.buildSponsoredListingAuctionCategoryMultiple(
                slots = 1,
                categories = emptyList()
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Categories list cannot be empty")
    }

    @Test
    @Suppress("DEPRECATION")
    fun `factory buildSponsoredListingAuctionKeyword throws on blank keyword`() {
        assertThatThrownBy {
            Auction.Factory.buildSponsoredListingAuctionKeyword(
                slots = 1,
                keyword = ""
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Keyword cannot be blank")
    }

    // Banner auction factory tests

    @Test
    fun `factory buildBannerAuctionLandingPage creates banners auction`() {
        val auction = Auction.Factory.buildBannerAuctionLandingPage(
            slots = 1,
            slotId = "slot-123",
            ids = listOf("p1", "p2"),
            device = Device.DESKTOP
        )

        assertThat(auction.type).isEqualTo("banners")
        assertThat(auction.slotId).isEqualTo("slot-123")
        assertThat(auction.device).isEqualTo(Device.DESKTOP)
        assertThat(auction.products!!.ids).containsExactly("p1", "p2")
    }

    @Test
    fun `factory buildBannerAuctionLandingPage with empty ids creates auction without products`() {
        val auction = Auction.Factory.buildBannerAuctionLandingPage(
            slots = 1,
            slotId = "slot-123",
            ids = emptyList()
        )

        assertThat(auction.products).isNull()
    }

    @Test
    fun `factory buildBannerAuctionLandingPage throws on blank slotId`() {
        assertThatThrownBy {
            Auction.Factory.buildBannerAuctionLandingPage(
                slots = 1,
                slotId = "  ",
                ids = listOf("p1")
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Slot ID cannot be blank")
    }

    @Test
    fun `factory buildBannerAuctionCategorySingle creates banners auction`() {
        val auction = Auction.Factory.buildBannerAuctionCategorySingle(
            slots = 1,
            slotId = "slot-1",
            category = "electronics"
        )

        assertThat(auction.type).isEqualTo("banners")
        assertThat(auction.category!!.id).isEqualTo("electronics")
    }

    @Test
    fun `factory buildBannerAuctionKeywords creates banners auction with searchQuery`() {
        val auction = Auction.Factory.buildBannerAuctionKeywords(
            slots = 1,
            slotId = "slot-1",
            keyword = "laptop"
        )

        assertThat(auction.type).isEqualTo("banners")
        assertThat(auction.searchQuery).isEqualTo("laptop")
    }

    // Serialization tests

    @Test
    fun `toJsonObject includes all required fields`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 2,
            ids = listOf("p1", "p2")
        )
        val auction = Auction.fromConfig(config)

        val json = auction.toJsonObject()

        assertThat(json.getString("type")).isEqualTo("listings")
        assertThat(json.getInt("slots")).isEqualTo(2)
        assertThat(json.has("products")).isTrue()
    }

    @Test
    fun `toJsonObject includes geoTargeting when present`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            geo = "BR"
        )
        val auction = Auction.fromConfig(config)

        val json = auction.toJsonObject()

        assertThat(json.has("geoTargeting")).isTrue()
        assertThat(json.getJSONObject("geoTargeting").getString("location")).isEqualTo("BR")
    }

    @Test
    fun `toJsonObject includes device for banner auctions`() {
        val auction = Auction.Factory.buildBannerAuctionLandingPage(
            slots = 1,
            slotId = "slot-1",
            ids = listOf("p1"),
            device = Device.MOBILE
        )

        val json = auction.toJsonObject()

        assertThat(json.getString("device")).isEqualTo("mobile")
    }

    @Test
    fun `toJsonObject omits null fields`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1")
        )
        val auction = Auction.fromConfig(config)

        val json = auction.toJsonObject()

        assertThat(json.has("category")).isFalse()
        assertThat(json.has("searchQuery")).isFalse()
        assertThat(json.has("geoTargeting")).isFalse()
        assertThat(json.has("slotId")).isFalse()
        assertThat(json.has("device")).isFalse()
    }

    // Category serialization tests

    @Test
    fun `category toJsonObject with single id`() {
        val category = Auction.Category(id = "cat-1")

        val json = category.toJsonObject()

        assertThat(json.getString("id")).isEqualTo("cat-1")
        assertThat(json.has("ids")).isFalse()
        assertThat(json.has("disjunctions")).isFalse()
    }

    @Test
    fun `category toJsonObject with multiple ids`() {
        val category = Auction.Category(ids = listOf("cat-1", "cat-2"))

        val json = category.toJsonObject()

        assertThat(json.has("id")).isFalse()
        val idsArray = json.getJSONArray("ids")
        assertThat(idsArray.length()).isEqualTo(2)
    }

    @Test
    fun `category toJsonObject with disjunctions`() {
        val category = Auction.Category(
            disjunctions = listOf(
                listOf("a", "b"),
                listOf("c", "d", "e")
            )
        )

        val json = category.toJsonObject()

        val disjunctions = json.getJSONArray("disjunctions")
        assertThat(disjunctions.length()).isEqualTo(2)
        assertThat(disjunctions.getJSONArray(0).length()).isEqualTo(2)
        assertThat(disjunctions.getJSONArray(1).length()).isEqualTo(3)
    }

    // AuctionConfig validation tests

    @Test
    fun `auctionConfig ProductIds throws on empty ids`() {
        assertThatThrownBy {
            AuctionConfig.ProductIds(numSlots = 1, ids = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Product IDs list cannot be empty")
    }

    @Test
    fun `auctionConfig CategorySingle throws on blank category`() {
        assertThatThrownBy {
            AuctionConfig.CategorySingle(numSlots = 1, category = "")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Category cannot be blank")
    }

    @Test
    fun `auctionConfig CategoryMultiple throws on empty categories`() {
        assertThatThrownBy {
            AuctionConfig.CategoryMultiple(numSlots = 1, categories = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Categories list cannot be empty")
    }

    @Test
    fun `auctionConfig CategoryDisjunctions throws on empty disjunctions`() {
        assertThatThrownBy {
            AuctionConfig.CategoryDisjunctions(numSlots = 1, disjunctions = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Disjunctions list cannot be empty")
    }

    @Test
    fun `auctionConfig Keyword throws on blank keyword`() {
        assertThatThrownBy {
            AuctionConfig.Keyword(numSlots = 1, keyword = "  ")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Keyword cannot be blank")
    }

    @Test
    fun `auctionConfig throws on non-positive slots`() {
        assertThatThrownBy {
            AuctionConfig.ProductIds(numSlots = 0, ids = listOf("p1"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Number of slots must be positive")

        assertThatThrownBy {
            AuctionConfig.ProductIds(numSlots = -1, ids = listOf("p1"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Number of slots must be positive")
    }
}
