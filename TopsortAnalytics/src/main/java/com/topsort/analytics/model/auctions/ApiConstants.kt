package com.topsort.analytics.model.auctions

/**
 * Constants for API endpoints and other configuration values
 */
object ApiConstants {
    // API Endpoints
    const val BASE_API_URL = "https://api.topsort.com"
    const val AUCTION_ENDPOINT = "/v2/auctions"
    const val EVENTS_ENDPOINT = "/v2/events"
    const val AUCTIONS_TOPSORT_URL = BASE_API_URL + AUCTION_ENDPOINT
    
    // Auction limits
    const val MIN_AUCTIONS = 1
    const val MAX_AUCTIONS = 5
    
    // For backward compatibility with existing code using ServiceSettings
    @Deprecated("Use BASE_API_URL instead", ReplaceWith("ApiConstants.BASE_API_URL"))
    var baseApiUrl: String = BASE_API_URL
} 