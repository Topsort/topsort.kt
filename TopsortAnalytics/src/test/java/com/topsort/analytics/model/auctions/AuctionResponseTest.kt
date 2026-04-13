package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AuctionResponseTest {

    @Test
    fun `AuctionWinnerItem parses campaignId when present`() {
        val json = """
            {
                "rank": 1,
                "type": "product",
                "id": "product-123",
                "resolvedBidId": "bid-456",
                "campaignId": "campaign-789"
            }
        """.trimIndent()

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(
            org.json.JSONObject(json)
        )

        assertThat(winner.rank).isEqualTo(1)
        assertThat(winner.type).isEqualTo(EntityType.PRODUCT)
        assertThat(winner.id).isEqualTo("product-123")
        assertThat(winner.resolvedBidId).isEqualTo("bid-456")
        assertThat(winner.campaignId).isEqualTo("campaign-789")
    }

    @Test
    fun `AuctionWinnerItem campaignId is null when absent`() {
        val json = """
            {
                "rank": 1,
                "type": "product",
                "id": "product-123",
                "resolvedBidId": "bid-456"
            }
        """.trimIndent()

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(
            org.json.JSONObject(json)
        )

        assertThat(winner.campaignId).isNull()
    }

    @Test
    fun `AuctionWinnerItem campaignId is null when explicitly null in JSON`() {
        val json = org.json.JSONObject().apply {
            put("rank", 1)
            put("type", "product")
            put("id", "product-123")
            put("resolvedBidId", "bid-456")
            put("campaignId", org.json.JSONObject.NULL)
        }

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(json)

        assertThat(winner.campaignId).isNull()
    }

    @Test
    fun `Asset parses content when present`() {
        val json = """
            {
                "url": "https://example.com/banner.png",
                "content": "<div>Banner HTML content</div>"
            }
        """.trimIndent()

        val asset = AuctionResponse.Asset.fromJsonObject(
            org.json.JSONObject(json)
        )

        assertThat(asset.url).isEqualTo("https://example.com/banner.png")
        assertThat(asset.content).isEqualTo("<div>Banner HTML content</div>")
    }

    @Test
    fun `Asset content is null when absent`() {
        val json = """
            {
                "url": "https://example.com/banner.png"
            }
        """.trimIndent()

        val asset = AuctionResponse.Asset.fromJsonObject(
            org.json.JSONObject(json)
        )

        assertThat(asset.url).isEqualTo("https://example.com/banner.png")
        assertThat(asset.content).isNull()
    }

    @Test
    fun `Asset content is null when explicitly null in JSON`() {
        val json = org.json.JSONObject().apply {
            put("url", "https://example.com/banner.png")
            put("content", org.json.JSONObject.NULL)
        }

        val asset = AuctionResponse.Asset.fromJsonObject(json)

        assertThat(asset.url).isEqualTo("https://example.com/banner.png")
        assertThat(asset.content).isNull()
    }

    @Test
    fun `AuctionWinnerItem parses with assets including content`() {
        val json = """
            {
                "rank": 1,
                "type": "vendor",
                "id": "vendor-123",
                "resolvedBidId": "bid-456",
                "campaignId": "campaign-001",
                "asset": [
                    {
                        "url": "https://example.com/img1.png",
                        "content": "Content 1"
                    },
                    {
                        "url": "https://example.com/img2.png"
                    }
                ]
            }
        """.trimIndent()

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(
            org.json.JSONObject(json)
        )

        assertThat(winner.campaignId).isEqualTo("campaign-001")
        assertThat(winner.asset).hasSize(2)
        assertThat(winner.asset!![0].url).isEqualTo("https://example.com/img1.png")
        assertThat(winner.asset!![0].content).isEqualTo("Content 1")
        assertThat(winner.asset!![1].url).isEqualTo("https://example.com/img2.png")
        assertThat(winner.asset!![1].content).isNull()
    }

    @Test
    fun `full AuctionResponse parses with new fields`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "listings",
                        "error": false,
                        "winners": [
                            {
                                "rank": 1,
                                "type": "product",
                                "id": "p1",
                                "resolvedBidId": "bid1",
                                "campaignId": "camp1",
                                "asset": [
                                    {"url": "https://example.com/a.png", "content": "Hello"}
                                ]
                            },
                            {
                                "rank": 2,
                                "type": "product",
                                "id": "p2",
                                "resolvedBidId": "bid2"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response).isNotNull
        assertThat(response!!.results).hasSize(1)
        assertThat(response.results[0].winners).hasSize(2)

        val winner1 = response.results[0].winners[0]
        assertThat(winner1.campaignId).isEqualTo("camp1")
        assertThat(winner1.asset).hasSize(1)
        assertThat(winner1.asset!![0].content).isEqualTo("Hello")

        val winner2 = response.results[0].winners[1]
        assertThat(winner2.campaignId).isNull()
        assertThat(winner2.asset).isNull()
    }
}
