package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topsort.analytics.core.eventNow
import com.topsort.analytics.core.randomId
import com.topsort.analytics.model.Entity
import com.topsort.analytics.model.EntityType
import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.Placement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
 class EventPipelineTest {


    @Test
    fun impressions_are_stored() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        EventPipeline.setup(appContext)
        EventPipeline.clear()

        val impressions1 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )
        val impressions2 = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        runBlocking {
            EventPipeline.storeImpression(ImpressionEvent(impressions1))
            delay(100)
            EventPipeline.storeImpression(ImpressionEvent(impressions2))
            delay(100)

            val storedStr = EventPipeline.readImpressions()
            val storedDeserialized = Impression.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(impressions1+impressions2).isEqualTo(storedDeserialized)
            println(storedStr)

            EventPipeline.upload()
            delay(100)
            EventPipeline.storeImpression(ImpressionEvent(impressions1))
            delay(100)
            EventPipeline.upload()
            delay(100)
        }
    }
}
