package com.topsort.analytics.service

import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.model.auctions.AuctionResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class TopsortAuctionsHttpServiceTest {

    private val mockService = mockk<AuctionsHttpService>()

    private val minimalRequest = AuctionRequest(
        listOf(Auction.Factory.buildSponsoredListingAuctionProductIds(1, listOf("p1")))
    )

    @Before
    fun setUp() {
        TopsortAuctionsHttpService.setMockService(mockService)
    }

    @After
    fun tearDown() {
        TopsortAuctionsHttpService.resetToDefaultService()
    }

    @Test
    fun `runAuctions delegates to injected service via runAuctionsSync`() = runBlocking {
        val expectedResponse = mockk<AuctionResponse>()
        every { mockService.runAuctionsSync(minimalRequest) } returns expectedResponse

        val result = TopsortAuctionsHttpService.runAuctions(minimalRequest)

        assertEquals(expectedResponse, result)
        verify(exactly = 1) { mockService.runAuctionsSync(minimalRequest) }
    }

    @Test
    fun `runAuctions throws EmptyResponse when service returns null`() = runBlocking<Unit> {
        every { mockService.runAuctionsSync(any()) } returns null

        var thrown: AuctionError? = null
        try {
            TopsortAuctionsHttpService.runAuctions(minimalRequest)
        } catch (e: AuctionError) {
            thrown = e
        }

        assertTrue("Expected AuctionError.EmptyResponse to be thrown", thrown is AuctionError.EmptyResponse)
    }

    @Test
    fun `runAuctions propagates AuctionError from service`() = runBlocking<Unit> {
        val cause = IOException("network error")
        val exception = AuctionError.HttpError(cause)
        every { mockService.runAuctionsSync(any()) } throws exception

        var thrown: AuctionError? = null
        try {
            TopsortAuctionsHttpService.runAuctions(minimalRequest)
        } catch (e: AuctionError) {
            thrown = e
        }

        assertTrue("Expected AuctionError.HttpError to be thrown", thrown is AuctionError.HttpError)
        assertSame(cause, (thrown as AuctionError.HttpError).error)
    }

    @Test
    fun `resetToDefaultService restores TopsortAuctionsHttpService as service instance`() {
        val anotherMock = mockk<AuctionsHttpService>()
        TopsortAuctionsHttpService.setMockService(anotherMock)
        TopsortAuctionsHttpService.resetToDefaultService()
        assertSame(TopsortAuctionsHttpService, TopsortAuctionsHttpService.serviceInstance)
    }

    @Test
    fun `setMockService replaces the service instance`() {
        val anotherMock = mockk<AuctionsHttpService>()
        TopsortAuctionsHttpService.setMockService(anotherMock)
        assertSame(anotherMock, TopsortAuctionsHttpService.serviceInstance)
        TopsortAuctionsHttpService.resetToDefaultService()
    }
}
