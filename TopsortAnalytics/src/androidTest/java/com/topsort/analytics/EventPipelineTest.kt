package com.topsort.analytics

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.topsort.analytics.core.Logger
import com.topsort.analytics.model.Click
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Purchase
import com.topsort.analytics.model.PurchaseEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
 class EventPipelineTest {

    lateinit var workManager: WorkManager

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)

        workManager = WorkManager.getInstance(appContext)

        EventPipeline.setup(appContext)
        Logger.log.clear()
    }

    @Test
    fun impressions_are_batched() {
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
            val job1 = EventPipeline.storeImpression(
                ImpressionEvent(impressions1),
                shouldFlush = false
            )
            val job2 = EventPipeline.storeImpression(
                ImpressionEvent(impressions2),
                shouldFlush = false
            )

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readImpressions()
            val storedDeserialized = Impression.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(impressions1 + impressions2)
        }
    }

    @Test
    fun clicks_are_batched() {
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
            val job1 = EventPipeline.storeClick(
                ClickEvent(clicks1),
                shouldFlush = false
            )
            val job2 = EventPipeline.storeClick(
                ClickEvent(clicks2),
                shouldFlush = false
            )

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readClicks()
            val storedDeserialized = Click.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(clicks1 + clicks2)
        }
    }

    @Test
    fun purchases_are_batched() {
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
            val job1 = EventPipeline.storePurchase(
                PurchaseEvent(purchases1),
                shouldFlush = false
            )
            val job2 = EventPipeline.storePurchase(
                PurchaseEvent(purchases2),
                shouldFlush = false
            )

            // Make sure they're persisted
            job1.join()
            job2.join()

            val storedStr = EventPipeline.readPurchases()
            val storedDeserialized = Purchase.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(storedDeserialized).containsExactlyInAnyOrderElementsOf(purchases1 + purchases2)
        }
    }

    @Test
    fun aggregate_joins_Events() {
        val impressions = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        val clicks = listOf(
            getClickPromoted(),
            getClickOrganic(),
        )

        val purchases = listOf(
            getRandomPurchase(),
            getRandomPurchase(),
        )

        runBlocking {
            EventPipeline.clear()
            val aggregated = Event(
                impressions = impressions,
                clicks = clicks,
                purchases = purchases
            )
            val job1 = EventPipeline.storeImpression(
                ImpressionEvent(impressions),
                shouldFlush = false
            )
            val job2 = EventPipeline.storeClick(
                ClickEvent(clicks),
                shouldFlush = false
            )
            val job3 = EventPipeline.storePurchase(
                PurchaseEvent(purchases),
                shouldFlush = false
            )

            // Make sure they're persisted
            job1.join()
            job2.join()
            job3.join()

            val storedEvent = EventPipeline.aggregateEvents()

            assertThat(aggregated).isEqualTo(storedEvent)
        }
    }

    @Test
    fun events_are_uploaded() {
        val impressions1 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )
        val impressions2 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        val aggregated = Event(
            impressions = impressions1 + impressions2,
        )

        runBlocking {
            EventPipeline.clear()

            EventPipeline.storeImpression(ImpressionEvent(impressions1), shouldFlush = false)
            EventPipeline.storeImpression(ImpressionEvent(impressions2))
                .join()

            // Wait for the upload work to be done
            while (
                workManager.getWorkInfos(WorkQuery.fromUniqueWorkNames("UPLOAD"))
                    .get().first().state != WorkInfo.State.SUCCEEDED
            ) {
                yield()
            }

        }

        assertThat(Logger.log).contains("uploading: ${aggregated.toJsonObject()}")
    }
}
