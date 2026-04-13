package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

internal class AuctionFieldsTest {

    // Tests for opaqueUserId and placementId in Auction

    @Test
    fun `auction fromConfig ProductIds with opaqueUserId and placementId`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 2,
            ids = listOf("p1", "p2"),
            opaqueUserId = "user-123",
            placementId = 5
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.opaqueUserId).isEqualTo("user-123")
        assertThat(auction.placementId).isEqualTo(5)
    }

    @Test
    fun `auction fromConfig CategorySingle with opaqueUserId and placementId`() {
        val config = AuctionConfig.CategorySingle(
            numSlots = 1,
            category = "electronics",
            opaqueUserId = "user-456",
            placementId = 3
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.opaqueUserId).isEqualTo("user-456")
        assertThat(auction.placementId).isEqualTo(3)
    }

    @Test
    fun `auction toJsonObject includes opaqueUserId and placementId when present`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            opaqueUserId = "user-789",
            placementId = 7
        )

        val auction = Auction.fromConfig(config)
        val json = auction.toJsonObject()

        assertThat(json.getString("opaqueUserId")).isEqualTo("user-789")
        assertThat(json.getInt("placementId")).isEqualTo(7)
    }

    @Test
    fun `auction toJsonObject omits opaqueUserId and placementId when null`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1")
        )

        val auction = Auction.fromConfig(config)
        val json = auction.toJsonObject()

        assertThat(json.has("opaqueUserId")).isFalse()
        assertThat(json.has("placementId")).isFalse()
    }

    @Test
    fun `auctionConfig placementId outside 1-8 is silently ignored`() {
        // Invalid values should be accepted but silently ignored during serialization
        val configWithZero = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            placementId = 0
        )
        val auctionWithZero = Auction.fromConfig(configWithZero)
        assertThat(auctionWithZero.placementId).isNull()

        val configWithNine = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            placementId = 9
        )
        val auctionWithNine = Auction.fromConfig(configWithNine)
        assertThat(auctionWithNine.placementId).isNull()

        val configWithNegative = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1"),
            placementId = -1
        )
        val auctionWithNegative = Auction.fromConfig(configWithNegative)
        assertThat(auctionWithNegative.placementId).isNull()
    }

    @Test
    fun `auctionConfig placementId valid values 1 to 8`() {
        for (i in 1..8) {
            val config = AuctionConfig.ProductIds(
                numSlots = 1,
                ids = listOf("p1"),
                placementId = i
            )
            assertThat(config.placementId).isEqualTo(i)
        }
    }

    // Tests for qualityScores in Products

    @Test
    fun `auction fromConfig ProductIds with qualityScores`() {
        val config = AuctionConfig.ProductIds(
            numSlots = 2,
            ids = listOf("p1", "p2", "p3"),
            qualityScores = listOf(0.8, 0.6, 0.9)
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.products).isNotNull
        assertThat(auction.products!!.qualityScores).containsExactly(0.8, 0.6, 0.9)
    }

    @Test
    fun `products toJsonObject includes qualityScores when present`() {
        val products = Auction.Products(
            ids = listOf("p1", "p2"),
            qualityScores = listOf(0.5, 0.7)
        )

        val json = products.toJsonObject()

        val scoresArray = json.getJSONArray("qualityScores")
        assertThat(scoresArray.length()).isEqualTo(2)
        assertThat(scoresArray.getDouble(0)).isEqualTo(0.5)
        assertThat(scoresArray.getDouble(1)).isEqualTo(0.7)
    }

    @Test
    fun `products toJsonObject omits qualityScores when null`() {
        val products = Auction.Products(ids = listOf("p1", "p2"))

        val json = products.toJsonObject()

        assertThat(json.has("qualityScores")).isFalse()
    }

    @Test
    fun `products qualityScores size mismatch is silently ignored in serialization`() {
        // Mismatched sizes should be accepted but scores silently omitted in JSON
        val products = Auction.Products(
            ids = listOf("p1", "p2", "p3"),
            qualityScores = listOf(0.5, 0.7) // size 2 vs ids size 3
        )

        // qualityScores is stored
        assertThat(products.qualityScores).containsExactly(0.5, 0.7)

        // But omitted from JSON because sizes don't match
        val json = products.toJsonObject()
        assertThat(json.has("qualityScores")).isFalse()
    }

    @Test
    fun `auctionConfig qualityScores size mismatch is silently ignored`() {
        // Mismatched sizes should be accepted but scores silently omitted
        val config = AuctionConfig.ProductIds(
            numSlots = 1,
            ids = listOf("p1", "p2"),
            qualityScores = listOf(0.5) // size 1 vs ids size 2
        )

        val auction = Auction.fromConfig(config)

        // qualityScores should be null because sizes don't match
        assertThat(auction.products!!.qualityScores).isNull()
    }

    // Tests for Factory methods

    @Test
    fun `deprecated factory methods do not support new fields - use AuctionConfig instead`() {
        // Deprecated methods only have original params (slots, ids, geoTargeting)
        // Users should migrate to AuctionConfig for opaqueUserId, placementId, qualityScores
        @Suppress("DEPRECATION")
        val auction = Auction.Factory.buildSponsoredListingAuctionProductIds(
            slots = 2,
            ids = listOf("p1", "p2"),
            geoTargeting = "US"
        )

        // New fields are not available on deprecated methods
        assertThat(auction.opaqueUserId).isNull()
        assertThat(auction.placementId).isNull()
        assertThat(auction.products!!.qualityScores).isNull()
    }

    @Test
    fun `factory buildBannerAuctionLandingPage with new fields`() {
        val auction = Auction.Factory.buildBannerAuctionLandingPage(
            slots = 1,
            slotId = "slot-1",
            ids = listOf("p1"),
            opaqueUserId = "user-456",
            placementId = 2,
            qualityScores = listOf(0.7)
        )

        assertThat(auction.opaqueUserId).isEqualTo("user-456")
        assertThat(auction.placementId).isEqualTo(2)
        assertThat(auction.products!!.qualityScores).containsExactly(0.7)
    }

    // Tests for AuctionConfig with all new fields

    @Test
    fun `auctionConfig CategoryMultiple with all new fields`() {
        val config = AuctionConfig.CategoryMultiple(
            numSlots = 3,
            categories = listOf("cat1", "cat2"),
            geo = "US",
            opaqueUserId = "user-123",
            placementId = 6
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.opaqueUserId).isEqualTo("user-123")
        assertThat(auction.placementId).isEqualTo(6)
        assertThat(auction.geoTargeting?.location).isEqualTo("US")
    }

    @Test
    fun `auctionConfig Keyword with opaqueUserId and placementId`() {
        val config = AuctionConfig.Keyword(
            numSlots = 2,
            keyword = "shoes",
            opaqueUserId = "user-abc",
            placementId = 1
        )

        val auction = Auction.fromConfig(config)

        assertThat(auction.opaqueUserId).isEqualTo("user-abc")
        assertThat(auction.placementId).isEqualTo(1)
        assertThat(auction.searchQuery).isEqualTo("shoes")
    }

    @Test
    fun `factory buildBannerAuctionCategorySingle with opaqueUserId and placementId`() {
        val auction = Auction.Factory.buildBannerAuctionCategorySingle(
            slots = 1,
            slotId = "banner-slot",
            category = "electronics",
            opaqueUserId = "user-test",
            placementId = 8
        )

        assertThat(auction.opaqueUserId).isEqualTo("user-test")
        assertThat(auction.placementId).isEqualTo(8)
    }

    @Test
    fun `factory banner methods silently ignore invalid placementId`() {
        val auction = Auction.Factory.buildBannerAuctionCategorySingle(
            slots = 1,
            slotId = "slot",
            category = "cat",
            placementId = 10 // Invalid - outside 1-8 range
        )

        // Invalid placementId should be silently set to null
        assertThat(auction.placementId).isNull()
    }

    @Test
    fun `factory banner methods silently ignore mismatched qualityScores`() {
        val auction = Auction.Factory.buildBannerAuctionLandingPage(
            slots = 1,
            slotId = "slot",
            ids = listOf("p1", "p2", "p3"),
            qualityScores = listOf(0.5, 0.7) // Size 2 vs ids size 3
        )

        // Mismatched qualityScores should be silently set to null
        assertThat(auction.products!!.qualityScores).isNull()
    }
}
