
# Topsort kotlin library

An Android library for interacting with the Topsort APIs. We currently only support sending events but will add more features shortly.

Licensed under [MIT][1].

## Installation / Getting started

We recommend installing the library via Gradle.
Simply add the dependency to your build.gradle file:

```gradle
dependencies {
    ...

    implementation 'com.topsort:topsort-kt:1.1.0'
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

##### Java
```Java
private void reportImpressionWithResolvedBidId() {
        Placement placement = new Placement(
                "search_results",
                null
        );

        Impression impression = new Impression(
                placement,
                null,
                null,
                null,
                "<The bid id from the auction winner>"

        );

        Analytics
                .INSTANCE
                .reportImpression(Collections.singletonList(impression));
    }

    private void reportImpression() {
        Placement placement = new Placement(
                "search_results",
                null
        );

        Impression impression = new Impression(
                placement,
                "productId",
                null,
                null,
                null
        );

        Analytics
                .INSTANCE
                .reportImpression(Collections.singletonList(impression));
    }

    private void reportClick() {
        Placement placement = new Placement(
                "search_results",
                null
        );

        Analytics
                .INSTANCE
                .reportClick(placement, "productId", null, null, null);
    }

    private void reportClickWithResolvedBidId() {
        Placement placement = new Placement(
                "search_results",
                null
        );

        Analytics
                .INSTANCE
                .reportClick(
                        placement,
                        null,
                        null,
                        null,
                        "<The bid id from the auction winner>"
                );
    }

    private void reportPurchase() {

        PurchasedItem item = new PurchasedItem(
                "productId",
                20,
                1295,
                null,
                null
        );

        Analytics
                .INSTANCE
                .reportPurchase(Collections.singletonList(item), "marketPlaceId");
    }

    private void reportPurchaseWithResolvedBidId() {
        PurchasedItem item = new PurchasedItem(
                "productId",
                20,
                1295,
                null,
                "<The bid id from the auction winner>"
        );

        Analytics
                .INSTANCE
                .reportPurchase(Collections.singletonList(item), "marketPlaceId");
    }
```

#### Banners on Android

#### Kotlin
You should first add the `BannerView` into your activity `xml`. You can do so with
Android Studio's visual editor, but the end file should like like the following
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <com.topsort.analytics.banners.BannerView
        android:id="@+id/bannerView"
        android:layout_width="353dp"
        android:layout_height="103dp"
        android:layout_marginTop="40dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/textView"
        tools:srcCompat="@tools:sample/backgrounds/scenic" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

Then, you have to call the `BannerView.setup()` function with you auction parameters.
Notice that since this makes network calls, we need to `launch` it in a co-routine.
```kotlin
class SampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_activity)

        this.lifecycleScope.launch {
            val bannerView = findViewById<BannerView>(R.id.bannerView)
            val bannerConfig =
                BannerConfig.CategorySingle(slotId = "slot", category = "category")
            bannerView.setup(
                bannerConfig,
                "sample_activity",
                null,
                { id, entityType -> onBannerClick(id, entityType) })
        }
    }
}
```


[1]: ./LICENSE
