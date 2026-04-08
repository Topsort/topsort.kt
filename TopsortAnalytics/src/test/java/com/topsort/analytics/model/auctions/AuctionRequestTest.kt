package com.topsort.analytics.model.auctions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

internal class AuctionRequestTest {

    private fun createTestAuction(): Auction {
        return Auction.fromConfig(
            AuctionConfig.ProductIds(numSlots = 1, ids = listOf("p1"))
        )
    }

    @Test
    fun `auctionRequest with single auction is valid`() {
        val request = AuctionRequest(auctions = listOf(createTestAuction()))

        assertThat(request.auctions).hasSize(1)
    }

    @Test
    fun `auctionRequest with max auctions is valid`() {
        val auctions = (1..ApiConstants.MAX_AUCTIONS).map { createTestAuction() }

        val request = AuctionRequest(auctions = auctions)

        assertThat(request.auctions).hasSize(ApiConstants.MAX_AUCTIONS)
    }

    @Test
    fun `auctionRequest with empty list throws InvalidNumberAuctions`() {
        assertThatThrownBy {
            AuctionRequest(auctions = emptyList())
        }.isInstanceOf(AuctionError.InvalidNumberAuctions::class.java)
            .hasMessageContaining("Invalid number of auctions: 0")
    }

    @Test
    fun `auctionRequest with too many auctions throws InvalidNumberAuctions`() {
        val auctions = (1..ApiConstants.MAX_AUCTIONS + 1).map { createTestAuction() }

        assertThatThrownBy {
            AuctionRequest(auctions = auctions)
        }.isInstanceOf(AuctionError.InvalidNumberAuctions::class.java)
            .hasMessageContaining("Invalid number of auctions: ${ApiConstants.MAX_AUCTIONS + 1}")
    }

    @Test
    fun `toJsonObject creates valid json structure`() {
        val auctions = listOf(
            Auction.fromConfig(AuctionConfig.ProductIds(numSlots = 1, ids = listOf("p1"))),
            Auction.fromConfig(AuctionConfig.CategorySingle(numSlots = 2, category = "electronics"))
        )
        val request = AuctionRequest(auctions = auctions)

        val json = request.toJsonObject()

        assertThat(json.has("auctions")).isTrue()
        val auctionsArray = json.getJSONArray("auctions")
        assertThat(auctionsArray.length()).isEqualTo(2)

        // First auction should have products
        assertThat(auctionsArray.getJSONObject(0).has("products")).isTrue()

        // Second auction should have category
        assertThat(auctionsArray.getJSONObject(1).has("category")).isTrue()
    }

    @Test
    fun `toJsonObject preserves auction order`() {
        val auctions = listOf(
            Auction.fromConfig(AuctionConfig.Keyword(numSlots = 1, keyword = "first")),
            Auction.fromConfig(AuctionConfig.Keyword(numSlots = 2, keyword = "second")),
            Auction.fromConfig(AuctionConfig.Keyword(numSlots = 3, keyword = "third"))
        )
        val request = AuctionRequest(auctions = auctions)

        val json = request.toJsonObject()
        val auctionsArray = json.getJSONArray("auctions")

        assertThat(auctionsArray.getJSONObject(0).getString("searchQuery")).isEqualTo("first")
        assertThat(auctionsArray.getJSONObject(1).getString("searchQuery")).isEqualTo("second")
        assertThat(auctionsArray.getJSONObject(2).getString("searchQuery")).isEqualTo("third")
    }

    @Test
    fun `invalidNumberAuctions error contains count`() {
        try {
            AuctionRequest(auctions = emptyList())
        } catch (e: AuctionError.InvalidNumberAuctions) {
            assertThat(e.count).isEqualTo(0)
            assertThat(e.message).contains("0")
            assertThat(e.message).contains(ApiConstants.MIN_AUCTIONS.toString())
            assertThat(e.message).contains(ApiConstants.MAX_AUCTIONS.toString())
        }
    }

    @Test
    fun `auctionRequest with different auction types`() {
        val auctions = listOf(
            Auction.fromConfig(AuctionConfig.ProductIds(numSlots = 1, ids = listOf("p1"))),
            Auction.fromConfig(AuctionConfig.CategorySingle(numSlots = 1, category = "cat1")),
            Auction.fromConfig(AuctionConfig.Keyword(numSlots = 1, keyword = "search"))
        )

        val request = AuctionRequest(auctions = auctions)
        val json = request.toJsonObject()

        assertThat(json.getJSONArray("auctions").length()).isEqualTo(3)
    }

    @Test
    fun `auctionRequest boundary test with exactly min auctions`() {
        val auctions = (1..ApiConstants.MIN_AUCTIONS).map { createTestAuction() }

        val request = AuctionRequest(auctions = auctions)

        assertThat(request.auctions).hasSize(ApiConstants.MIN_AUCTIONS)
    }

    @Test
    fun `auctionRequest boundary test with exactly max auctions`() {
        val auctions = (1..ApiConstants.MAX_AUCTIONS).map { createTestAuction() }

        val request = AuctionRequest(auctions = auctions)

        assertThat(request.auctions).hasSize(ApiConstants.MAX_AUCTIONS)
    }
}
