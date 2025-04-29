package com.topsort.analytics.model.auctions

/**
 * Sealed class that represents various errors that can occur during auctions
 */
sealed class AuctionError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Error that occurs when there's an HTTP-related issue
     * @param error The original HTTP client error
     */
    class HttpError(val error: Throwable) : AuctionError("HTTP error during auction", error)

    /**
     * Error that occurs when the number of auctions is invalid
     * @param count The invalid auction count
     */
    class InvalidNumberAuctions(val count: Int) : 
        AuctionError("Invalid number of auctions: $count. Must be between ${ApiConstants.MIN_AUCTIONS} and ${ApiConstants.MAX_AUCTIONS}")

    /**
     * Error that occurs during serialization of auction requests
     */
    object SerializationError : AuctionError("Failed to serialize auction request")

    /**
     * Error that occurs during deserialization of auction responses
     * @param error The original error that occurred during deserialization
     * @param data The data that failed to deserialize
     */
    class DeserializationError(val error: Throwable, val data: ByteArray) : 
        AuctionError("Failed to deserialize auction response", error)

    /**
     * Error that occurs when the auction response is empty
     */
    object EmptyResponse : AuctionError("Auction returned an empty response")
} 