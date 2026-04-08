package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONObject
import org.junit.Test

internal class AuctionResponseTest {

    @Test
    fun `fromJson parses valid response with winners`() {
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
                                "id": "prod-123",
                                "resolvedBidId": "bid-456"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response).isNotNull
        assertThat(response!!.results).hasSize(1)
        assertThat(response.results[0].resultType).isEqualTo("listings")
        assertThat(response.results[0].error).isFalse()
        assertThat(response.results[0].winners).hasSize(1)
        assertThat(response.results[0].winners[0].id).isEqualTo("prod-123")
    }

    @Test
    fun `fromJson parses response with multiple results`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "listings",
                        "error": false,
                        "winners": [
                            {"rank": 1, "type": "product", "id": "p1", "resolvedBidId": "b1"}
                        ]
                    },
                    {
                        "resultType": "banners",
                        "error": false,
                        "winners": [
                            {"rank": 1, "type": "vendor", "id": "v1", "resolvedBidId": "b2"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response!!.results).hasSize(2)
        assertThat(response.results[0].resultType).isEqualTo("listings")
        assertThat(response.results[1].resultType).isEqualTo("banners")
    }

    @Test
    fun `fromJson parses response with empty winners`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "listings",
                        "error": false,
                        "winners": []
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response!!.results[0].winners).isEmpty()
    }

    @Test
    fun `fromJson parses winner with assets`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "banners",
                        "error": false,
                        "winners": [
                            {
                                "rank": 1,
                                "type": "product",
                                "id": "p1",
                                "resolvedBidId": "b1",
                                "asset": [
                                    {"url": "https://example.com/banner1.png"},
                                    {"url": "https://example.com/banner2.png"}
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        val winner = response!!.results[0].winners[0]
        assertThat(winner.asset).hasSize(2)
        assertThat(winner.asset!![0].url).isEqualTo("https://example.com/banner1.png")
        assertThat(winner.asset!![1].url).isEqualTo("https://example.com/banner2.png")
    }

    @Test
    fun `fromJson parses winner without assets`() {
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
                                "resolvedBidId": "b1"
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response!!.results[0].winners[0].asset).isNull()
    }

    @Test
    fun `fromJson throws EmptyResponse for null input`() {
        assertThatThrownBy {
            AuctionResponse.fromJson(null)
        }.isInstanceOf(AuctionError.EmptyResponse::class.java)
    }

    @Test
    fun `fromJson throws DeserializationError for missing results field`() {
        val json = """{"data": []}"""

        assertThatThrownBy {
            AuctionResponse.fromJson(json)
        }.isInstanceOf(AuctionError.DeserializationError::class.java)
            .hasRootCauseMessage("Missing 'results' field")
    }

    @Test
    fun `fromJson throws DeserializationError for invalid json`() {
        val json = "not valid json"

        assertThatThrownBy {
            AuctionResponse.fromJson(json)
        }.isInstanceOf(AuctionError.DeserializationError::class.java)
    }

    @Test
    fun `auctionResponseItem fromJsonObject throws for missing winners`() {
        val json = JSONObject("""
            {
                "resultType": "listings",
                "error": false
            }
        """.trimIndent())

        assertThatThrownBy {
            AuctionResponse.AuctionResponseItem.fromJsonObject(json)
        }.isInstanceOf(AuctionError.DeserializationError::class.java)
            .hasRootCauseMessage("Missing 'winners' field")
    }

    @Test
    fun `auctionWinnerItem parses product type`() {
        val json = JSONObject("""
            {
                "rank": 1,
                "type": "product",
                "id": "p1",
                "resolvedBidId": "b1"
            }
        """.trimIndent())

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(json)

        assertThat(winner.type).isEqualTo(EntityType.PRODUCT)
    }

    @Test
    fun `auctionWinnerItem parses vendor type`() {
        val json = JSONObject("""
            {
                "rank": 1,
                "type": "vendor",
                "id": "v1",
                "resolvedBidId": "b1"
            }
        """.trimIndent())

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(json)

        assertThat(winner.type).isEqualTo(EntityType.VENDOR)
    }

    @Test
    fun `auctionWinnerItem parses url type`() {
        val json = JSONObject("""
            {
                "rank": 1,
                "type": "url",
                "id": "https://example.com",
                "resolvedBidId": "b1"
            }
        """.trimIndent())

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(json)

        assertThat(winner.type).isEqualTo(EntityType.URL)
    }

    @Test
    fun `auctionWinnerItem preserves rank`() {
        val json = JSONObject("""
            {
                "rank": 5,
                "type": "product",
                "id": "p1",
                "resolvedBidId": "b1"
            }
        """.trimIndent())

        val winner = AuctionResponse.AuctionWinnerItem.fromJsonObject(json)

        assertThat(winner.rank).isEqualTo(5)
    }

    @Test
    fun `asset fromJsonObject parses url`() {
        val json = JSONObject("""
            {
                "url": "https://cdn.example.com/image.png"
            }
        """.trimIndent())

        val asset = AuctionResponse.Asset.fromJsonObject(json)

        assertThat(asset.url).isEqualTo("https://cdn.example.com/image.png")
    }

    @Test
    fun `asset fromJsonObject throws for missing url`() {
        val json = JSONObject("""
            {
                "content": {"key": "value"}
            }
        """.trimIndent())

        assertThatThrownBy {
            AuctionResponse.Asset.fromJsonObject(json)
        }.isInstanceOf(AuctionError.DeserializationError::class.java)
    }

    @Test
    fun `fromJson parses response with error flag true`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "listings",
                        "error": true,
                        "winners": []
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        assertThat(response!!.results[0].error).isTrue()
    }

    @Test
    fun `fromJson handles multiple winners with different ranks`() {
        val json = """
            {
                "results": [
                    {
                        "resultType": "listings",
                        "error": false,
                        "winners": [
                            {"rank": 1, "type": "product", "id": "p1", "resolvedBidId": "b1"},
                            {"rank": 2, "type": "product", "id": "p2", "resolvedBidId": "b2"},
                            {"rank": 3, "type": "product", "id": "p3", "resolvedBidId": "b3"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val response = AuctionResponse.fromJson(json)

        val winners = response!!.results[0].winners
        assertThat(winners).hasSize(3)
        assertThat(winners[0].rank).isEqualTo(1)
        assertThat(winners[1].rank).isEqualTo(2)
        assertThat(winners[2].rank).isEqualTo(3)
    }
}
