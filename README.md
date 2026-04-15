# Topsort Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.topsort/topsort-kt)](https://central.sonatype.com/artifact/com.topsort/topsort-kt)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/Topsort/topsort.kt/blob/main/LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![CI](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml/badge.svg)](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml)

The official Android SDK for the [Topsort](https://www.topsort.com) retail media platform. Track impressions, clicks, purchases, and page views with full support for promoted and organic content attribution.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Event Tracking](#event-tracking)
  - [Impressions](#impressions)
  - [Clicks](#clicks)
  - [Purchases](#purchases)
  - [Page Views](#page-views)
- [Running Auctions](#running-auctions)
  - [Sponsored Listings](#sponsored-listings)
  - [Banner Auctions](#banner-auctions)
- [Advanced Features](#advanced-features)
  - [Event Context](#event-context)
  - [A/B Testing](#ab-testing)
  - [Quality Scores](#quality-scores)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [License](#license)

## Features

- **Event Tracking** - Impressions, clicks, purchases, and page views
- **Promoted & Organic** - Full attribution support for both content types
- **Banner Auctions** - Run auctions with comprehensive error handling and callbacks
- **A/B Testing** - Built-in experiment bucket support (`placementId`)
- **Quality Scores** - Pass product quality signals for auction optimization
- **Event Context** - Rich context with device type, channel, and page information
- **Offline Support** - Events are queued and sent when connectivity is restored
- **Java & Kotlin** - Full interoperability with both languages

## Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Java 11+
- `INTERNET` permission

## Installation

Add the dependency to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.topsort:topsort-kt:2.0.1'
}
```

Ensure Java 11 compatibility:

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

## Quick Start

Initialize the SDK in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Analytics.setup(
            context = this,
            opaqueUserId = "user-unique-id", // Consistent ID for the user
            bearerToken = "your-api-token"
        )
    }
}
```

Report a promoted click:

```kotlin
Analytics.reportClickPromoted(
    resolvedBidId = "bid-from-auction",
    placement = Placement(path = "/search")
)
```

## Event Tracking

### Impressions

Track when products are displayed to users.

**Promoted impression** (from auction winner):

```kotlin
Analytics.reportImpressionPromoted(
    resolvedBidId = "resolved-bid-id",
    placement = Placement(
        path = "/search/results",
        position = 1,
        page = 1,
        pageSize = 20
    ),
    // Optional context
    deviceType = Device.MOBILE,
    channel = Channel.ONSITE,
    page = Page.Factory.buildWithId(Page.TYPE_SEARCH, "electronics")
)
```

**Organic impression** (non-promoted content):

```kotlin
Analytics.reportImpressionOrganic(
    entity = Entity(id = "product-123", type = EntityType.PRODUCT),
    placement = Placement(path = "/category/electronics")
)
```

### Clicks

Track when users click on products.

**Promoted click**:

```kotlin
Analytics.reportClickPromoted(
    resolvedBidId = "resolved-bid-id",
    placement = Placement(path = "/search"),
    clickType = ClickType.PRODUCT,  // or LIKE, ADD_TO_CART
    deviceType = Device.MOBILE,
    channel = Channel.ONSITE
)
```

**Organic click**:

```kotlin
Analytics.reportClickOrganic(
    entity = Entity(id = "product-123", type = EntityType.PRODUCT),
    placement = Placement(path = "/home")
)
```

### Purchases

Track completed purchases with full item details:

```kotlin
Analytics.reportPurchase(
    id = "order-12345",
    items = listOf(
        PurchasedItem(
            productId = "product-123",
            quantity = 2,
            unitPrice = 1999,  // $19.99 in cents
            resolvedBidId = "bid-id-if-promoted",  // Optional
            vendorId = "vendor-456"  // Optional, for halo attribution
        )
    ),
    deviceType = Device.DESKTOP,
    channel = Channel.ONSITE
)
```

### Page Views

Track page/screen views for analytics:

```kotlin
Analytics.reportPageView(
    page = Page.Factory.buildWithId(
        type = Page.TYPE_PDP,
        pageId = "product-123"
    ),
    deviceType = Device.MOBILE,
    channel = Channel.ONSITE
)
```

**Page types**: `TYPE_HOME`, `TYPE_PDP`, `TYPE_SEARCH`, `TYPE_CATEGORY`, `TYPE_CHECKOUT`, `TYPE_OTHER`

## Running Auctions

### Sponsored Listings

Use `AuctionConfig` to run sponsored listing auctions:

```kotlin
// Create auction configuration
val config = AuctionConfig.ProductIds(
    numSlots = 3,
    ids = listOf("product-1", "product-2", "product-3"),
    opaqueUserId = "user-123",        // For targeting
    placementId = 5,                   // A/B test bucket (1-8)
    qualityScores = listOf(0.9, 0.8, 0.7)  // Optional quality signals
)

// Build and run the auction
val auction = Auction.fromConfig(config)
val request = AuctionRequest(listOf(auction))

lifecycleScope.launch(Dispatchers.IO) {
    try {
        val response = TopsortAuctionsHttpService.runAuctionsSync(request)

        response.results.forEach { result ->
            result.winners.forEach { winner ->
                println("Winner: ${winner.id}")
                println("Bid ID: ${winner.resolvedBidId}")
                println("Campaign: ${winner.campaignId}")  // Campaign attribution
            }
        }
    } catch (e: AuctionError) {
        handleError(e)
    }
}
```

**Other auction types**:

```kotlin
// Single category
AuctionConfig.CategorySingle(numSlots = 2, category = "electronics")

// Multiple categories
AuctionConfig.CategoryMultiple(numSlots = 2, categories = listOf("phones", "tablets"))

// Keyword search
AuctionConfig.Keyword(numSlots = 3, keyword = "wireless headphones")
```

### Banner Auctions

Use `BannerView` for banner ads with callbacks:

```kotlin
val bannerView = findViewById<BannerView>(R.id.banner_view)

// Configure callbacks
bannerView.onError { throwable ->
    Log.e("Banner", "Error loading banner", throwable)
}

bannerView.onAuctionError { error ->
    when (error) {
        is AuctionError.HttpError -> // Network error
        is AuctionError.DeserializationError -> // Parse error
        is AuctionError.EmptyResponse -> // No response
        else -> // Other errors
    }
}

bannerView.onNoWinners {
    // No ads available
    bannerView.visibility = View.GONE
}

bannerView.onImageLoad {
    // Banner loaded successfully
    bannerView.visibility = View.VISIBLE
}

// Run auction
val config = BannerConfig.LandingPage(
    slotId = "homepage-banner",
    ids = listOf("featured-product-1", "featured-product-2")
)

bannerView.setup(
    config = config,
    path = "/home",
    location = "hero-banner"
) { id, type ->
    // Handle click
    when (type) {
        EntityType.PRODUCT -> openProductPage(id)
        EntityType.VENDOR -> openVendorPage(id)
        else -> openUrl(id)
    }
}
```

## Advanced Features

### Event Context

Add rich context to all events:

```kotlin
// Device type
deviceType = Device.MOBILE    // or DESKTOP

// Channel
channel = Channel.ONSITE      // or OFFSITE, INSTORE

// Click type (for clicks only)
clickType = ClickType.PRODUCT // or LIKE, ADD_TO_CART

// Page context
page = Page.Factory.buildWithId(Page.TYPE_SEARCH, "query-id")
// or with multiple values
page = Page.Factory.buildWithValues(Page.TYPE_CATEGORY, listOf("electronics", "phones"))
```

### A/B Testing

Use `placementId` (1-8) to bucket users into experiments:

```kotlin
val config = AuctionConfig.ProductIds(
    numSlots = 3,
    ids = productIds,
    placementId = userBucket  // 1-8 based on user assignment
)
```

### Quality Scores

Pass quality signals to optimize auction results:

```kotlin
val config = AuctionConfig.ProductIds(
    numSlots = 3,
    ids = listOf("p1", "p2", "p3"),
    qualityScores = listOf(0.95, 0.82, 0.71)  // Must match ids size
)
```

## Error Handling

The SDK uses `AuctionError` sealed class for auction errors:

```kotlin
try {
    val response = TopsortAuctionsHttpService.runAuctionsSync(request)
} catch (e: AuctionError) {
    when (e) {
        is AuctionError.HttpError ->
            Log.e("Auction", "Network error", e.error)
        is AuctionError.DeserializationError ->
            Log.e("Auction", "Invalid response: ${String(e.data)}")
        is AuctionError.EmptyResponse ->
            Log.e("Auction", "Empty response from server")
        is AuctionError.SerializationError ->
            Log.e("Auction", "Failed to build request")
        is AuctionError.InvalidNumberAuctions ->
            Log.e("Auction", "Invalid auction count: ${e.count}")
    }
}
```

## Testing

Mock the auction service in tests:

```kotlin
@Before
fun setup() {
    TopsortAuctionsHttpService.setMockService(mockService)
}

@After
fun teardown() {
    TopsortAuctionsHttpService.resetToDefaultService()
}
```

## License

This library is licensed under the [MIT License](https://github.com/Topsort/topsort.kt/blob/main/LICENSE).

---

For more information, visit [topsort.com](https://www.topsort.com) or check the [API documentation](https://docs.topsort.com).
