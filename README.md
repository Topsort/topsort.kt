# Topsort Android SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.topsort/topsort-kt)](https://central.sonatype.com/artifact/com.topsort/topsort-kt)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/Topsort/topsort.kt/blob/main/LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![CI](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml/badge.svg)](https://github.com/Topsort/topsort.kt/actions/workflows/tests.yaml)
[![Coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Ftopsort.github.io%2Ftopsort.kt%2Fcoverage.json)](https://topsort.github.io/topsort.kt/)

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
  - [Displaying a Banner You Resolved Yourself](#displaying-a-banner-you-resolved-yourself)
- [Advanced Features](#advanced-features)
  - [Event Context](#event-context)
  - [A/B Testing](#ab-testing)
  - [Quality Scores](#quality-scores)
- [Java](#java)
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
    implementation 'com.topsort:topsort-kt:3.3.0' // x-release-please-version
}
```

Ensure Java 11 compatibility:

```gradle
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
```

## Quick Start

Initialize the SDK in your Application class:

```kotlin
import com.topsort.analytics.Analytics
import com.topsort.analytics.UserIdentity

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val userId = "user-unique-id" // your own id for this user, if you have one

        Analytics.setup(
            application = this,
            // Your own id for this user. Audience matching resolves it against your records,
            // so pass it whenever you have one - logged in or not. `of` turns a null or blank
            // id into UserIdentity.Unidentified, so the fallback is named rather than silent.
            identity = UserIdentity.of(userId),
            token = "your-api-token"
        )
        // Never have an id? Say so directly with UserIdentity.Unidentified. The SDK mints one
        // and keeps it across launches. A minted id is a persistent device-scoped pseudonymous
        // identifier, not anonymisation, and events under it will not audience-match:
        //
        //     Analytics.setup(this, UserIdentity.Unidentified, "your-api-token")
    }
}
```

`Analytics.opaqueUserId` returns the id events are actually reported under.

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
    page = Page.Factory.buildWithId(PageType.SEARCH, "electronics")
)
```

A promoted impression is reported at most once per `resolvedBidId` per session; repeats are
dropped with a warning. To report several impressions in one event - a list screen, say - build
them with `Impression.Factory` and pass them to `Analytics.reportImpressions(list)`.

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
        type = PageType.PDP,
        pageId = "product-123"
    ),
    deviceType = Device.MOBILE,
    channel = Channel.ONSITE
)
```

**Page types** (from `PageType` enum): `HOME`, `PDP`, `SEARCH`, `CATEGORY`, `CART`, `OTHER`

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
            ?: return@launch  // No response

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

lifecycleScope.launch {
    bannerView.setup(
        config = config,
        path = "/home",
        location = "hero-banner"
    ) { id, type ->
        // Handle click. `type` is com.topsort.analytics.model.auctions.EntityType, which also
        // has BRAND and URL - not the model.EntityType used for organic events.
        when (type) {
            EntityType.PRODUCT -> openProductPage(id)
            EntityType.VENDOR -> openVendorPage(id)
            else -> openUrl(id)
        }
    }
}
```

`suspend fun runBannerAuction(config)` runs the same single-slot auction without a view. It returns
null when there is no winner or the winner has no creative URL, and throws `AuctionError` on
failure.

### Displaying a Banner You Resolved Yourself

If you run the auction with your own HTTP stack, map the winner onto `BannerResponse` and let the
view do the rest - load the creative, report the impression once the banner is laid out, report the
click:

```kotlin
val winner = BannerResponse(
    id = "product-123",
    url = "https://cdn.example.com/creative.png",
    type = EntityType.PRODUCT,
    resolvedBidId = "resolved-bid-id",
)
bannerView.setup(winner, path = "/home", location = "hero-banner") { id, type -> openProductPage(id) }
```

No auction runs, so `onAuctionError` and `onNoWinners` are never invoked from this overload;
`onImageLoad` and `onError` still report the creative load.

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
page = Page.Factory.buildWithId(PageType.SEARCH, "query-id")
// or with multiple values
page = Page.Factory.buildWithValues(PageType.CATEGORY, listOf("electronics", "phones"))
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

## Java

`Analytics` is a Kotlin `object`, reached from Java through `Analytics.INSTANCE`. Its report
methods have no Java overloads, so every parameter is passed explicitly - `null` for the optional
ones:

```java
Analytics.INSTANCE.reportClickPromoted(
        resolvedBidId,
        Placement.Companion.build("/search"),
        null,   // opaqueUserId - falls back to the one given to setup
        null,   // id           - generated when null
        null,   // occurredAt   - now when null
        null,   // deviceType
        null,   // channel
        null,   // page
        null    // clickType
);
```

The sample app's `JavaSampleActivity.java` reports impressions, clicks and purchases from Java.

## Error Handling

Events the SDK gives up on - rejected by the API with a 4xx, evicted from a full cache, or
unreadable - are logged and dropped. To count that loss yourself, register a listener; it runs on
the SDK's worker thread, so keep it quick and capture no `Context`:

```kotlin
Analytics.eventDiscardListener = EventDiscardListener { reason, count ->
    metrics.increment("topsort.events.discarded", count, "reason" to reason.name)
}
```

The SDK uses `AuctionError` sealed class for auction errors:

```kotlin
try {
    val response = TopsortAuctionsHttpService.runAuctionsSync(request)
        ?: throw AuctionError.EmptyResponse

    // Use response...
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
