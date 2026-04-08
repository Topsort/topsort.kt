# Topsort Kotlin SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.topsort/topsort-kt.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.topsort/topsort-kt)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org)
[![CI](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml/badge.svg)](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml)

An Android library for interacting with the Topsort APIs. We provide support for sending events (impressions, clicks, purchases, page views) and running auctions with comprehensive error handling and callback support.

## Features

- **Event Tracking**: Report impressions, clicks, purchases, and page views
- **Auction Support**: Run sponsored listings and banner auctions
- **Offline Support**: Events are cached and sent when network is available
- **Error Handling**: Comprehensive error types with sealed classes
- **A/B Testing**: Built-in support for experiment placement IDs
- **Quality Scores**: Pass product quality scores for auction optimization

## Requirements

- Minimum Java version: 11
- Android SDK: 24+
- `INTERNET` permission (add to your `AndroidManifest.xml` if not already present)

## Installation

Add the dependency to your `build.gradle` file:

```gradle
dependencies {
    // Check Maven Central for the latest version:
    // https://central.sonatype.com/artifact/com.topsort/topsort-kt
    implementation 'com.topsort:topsort-kt:2.1.0'
}
```

Ensure your project is configured to use at least Java 11:

```gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = '11'
    }
}
```

The library is distributed through Maven Central:

```gradle
repositories {
    mavenCentral()
}
```

## Quick Start

### Setup

Initialize the SDK in your Application class:

```kotlin
import android.app.Application
import com.topsort.analytics.Analytics

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Analytics.setup(
            application = this,
            opaqueUserId = "user-unique-id",  // Consistent per user
            token = "your-bearer-token"
        )
    }
}
```

## Event Tracking

### Impressions and Clicks

Track when products are viewed or clicked:

```kotlin
// Promoted events (with resolvedBidId from auction)
Analytics.reportImpressionPromoted(
    resolvedBidId = "auction-winner-bid-id",
    placement = Placement(path = "/search/results"),
    deviceType = "mobile",      // optional: "desktop" or "mobile"
    channel = "onsite",         // optional: "onsite", "offsite", or "instore"
    page = Page.Factory.build(type = Page.TYPE_SEARCH)  // optional
)

Analytics.reportClickPromoted(
    resolvedBidId = "auction-winner-bid-id",
    placement = Placement(path = "/search/results"),
    clickType = Click.TYPE_PRODUCT  // optional: "product", "like", or "add-to-cart"
)

// Organic events (without auction)
Analytics.reportImpressionOrganic(
    entity = Entity(id = "product-123", type = EntityType.PRODUCT),
    placement = Placement(path = "/category/electronics")
)

Analytics.reportClickOrganic(
    entity = Entity(id = "product-123", type = EntityType.PRODUCT),
    placement = Placement(path = "/category/electronics")
)
```

### Purchases

Track completed purchases:

```kotlin
Analytics.reportPurchase(
    id = "order-123",
    items = listOf(
        PurchasedItem(
            productId = "product-123",
            quantity = 2,
            unitPrice = 1295,  // price in cents ($12.95)
            resolvedBidId = "bid-id",  // optional: if from promoted click
            vendorId = "vendor-456"    // optional: for halo attribution
        )
    )
)
```

### Page Views

Track page views for analytics:

```kotlin
Analytics.reportPageView(
    page = Page.Factory.buildWithId(
        type = Page.TYPE_PDP,
        pageId = "product-123"
    ),
    deviceType = "mobile",
    channel = "onsite"
)

// Page types: TYPE_HOME, TYPE_CATEGORY, TYPE_PDP, TYPE_SEARCH, TYPE_CART, TYPE_OTHER
```

## Auctions

### Sponsored Listings

Run auctions to get promoted products:

```kotlin
import com.topsort.analytics.model.auctions.Auction
import com.topsort.analytics.model.auctions.AuctionConfig
import com.topsort.analytics.model.auctions.AuctionRequest
import com.topsort.analytics.service.TopsortAuctionsHttpService

// Create auction config
val config = AuctionConfig.ProductIds(
    numSlots = 3,
    ids = listOf("product-1", "product-2", "product-3"),
    qualityScores = listOf(0.9, 0.8, 0.7),  // optional
    opaqueUserId = "user-123",              // optional: for targeting
    placementId = 5                         // optional: for A/B testing (1-8)
)

// Build and run auction
val auction = Auction.fromConfig(config)
val request = AuctionRequest(auctions = listOf(auction))

// Synchronous
val response = TopsortAuctionsHttpService.runAuctionsSync(request)

// Or with coroutines
lifecycleScope.launch {
    val response = TopsortAuctionsHttpService.runAuctions(request)
    response.results.forEach { result ->
        result.winners.forEach { winner ->
            println("Winner: ${winner.id}, Bid: ${winner.resolvedBidId}")
            println("Campaign: ${winner.campaignId}")  // optional campaign info
        }
    }
}
```

### Banner Auctions

Display banner ads with the BannerView component:

```kotlin
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView

val bannerView = findViewById<BannerView>(R.id.banner_view)

// Configure callbacks
bannerView.onError { throwable ->
    Log.e("Banner", "Error loading banner", throwable)
}

bannerView.onAuctionError { error ->
    when (error) {
        is AuctionError.HttpError -> Log.e("Banner", "Network error")
        is AuctionError.EmptyResponse -> Log.e("Banner", "No response")
        else -> Log.e("Banner", "Auction error: $error")
    }
}

bannerView.onNoWinners {
    Log.d("Banner", "No winners for this auction")
}

bannerView.onImageLoad {
    Log.d("Banner", "Banner loaded successfully")
}

// Run the auction
lifecycleScope.launch {
    val config = BannerConfig.LandingPage(
        slotId = "slot-123",
        ids = listOf("product-1", "product-2")
    )

    bannerView.setup(
        config = config,
        path = "home-page",
        location = "top-banner"
    ) { id, type ->
        // Handle banner click
        when (type) {
            EntityType.PRODUCT -> openProductPage(id)
            EntityType.VENDOR -> openVendorPage(id)
            else -> openUrl(id)
        }
    }
}
```

## Error Handling

The library uses sealed classes for comprehensive error handling:

```kotlin
when (error) {
    is AuctionError.HttpError -> {
        // Network or server error
        Log.e("Auction", "HTTP ${error.code}: ${error.message}")
    }
    is AuctionError.DeserializationError -> {
        // Failed to parse response
        Log.e("Auction", "Parse error", error.cause)
    }
    is AuctionError.SerializationError -> {
        // Failed to serialize request
    }
    is AuctionError.EmptyResponse -> {
        // Server returned empty response
    }
    is AuctionError.InvalidNumberAuctions -> {
        // Invalid number of auctions (must be 1-5)
    }
}
```

## Testing

Mock the auction service for testing:

```kotlin
// Setup mock
val mockService = MockAuctionsHttpService()
TopsortAuctionsHttpService.setMockService(mockService)

// Run tests...

// Cleanup
TopsortAuctionsHttpService.resetToDefaultService()
```

## API Reference

For detailed API documentation, visit our [API docs](https://docs.topsort.com).

## License

This project is licensed under the MIT License - see the [LICENSE][1] file for details.

[1]: https://github.com/Topsort/topsort.kt/blob/main/LICENSE
