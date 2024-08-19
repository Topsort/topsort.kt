package com.topsort.analytics.banners

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class RunTest {
    @Test
    fun buildLandingPageBanner() {
        val slot = "slot"
        val ids = listOf("id1", "id2")
        val bannerConfig = BannerConfig.LandingPage(slotId = slot, ids = ids)
        val bannerAuction = buildBannerAuction(bannerConfig);
        val json = bannerAuction.toJsonObject().toString()
        val expectedJson =
            "{\"slots\":1,\"slotId\":\"$slot\",\"type\":\"banners\",\"device\":\"mobile\",\"products\":{\"ids\":[\"${ids[0]}\",\"${ids[1]}\"]}}"
        assertThat(json).isEqualTo(expectedJson)
    }

    @Test
    fun buildSingleCategoryBanner() {
        val slot = "slot"
        val category = "category"
        val bannerConfig = BannerConfig.CategorySingle(slotId = slot, category = category)
        val bannerAuction = buildBannerAuction(bannerConfig);
        val json = bannerAuction.toJsonObject().toString()
        val expectedJson =
            "{\"slots\":1,\"slotId\":\"$slot\",\"type\":\"banners\",\"category\":{\"id\":\"$category\"},\"device\":\"mobile\"}"
        assertThat(json).isEqualTo(expectedJson)
    }

    @Test
    fun buildMultipleCategoryBanner() {
        val slot = "slot"
        val categories = listOf("cat1", "cat2")
        val bannerConfig = BannerConfig.CategoryMultiple(slotId = slot, categories = categories)
        val bannerAuction = buildBannerAuction(bannerConfig);
        val json = bannerAuction.toJsonObject().toString()
        val expectedJson =
            "{\"slots\":1,\"slotId\":\"$slot\",\"type\":\"banners\",\"category\":{\"ids\":[\"${categories[0]}\",\"${categories[1]}\"]},\"device\":\"mobile\"}"
        assertThat(json).isEqualTo(expectedJson)
    }

    @Test
    fun buildDisjuctionsCategoryBanner() {
        val slot = "slot"
        val disjunctions = listOf(listOf("cat1", "cat2"), listOf("cat3"))
        val bannerConfig =
            BannerConfig.CategoryDisjunctions(slotId = slot, disjunctions = disjunctions)
        val bannerAuction = buildBannerAuction(bannerConfig);
        val json = bannerAuction.toJsonObject().toString()
        val expectedJson =
            "{\"slots\":1,\"slotId\":\"$slot\",\"type\":\"banners\",\"category\":{\"disjunctions\":[[\"${disjunctions[0][0]}\",\"${disjunctions[0][1]}\"],[\"${disjunctions[1][0]}\"]]},\"device\":\"mobile\"}"
        assertThat(json).isEqualTo(expectedJson)
    }
}
