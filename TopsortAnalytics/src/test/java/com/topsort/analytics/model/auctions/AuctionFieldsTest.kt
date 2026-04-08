package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
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
    fun `auctionConfig placementId must be between 1 and 8`() {
        assertThatThrownBy {
            AuctionConfig.ProductIds(
                numSlots = 1,
                ids = listOf("p1"),
                placementId = 0
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("placementId must be between 1 and 8")

        assertThatThrownBy {
            AuctionConfig.ProductIds(
                numSlots = 1,
                ids = listOf("p1"),
                placementId = 9
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("placementId must be between 1 and 8")
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
    fun `products qualityScores size must match ids size`() {
        assertThatThrownBy {
            Auction.Products(
                ids = listOf("p1", "p2", "p3"),
                qualityScores = listOf(0.5, 0.7)
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("qualityScores size (2) must match ids size (3)")
    }

    @Test
    fun `auctionConfig qualityScores size must match ids size`() {
        assertThatThrownBy {
            AuctionConfig.ProductIds(
                numSlots = 1,
                ids = listOf("p1", "p2"),
                qualityScores = listOf(0.5)
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("qualityScores size (1) must match ids size (2)")
    }

    // Tests for Factory methods with new fields

    @Test
    fun `factory buildSponsoredListingAuctionProductIds with new fields`() {
        @Suppress("DEPRECATION")
        val auction = Auction.Factory.buildSponsoredListingAuctionProductIds(
            slots = 2,
            ids = listOf("p1", "p2"),
            opaqueUserId = "user-123",
            placementId = 4,
            qualityScores = listOf(0.8, 0.9)
        )

        assertThat(auction.opaqueUserId).isEqualTo("user-123")
        assertThat(auction.placementId).isEqualTo(4)
        assertThat(auction.products!!.qualityScores).containsExactly(0.8, 0.9)
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

    // Tests for campaignId in AuctionWinnerItem

    @Test
    fun `auctionWinnerItem deserializes campaignId when present`() {
        val json = """
            {
                "rank": 1,
                "type": "product",
                "id": "prod-123",
                "resolvedBidId": "bid-456",
                "campaignId": "campaign-789"
            }
        """.trimIndent()

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(JSONObject(json))

        assertThat(winner.campaignId).isEqualTo("campaign-789")
    }

    @Test
    fun `auctionWinnerItem campaignId is null when not present`() {
        val json = """
            {
                "rank": 1,
                "type": "product",
                "id": "prod-123",
                "resolvedBidId": "bid-456"
            }
        """.trimIndent()

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(JSONObject(json))

        assertThat(winner.campaignId).isNull()
    }

    // Tests for content in Asset

    @Test
    fun `asset deserializes content when present`() {
        val json = """
            {
                "url": "https://example.com/banner.png",
                "content": {
                    "headline": "Special Offer",
                    "cta": "Shop Now",
                    "description": "Limited time only"
                }
            }
        """.trimIndent()

        val asset = AuctionResponse.Asset.fromJsonObject(JSONObject(json))

        assertThat(asset.url).isEqualTo("https://example.com/banner.png")
        assertThat(asset.content).isNotNull
        assertThat(asset.content!!["headline"]).isEqualTo("Special Offer")
        assertThat(asset.content!!["cta"]).isEqualTo("Shop Now")
        assertThat(asset.content!!["description"]).isEqualTo("Limited time only")
    }

    @Test
    fun `asset content is null when not present`() {
        val json = """
            {
                "url": "https://example.com/banner.png"
            }
        """.trimIndent()

        val asset = AuctionResponse.Asset.fromJsonObject(JSONObject(json))

        assertThat(asset.url).isEqualTo("https://example.com/banner.png")
        assertThat(asset.content).isNull()
    }

    @Test
    fun `asset deserializes empty content map`() {
        val json = """
            {
                "url": "https://example.com/banner.png",
                "content": {}
            }
        """.trimIndent()

        val asset = AuctionResponse.Asset.fromJsonObject(JSONObject(json))

        assertThat(asset.content).isNotNull
        assertThat(asset.content).isEmpty()
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
}
