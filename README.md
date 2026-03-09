# Topsort kotlin library

An Android library for interacting with the Topsort APIs. We provide support for sending events and running banner auctions with comprehensive error handling and callback support.

Licensed under [MIT][1].

## Requirements

- Minimum Java version: 11
- Android SDK: 24+
- `INTERNET` permission (add to your `AndroidManifest.xml` if not already present)

## Installation / Getting started

We recommend installing the library via Gradle.
Simply add the dependency to your build.gradle file:

```gradle
dependencies {
    ...

    // Check Maven Central for the latest version:
    // https://central.sonatype.com/artifact/com.topsort/topsort-kt
    implementation 'com.topsort:topsort-kt:2.0.1'
}
```

Ensure your project is configured to use at least Java 11:

```gradle
android {
    // Other configurations...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = '11'
    }
}
```

The library is distributed through Maven central, which is usually included by default in your repositories.
You can also add it directly, if needed:

```gradle
repositories {
    mavenCentral()
}
```

## Usage/Examples

#### Setup

The following sample code shows how to setup the analytics library before reporting any event:

##### Kotlin

```kotlin
import android.app.Application
import com.topsort.analytics.Analytics

class KotlinApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Either generate a unique session id here or hash an existing
        // identifier. It should be consistent for
        // each user (impression, click, purchase).

        Analytics.setup(
            this,
            "sessionId",
            "bearerToken"
        )
    }
}
```

##### Java
```java
import android.app.Application;
import com.topsort.analytics.Analytics;

public class JavaApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        Analytics
                .INSTANCE
                .setup(this, "sessionId", "bearerToken");
    }
}

```

#### Reporting Events

The following samples show how to report different events after setting up.

The `Placement` constructor requires a `path` parameter (the URL path or deeplink for the current view). Other fields like `location`, `page`, `position`, `pageSize`, `productId`, `categoryIds`, and `searchQuery` are optional:

```kotlin
val placement = Placement(
    path = "/search/results",
    location = "position_1",
    page = 1,
    pageSize = 20
)
```

##### Promoted events (with resolvedBidId from auction)

```kotlin
fun reportPromotedImpression() {
    val placement = Placement(
        path = "/search/results",
        location = "position_1"
    )

    Analytics.reportImpressionPromoted(
        resolvedBidId = "<The bid id from the auction winner>",
        placement = placement
    )
}

fun reportPromotedClick() {
    val placement = Placement(
        path = "/search/results",
        location = "position_1"
    )

    Analytics.reportClickPromoted(
        resolvedBidId = "<The bid id from the auction winner>",
        placement = placement
    )
}
```

##### Organic events (with entity instead of resolvedBidId)

```kotlin
fun reportOrganicImpression() {
    val placement = Placement(
        path = "/search/results",
        location = "position_1"
    )

    Analytics.reportImpressionOrganic(
        entity = Entity(id = "productId", type = EntityType.PRODUCT),
        placement = placement
    )
}

fun reportOrganicClick() {
    val placement = Placement(
        path = "/search/results",
        location = "position_1"
    )

    Analytics.reportClickOrganic(
        entity = Entity(id = "productId", type = EntityType.PRODUCT),
        placement = placement
    )
}
```

##### Purchase events

```kotlin
fun reportPurchase() {
    val item = PurchasedItem(
        productId = "productId",
        quantity = 20,
        unitPrice = 1295, // price in cents ($12.95)
    )

    Analytics.reportPurchase(
        id = "orderId",
        items = listOf(item)
    )
}
```

## Banner Auctions with Error Handling and Callbacks

The library provides comprehensive support for banner auctions with robust error handling and callbacks:

### Kotlin

```kotlin
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView
import com.topsort.analytics.model.auctions.AuctionError
import com.topsort.analytics.model.auctions.EntityType

// In your Activity or Fragment
val bannerView = findViewById<BannerView>(R.id.banner_view)

// Configure error handling and callbacks
bannerView.onError { throwable: Throwable ->
    Log.e("BannerDemo", "Error loading banner", throwable)
    // Handle general errors
}

bannerView.onAuctionError { error: AuctionError ->
    when (error) {
        is AuctionError.HttpError -> Log.e("BannerDemo", "Network error", error)
        is AuctionError.DeserializationError -> Log.e("BannerDemo", "Failed to parse response", error)
        is AuctionError.EmptyResponse -> Log.e("BannerDemo", "Empty response from server")
        else -> Log.e("BannerDemo", "Other auction error", error)
    }
    // Handle auction-specific errors
}

bannerView.onNoWinners {
    Log.d("BannerDemo", "No winners for this auction")
    // Handle case when auction returns no winners
}

bannerView.onImageLoad {
    Log.d("BannerDemo", "Banner image loaded successfully")
    // Execute code after successful image load
}

// Run the auction
lifecycleScope.launch {
    // Configure and run the auction
    val config = BannerConfig.LandingPage(
        slotId = "your-slot-id", 
        ids = listOf("product-1", "product-2")
    )
    
    bannerView.setup(
        config = config,
        path = "product-page",
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

The library now provides detailed error handling through the AuctionError sealed class:

```kotlin
when (error) {
    is AuctionError.HttpError -> // Handle HTTP errors
    is AuctionError.DeserializationError -> // Handle deserialization errors
    is AuctionError.SerializationError -> // Handle serialization errors
    is AuctionError.EmptyResponse -> // Handle empty responses
    is AuctionError.InvalidNumberAuctions -> // Handle invalid auction count
}
```

## Testing Support

For testing, the library includes helper classes to mock auction services:

```kotlin
// In your test
val mockService = MockAuctionsHttpService()
TopsortAuctionsHttpService.setMockService(mockService)

// After test
TopsortAuctionsHttpService.resetToDefaultService()
```

For more details, refer to the code samples and API documentation.

[1]: https://github.com/Topsort/topsort.kt/blob/main/LICENSE
