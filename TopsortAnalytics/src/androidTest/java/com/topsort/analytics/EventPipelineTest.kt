package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
 class EventPipelineTest {


    @Test
    fun impressions_are_batched() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        EventPipeline.setup(appContext)

        val impressions1 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )
        val impressions2 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        runBlocking {
            EventPipeline.clear()
            val job1 = EventPipeline.storeImpression(ImpressionEvent(impressions1))
            val job2 = EventPipeline.storeImpression(ImpressionEvent(impressions2))

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readImpressions()
            val storedDeserialized = Impression.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(impressions1+impressions2)
        }
    }

    @Test
    fun clicks_are_batched() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        EventPipeline.setup(appContext)

        val clicks1 = listOf(
            getClickPromoted(),
            getClickOrganic(),
        )
        val clicks2 = listOf(
            getClickPromoted(),
            getClickOrganic(),
        )

        runBlocking {
            EventPipeline.clear()
            val job1 = EventPipeline.storeClick(ClickEvent(clicks1))
            val job2 = EventPipeline.storeClick(ClickEvent(clicks2))

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readClicks()
            val storedDeserialized = Click.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(clicks1+clicks2)
        }
    }

    @Test
    fun purchases_are_batched() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        EventPipeline.setup(appContext)

        val purchases1 = listOf(
            getRandomPurchase(),
            getRandomPurchase(),
        )
        val purchases2 = listOf(
            getRandomPurchase(),
            getRandomPurchase(),
        )

        runBlocking {
            EventPipeline.clear()
            val job1 = EventPipeline.storePurchase(PurchaseEvent(purchases1))
            val job2 = EventPipeline.storePurchase(PurchaseEvent(purchases2))

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readPurchases()
            val storedDeserialized = Purchase.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(purchases1+purchases2)
        }
    }
}
