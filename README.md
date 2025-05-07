# Topsort kotlin library

An Android library for interacting with the Topsort APIs. We provide support for sending events and running banner auctions with comprehensive error handling and callback support.

Licensed under [MIT][1].

## Requirements

- Minimum Java version: 17
- Android SDK: 24+

## Installation / Getting started

We recommend installing the library via Gradle.
Simply add the dependency to your build.gradle file:

```gradle
dependencies {
    ...

    implementation 'com.topsort:topsort-kt:2.0.0'
}
```

Ensure your project is configured to use Java 17:

```gradle
android {
    // Other configurations...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
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

The following samples show how report different events after setting up:

##### Kotlin
```kotlin
fun reportPurchase() {
    val item = PurchasedItem(
        resolvedBidId = "<The bid id from the auction winner>",
        productId = "productId",
        unitPrice = 1295,
        quantity = 20
    )

    Analytics.reportPurchase(
        id = "marketPlaceId",
        items = listOf(item)
    )
}

fun reportClick() {
    val placement = Placement(
        page = "search_results",
        location = "position_1"
    )

    Analytics.reportClick(
        placement = placement,
        productId = "productId"
    )
}

fun reportClick() {
    val placement = Placement(
        page = "search_results",
        location = "position_1"
    )

    Analytics.reportClickWithResolvedBidId(
        resolvedBidId = "<The bid id from the auction winner>",
        placement = placement
    )
}

fun reportImpression() {
    val placement = Placement(
        page = "search_results",
        location = "position_1"
    )

    val impression = Impression(
        productId = "productId",
        placement = placement
    )

    Analytics.reportImpression(listOf(impression))
}

fun reportImpressionWithResolvedBidId() {
    val placement = Placement(
        page = "search_results",
        location = "position_1"
    )

    Analytics.reportImpressionWithResolvedBidId(
        resolvedBidId = "<The bid id from the auction winner>",
        placement = placement
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
            EntityType.CATEGORY -> openCategoryPage(id)
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
