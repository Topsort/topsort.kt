# topsort.kt

[![Build](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml/badge.svg)](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml)
[![Maven Central](https://img.shields.io/maven-central/v/com.topsort/topsort-kt.svg)](https://central.sonatype.com/artifact/com.topsort/topsort-kt)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android API](https://img.shields.io/badge/API-24+-34A853.svg?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://github.com/Topsort/topsort.kt/blob/main/LICENSE)

Android SDK for [Topsort's](https://www.topsort.com) retail media platform. Provides auction execution, event tracking (impressions, clicks, purchases, page views), and banner ad components.

## Features

- **Auctions** - Run sponsored listing and banner auctions
- **Event Tracking** - Report impressions, clicks, purchases, and page views
- **Banner Ads** - Ready-to-use `BannerView` component with automatic tracking
- **Offline Support** - Events are cached and delivered when connectivity returns
- **Zero Configuration** - Sensible defaults with optional customization

## Requirements

- Android API 24+ (Android 7.0 Nougat)
- Java 11+

## Installation

Add the dependency to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.topsort:topsort-kt:2.0.1'
}
```

The library is distributed via [Maven Central](https://central.sonatype.com/artifact/com.topsort/topsort-kt).

## Quick Start

### Setup

Initialize the SDK in your `Application` class:

```kotlin
import com.topsort.analytics.Analytics

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Analytics.setup(
            application = this,
            opaqueUserId = "user-unique-id",
            token = "your-api-token"
        )
    }
}
```

### Event Tracking

#### Impressions & Clicks

```kotlin
// Promoted (from auction winner)
Analytics.reportImpressionPromoted(
    resolvedBidId = "auction-bid-id",
    placement = Placement(path = "/search/results")
)

Analytics.reportClickPromoted(
    resolvedBidId = "auction-bid-id",
    placement = Placement(path = "/search/results")
)

// Organic (without auction)
Analytics.reportImpressionOrganic(
    entity = Entity(id = "product-123", type = EntityType.PRODUCT),
    placement = Placement(path = "/category/electronics")
)
```

#### Purchases

```kotlin
Analytics.reportPurchase(
    id = "order-123",
    items = listOf(
        PurchasedItem(
            productId = "product-123",
            quantity = 2,
            unitPrice = 1295  // cents ($12.95)
        )
    )
)
```

#### Page Views

```kotlin
import com.topsort.analytics.model.Page

// Product detail page
Analytics.reportPageView(
    page = Page.Factory.buildWithId(
        type = Page.TYPE_PDP,
        pageId = "product-123"
    ),
    deviceType = "mobile",
    channel = "onsite"
)

// Category with hierarchy
Analytics.reportPageView(
    page = Page.Factory.buildWithValues(
        type = Page.TYPE_CATEGORY,
        values = listOf("Electronics", "Phones", "Smartphones")
    )
)

// Search results
Analytics.reportPageView(
    page = Page.Factory.buildWithId(
        type = Page.TYPE_SEARCH,
        pageId = "search",
        value = "running shoes"  // search query
    )
)
```

**Page Types:** `TYPE_HOME`, `TYPE_CATEGORY`, `TYPE_PDP`, `TYPE_SEARCH`, `TYPE_CART`, `TYPE_OTHER`

### Auctions

```kotlin
import com.topsort.analytics.model.auctions.*
import com.topsort.analytics.service.TopsortAuctionsHttpService

val config = AuctionConfig.ProductIds(
    numSlots = 3,
    ids = listOf("product-1", "product-2", "product-3")
)

val auction = Auction.fromConfig(config)
val request = AuctionRequest(auctions = listOf(auction))

// Coroutine
lifecycleScope.launch {
    val response = TopsortAuctionsHttpService.runAuctions(request)
    response.results.forEach { result ->
        result.winners.forEach { winner ->
            // Use winner.resolvedBidId for tracking
        }
    }
}

// Synchronous (not on main thread)
val response = TopsortAuctionsHttpService.runAuctionsSync(request)
```

### Banner Ads

```kotlin
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView

val bannerView = findViewById<BannerView>(R.id.banner_view)

// Callbacks
bannerView.onImageLoad { /* Banner loaded */ }
bannerView.onNoWinners { /* No ads available */ }
bannerView.onError { throwable -> /* Handle error */ }
bannerView.onAuctionError { error -> /* Handle auction error */ }

// Run auction and display
lifecycleScope.launch {
    val config = BannerConfig.LandingPage(
        slotId = "home-banner",
        ids = listOf("product-1", "product-2")
    )

    bannerView.setup(config, path = "home", location = "top") { id, type ->
        // Handle click
    }
}
```

## Error Handling

```kotlin
when (error) {
    is AuctionError.HttpError -> // Network/server error
    is AuctionError.DeserializationError -> // Parse error
    is AuctionError.EmptyResponse -> // No response
    is AuctionError.InvalidNumberAuctions -> // Invalid auction count (1-5)
    else -> // Other errors
}
```

## Testing

Mock the auction service for unit tests:

```kotlin
val mockService = MockAuctionsHttpService()
TopsortAuctionsHttpService.setMockService(mockService)

// Run tests...

TopsortAuctionsHttpService.resetToDefaultService()
```

## Documentation

- [API Documentation](https://docs.topsort.com)
- [Topsort Dashboard](https://app.topsort.com)

## License

[MIT](LICENSE)
