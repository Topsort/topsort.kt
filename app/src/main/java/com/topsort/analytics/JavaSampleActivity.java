package com.topsort.analytics;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.topsort.analytics.model.Impression;
import com.topsort.analytics.model.Placement;
import com.topsort.analytics.model.PurchasedItem;

import java.util.Collections;

public class JavaSampleActivity extends ComponentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reportImpression();
        reportImpressionWithResolvedBidId();
        reportClick();
        reportClickWithResolvedBidId();
        reportPurchase();
        reportPurchaseWithResolvedBidId();
    }

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
                "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0="
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
                "p_SA0238",
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
                .reportClick(placement, "p_SA0238", null, null, null);
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
                        "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0="
                );
    }

    private void reportPurchase() {

        PurchasedItem item = new PurchasedItem(
                "p_SA0238",
                20,
                1295,
                null,
                null
        );

        Analytics
                .INSTANCE
                .reportPurchase(Collections.singletonList(item), "o:567-123");
    }

    private void reportPurchaseWithResolvedBidId() {
        PurchasedItem item = new PurchasedItem(
                "p_SA0238",
                20,
                1295,
                null,
                "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0="
        );

        Analytics
                .INSTANCE
                .reportPurchase(Collections.singletonList(item), "o:567-123");
    }
}
