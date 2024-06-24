
# Topsort kotlin library

An Android library for interacting with the Topsort APIs. We currently only support sending events but will add more features shortly.

Licensed under [MIT][1].

## Installation / Getting started

- Clone the repository from github: `git clone https://github.com/topsort/topsort.kt.git`
- From Android Studio, import the module: `File -> New -> Import Module`.
- A popup window will open with the title: Import Module From Source
- Select the source directory of the downloaded library named: `TopsortAnalytics`, and click finish
- Right click on the application project module, from the menu select `Open module Settings`
- From the left side of the displayed panel select `dependencies`
- Under `Modules` select the application module
- Under `Declared Dependencies`, click the `+` button, and select `Module Dependency`
- A popup will show up, select `TopsortAnalytics`, and at the bottom of the dialog select the type of configuration as `implementation`, then click ok

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

[1]: ./LICENSE
