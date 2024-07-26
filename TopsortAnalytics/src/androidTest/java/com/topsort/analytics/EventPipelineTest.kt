package com.topsort.analytics

import com.topsort.analytics.model.Impression
import com.topsort.analytics.model.ImpressionEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.Test

 class EventPipelineTest {

    @Test
    fun `impressions are stored`() {
        val ep = EventPipeline
        val impressions = listOf(
            getImpressionPromoted(),
            getImpressionOrganic()
        )

        ep.storeImpression(ImpressionEvent(impressions))

        runBlocking {
            val storedStr = ep.readImpressions()
            val storedDeserialized = Impression.Factory.fromJsonArray(JSONArray("[$storedStr]"))

            assertThat(impressions).isEqualTo(storedDeserialized)
        }
    }
}
