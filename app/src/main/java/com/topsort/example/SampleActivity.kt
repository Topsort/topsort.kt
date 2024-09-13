package com.topsort.example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.topsort.analytics.Analytics
import com.topsort.analytics.banners.BannerConfig
import com.topsort.analytics.banners.BannerView
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.PurchasedItem
import kotlinx.coroutines.launch
import com.topsort.analytics.model.auctions.EntityType as BannerEntityType

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

        reportPurchaseWithResolvedBidId()
        reportClickWithResolvedBidId()
        reportImpressionWithResolvedBidId()

        reportPurchase()
        reportClick()
        reportImpression()
    }


    private fun reportPurchaseWithResolvedBidId() {
        val item = PurchasedItem(
            resolvedBidId = "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0=",
            productId = "p_SA0238",
            unitPrice = 1295,
            quantity = 20
        )

        Analytics.reportPurchase(
            id = "o:567-123",
            items = listOf(item)
        )
    }

    private fun reportPurchase() {
        val item = PurchasedItem(
            productId = "p_SA0238",
            unitPrice = 1295,
            quantity = 20
        )

        Analytics.reportPurchase(
            items = listOf(item),
            id = "o:567-123",
        )
    }

    private fun reportClick() {
        val placement = Placement(
            path = "search_results",
            location = "position_1",
        )

        Analytics.reportClickOrganic(
            placement = placement,
            entity = Entity(id = "p_SA0238", type = EntityType.PRODUCT),
        )
    }

    private fun reportClickWithResolvedBidId() {
        val placement = Placement(
            path = "search_results",
            location = "position_1"
        )

        Analytics.reportClickPromoted(
            resolvedBidId = "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0=",
            placement = placement,
            id = "p_SA0238",
        )
    }

    private fun reportImpression() {
        val placement = Placement(
            path = "search_results",
            location = "position_1"
        )

        Analytics.reportImpressionOrganic(
            id = "p_SA0238",
            placement = placement,
            entity = Entity(id = "p_SA0238", type = EntityType.PRODUCT),
        )
    }

    private fun reportImpressionWithResolvedBidId() {
        val placement = Placement(
            path = "search_results",
            location = "position_1"
        )

        Analytics.reportImpressionPromoted(
            resolvedBidId = "WyJiX01mazE1IiwiMTJhNTU4MjgtOGVhZC00Mjk5LTgzMjctY2ViYjAwMmEwZmE4IiwibGlzdGluZ3MiLCJkZWZhdWx0IiwiIl0=",
            placement = placement,
            id = "marketPlaceImpressionId"
        )
    }
}

fun onBannerClick(id: String, entityType: BannerEntityType) {
    Log.i("BannerClick", "Clicked banner for $entityType with id $id")

}