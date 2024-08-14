package com.topsort.analytics;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.topsort.analytics.model.Entity;
import com.topsort.analytics.model.EntityType;
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
        Placement placement = Placement.Companion.build(
                "search_results",
                null
        );

        String resolvedBidId = "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0=";

        Analytics
                .INSTANCE
                .reportImpressionPromoted(
                        resolvedBidId,
                        placement,
                        null,
                        null,
                        null
                );
    }

    private void reportImpression() {
        Placement placement = Placement.Companion.build(
                "search_results",
                null
        );

        Analytics
                .INSTANCE
                .reportImpressionOrganic(
                        new Entity("p_SA0238", EntityType.PRODUCT),
                        placement,
                        null,
                        null,
                        null
                );
    }

    private void reportClick() {
        Placement placement = Placement.Companion.build(
                "search_results",
                null
        );

        Analytics
                .INSTANCE
                .reportClickOrganic(
                        new Entity("p_SA0238", EntityType.PRODUCT),
                        placement,
                        null,
                        null,
                        null
                );
    }

    private void reportClickWithResolvedBidId() {
        Placement placement = Placement.Companion.build(
                "search_results",
                null
        );

        String resolvedBidId = "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0=";

        Analytics
                .INSTANCE
                .reportClickPromoted(
                        resolvedBidId,
                        placement,
                        null,
                        null,
                        null
                );
    }

    private void reportPurchase() {

        PurchasedItem item = new PurchasedItem(
                "p_SA0238",
                20,
                1295,
                null
        );

        Analytics
                .INSTANCE
                .reportPurchase(
                        Collections.singletonList(item),
                        "o:567-123",
                        null,
                        null
                );
    }

    private void reportPurchaseWithResolvedBidId() {
        PurchasedItem item = new PurchasedItem(
                "p_SA0238",
                20,
                1295,
                "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0="
        );

        Analytics
                .INSTANCE
                .reportPurchase(
                        Collections.singletonList(item),
                        "o:567-123",
                        null,
                        null
                );
    }
}
