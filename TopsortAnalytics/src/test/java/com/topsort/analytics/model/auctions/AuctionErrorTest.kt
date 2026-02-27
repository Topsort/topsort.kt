package com.topsort.analytics.model.auctions

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AuctionErrorTest {

    @Test
    fun `HttpError wraps throwable cause`() {
        val cause = IOException("timeout")
        val error = AuctionError.HttpError(cause)
        assertSame(cause, error.error)
    }

    @Test
    fun `HttpError message describes the failure`() {
        val error = AuctionError.HttpError(RuntimeException("conn refused"))
        assertTrue(error.message!!.isNotBlank())
    }

    @Test
    fun `DeserializationError carries cause and raw data`() {
        val cause = IllegalArgumentException("bad json")
        val data = "bad".toByteArray()
        val error = AuctionError.DeserializationError(cause, data)
        assertSame(cause, error.error)
        assertArrayEquals(data, error.data)
    }

    @Test
    fun `EmptyResponse is a singleton`() {
        assertSame(AuctionError.EmptyResponse, AuctionError.EmptyResponse)
    }

    @Test
    fun `SerializationError is a singleton`() {
        assertSame(AuctionError.SerializationError, AuctionError.SerializationError)
    }

    @Test
    fun `InvalidNumberAuctions carries the invalid count`() {
        val error = AuctionError.InvalidNumberAuctions(0)
        assertEquals(0, error.count)
    }

    @Test
    fun `InvalidNumberAuctions message includes the count`() {
        val error = AuctionError.InvalidNumberAuctions(99)
        assertTrue(error.message!!.contains("99"))
    }

    @Test
    fun `when expression is exhaustive over all subtypes`() {
        val cause = RuntimeException("test")
        val errors: List<AuctionError> = listOf(
            AuctionError.HttpError(cause),
            AuctionError.DeserializationError(cause, byteArrayOf()),
            AuctionError.SerializationError,
            AuctionError.EmptyResponse,
            AuctionError.InvalidNumberAuctions(0),
        )
        errors.forEach { error ->
            val handled = when (error) {
                is AuctionError.HttpError -> true
                is AuctionError.DeserializationError -> true
                is AuctionError.SerializationError -> true
                is AuctionError.EmptyResponse -> true
                is AuctionError.InvalidNumberAuctions -> true
            }
            assertTrue(handled)
        }
    }
}
