package com.topsort.analytics

import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Placement
import com.topsort.analytics.model.Session
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry

/**
 *  Instrumented test for the Cache class, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class CacheInstrumentedTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val session = Session("s_sessionId123")

    @Before
    fun setup(){
        Cache.setup(appContext, session.sessionId, "t_token123")
    }

    @Test
    fun testClickEvent(){
        val event = ClickEvent(
            session = session,
            placement = Placement("1"),
            id = "qwerty123",
        )

        val recordId = Cache.storeClick(event)
        val cachedEvent = Cache.readClick(recordId)

        assertNotNull(cachedEvent)
        assertEquals(event.session, cachedEvent!!.session)
        assertEquals(event.id, cachedEvent.id)
    }
}