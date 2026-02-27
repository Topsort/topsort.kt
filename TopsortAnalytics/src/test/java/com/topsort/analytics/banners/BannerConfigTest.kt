package com.topsort.analytics.banners

import com.topsort.analytics.model.auctions.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BannerConfigTest {

    @Test
    fun `LandingPage defaults device to MOBILE`() {
        val config = BannerConfig.LandingPage(slotId = "slot-1")
        assertEquals(Device.MOBILE, config.device)
    }

    @Test
    fun `LandingPage ids defaults to null`() {
        val config = BannerConfig.LandingPage(slotId = "slot-1")
        assertNull(config.ids)
    }

    @Test
    fun `LandingPage geoTargeting defaults to null`() {
        val config = BannerConfig.LandingPage(slotId = "slot-1")
        assertNull(config.geoTargeting)
    }

    @Test
    fun `LandingPage with ids is distinct from one without`() {
        val withIds = BannerConfig.LandingPage(slotId = "s", ids = listOf("p1"))
        val withoutIds = BannerConfig.LandingPage(slotId = "s")
        assertNotEquals(withIds, withoutIds)
    }

    @Test
    fun `LandingPage copy preserves unchanged fields`() {
        val original = BannerConfig.LandingPage(slotId = "s", ids = listOf("p1"), geoTargeting = "US")
        val copy = original.copy(slotId = "s2")
        assertEquals(listOf("p1"), copy.ids)
        assertEquals("US", copy.geoTargeting)
        assertEquals(Device.MOBILE, copy.device)
    }

    @Test
    fun `CategorySingle defaults device to MOBILE`() {
        val config = BannerConfig.CategorySingle(slotId = "slot-1", category = "electronics")
        assertEquals(Device.MOBILE, config.device)
    }

    @Test
    fun `CategorySingle accepts DESKTOP device`() {
        val config = BannerConfig.CategorySingle(slotId = "slot-1", category = "electronics", device = Device.DESKTOP)
        assertEquals(Device.DESKTOP, config.device)
    }

    @Test
    fun `CategoryMultiple stores all categories`() {
        val categories = listOf("cat1", "cat2", "cat3")
        val config = BannerConfig.CategoryMultiple(slotId = "slot-1", categories = categories)
        assertEquals(categories, config.categories)
    }

    @Test
    fun `Keyword stores keyword value`() {
        val config = BannerConfig.Keyword(slotId = "slot-1", keyword = "running shoes")
        assertEquals("running shoes", config.keyword)
    }
}
