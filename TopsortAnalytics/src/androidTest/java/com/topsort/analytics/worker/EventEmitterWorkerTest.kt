package com.topsort.analytics.worker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.topsort.analytics.Analytics
import com.topsort.analytics.Cache
import com.topsort.analytics.DiscardReason
import com.topsort.analytics.EventDiscardListener
import com.topsort.analytics.UserIdentity
import com.topsort.analytics.core.HttpResponse
import com.topsort.analytics.getTestClickEvent
import com.topsort.analytics.getTestImpressionEvent
import com.topsort.analytics.getTestPageViewEvent
import com.topsort.analytics.getTestPurchaseEvent
import com.topsort.analytics.model.ClickEvent
import com.topsort.analytics.model.Event
import com.topsort.analytics.model.EventType
import com.topsort.analytics.model.ImpressionEvent
import com.topsort.analytics.model.PageViewEvent
import com.topsort.analytics.model.PurchaseEvent
import com.topsort.analytics.service.TopsortAnalyticsHttpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventEmitterWorkerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var mockService: MockAnalyticsService

    @Before
    fun setup() {
        Cache.setup(context, UserIdentity.of("test-user"), "test-token")
        mockService = MockAnalyticsService()
        TopsortAnalyticsHttpService.setMockService(mockService)
    }

    @After
    fun teardown() {
        Analytics.eventDiscardListener = null
        TopsortAnalyticsHttpService.resetToDefaultService()
        // The 5xx and exception tests deliberately leave their record cached - that is the
        // assertion. Without this the class finishes having written undelivered records and an
        // advanced id counter into the real store, and the next class either happens to clear it
        // or happens not to care. Isolation by coincidence, and it breaks order-dependently.
        Cache.clearForTests()
    }

    // ==================== Invalid input tests ====================

    @Test
    fun doWork_with_invalid_record_id_returns_success() {
        val inputData = Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, -1)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, EventType.Impression.ordinal)
            .build()

        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Asserts nothing was sent. Note this does NOT pin the recordId < 0 clause of the input
        // guard: with a negative id, removing that clause just falls through to the record-absent
        // branch, which also sends nothing and also returns success. Verified by deleting the
        // clause - this test still passes. The clause is defensive and unobservable from here; what
        // this test does pin is that a negative id never reaches the wire.
        assertThat(mockService.lastMethod).isNull()
    }

    @Test
    fun doWork_with_invalid_event_type_returns_success() {
        val inputData = Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, 1)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, -1)
            .build()

        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_with_missing_data_returns_success() {
        val inputData = Data.Builder().build()

        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_with_out_of_range_event_type_ordinal_returns_success() {
        val inputData = Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, 1)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, 999) // Out of range
            .build()

        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    // ==================== Impression tests ====================

    @Test
    fun doWork_impression_success_deletes_event_from_cache() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.responseCode = 200

        val inputData = buildInputData(recordId, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(Cache.readImpression(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportImpression")
    }

    @Test
    fun doWork_impression_4xx_error_returns_failure_and_deletes_event() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.responseCode = 400

        val inputData = buildInputData(recordId, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(Cache.readImpression(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportImpression")
    }

    @Test
    fun doWork_impression_5xx_error_returns_retry() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.responseCode = 500

        val inputData = buildInputData(recordId, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        // Event should NOT be deleted on transient failure
        assertThat(Cache.readImpression(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportImpression")
    }

    @Test
    fun doWork_impression_429_returns_retry_and_keeps_event() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.responseCode = 429

        val result = buildWorker(buildInputData(recordId, EventType.Impression)).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(Cache.readImpression(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportImpression")
    }

    @Test
    fun doWork_impression_408_returns_retry_and_keeps_event() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.responseCode = 408

        val result = buildWorker(buildInputData(recordId, EventType.Impression)).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(Cache.readImpression(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportImpression")
    }

    @Test
    fun doWork_5xx_before_the_last_attempt_still_retries() {
        val recordId = Cache.storeImpression(getTestImpressionEvent())
        mockService.responseCode = 500

        val result = buildWorker(buildInputData(recordId, EventType.Impression), attempt = 3).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun doWork_5xx_on_the_last_attempt_returns_failure_and_keeps_event() {
        val recordId = Cache.storeImpression(getTestImpressionEvent())
        mockService.responseCode = 500

        val result = buildWorker(buildInputData(recordId, EventType.Impression), attempt = 4).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        // Left for the next sweep, not discarded.
        assertThat(Cache.readImpression(recordId)).isNotNull
    }

    @Test
    fun a_4xx_discard_notifies_the_listener_with_its_reason() {
        val discards = mutableListOf<Pair<DiscardReason, Int>>()
        Analytics.eventDiscardListener = EventDiscardListener { reason, count -> discards += reason to count }
        val recordId = Cache.storeImpression(getTestImpressionEvent())
        mockService.responseCode = 400

        buildWorker(buildInputData(recordId, EventType.Impression)).doWork()

        assertThat(discards).containsExactly(DiscardReason.PERMANENTLY_REJECTED to 1)
    }

    @Test
    fun a_throwing_listener_does_not_stop_the_discard() {
        var notified = false
        Analytics.eventDiscardListener = EventDiscardListener { _, _ ->
            notified = true
            error("host bug")
        }
        val recordId = Cache.storeImpression(getTestImpressionEvent())
        mockService.responseCode = 400

        val result = buildWorker(buildInputData(recordId, EventType.Impression)).doWork()

        assertThat(notified).isTrue()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(Cache.readImpression(recordId)).isNull()
    }

    @Test
    fun doWork_impression_nonexistent_returns_success() {
        val inputData = buildInputData(999999L, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Not just "returned success" - success has five sources in doWork() (input guard,
        // age cap, record absent, unparseable body, real send). Without this the test passes
        // whichever one fired, including a genuine delivery.
        assertThat(mockService.lastMethod).isNull()
    }

    // ==================== Click tests ====================

    @Test
    fun doWork_click_success_deletes_event_from_cache() {
        val event = getTestClickEvent()
        val recordId = Cache.storeClick(event)
        mockService.responseCode = 201

        val inputData = buildInputData(recordId, EventType.Click)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(Cache.readClick(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportClick")
    }

    @Test
    fun doWork_click_4xx_error_returns_failure_and_deletes_event() {
        val event = getTestClickEvent()
        val recordId = Cache.storeClick(event)
        mockService.responseCode = 422

        val inputData = buildInputData(recordId, EventType.Click)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(Cache.readClick(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportClick")
    }

    @Test
    fun doWork_click_5xx_error_returns_retry() {
        val event = getTestClickEvent()
        val recordId = Cache.storeClick(event)
        mockService.responseCode = 503

        val inputData = buildInputData(recordId, EventType.Click)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(Cache.readClick(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportClick")
    }

    @Test
    fun doWork_click_nonexistent_returns_success() {
        val inputData = buildInputData(999998L, EventType.Click)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Not just "returned success" - success has five sources in doWork() (input guard,
        // age cap, record absent, unparseable body, real send). Without this the test passes
        // whichever one fired, including a genuine delivery.
        assertThat(mockService.lastMethod).isNull()
    }

    // ==================== Purchase tests ====================

    @Test
    fun doWork_purchase_success_deletes_event_from_cache() {
        val event = getTestPurchaseEvent()
        val recordId = Cache.storePurchase(event)
        mockService.responseCode = 200

        val inputData = buildInputData(recordId, EventType.Purchase)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(Cache.readPurchase(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportPurchase")
    }

    @Test
    fun doWork_purchase_4xx_error_returns_failure_and_deletes_event() {
        val event = getTestPurchaseEvent()
        val recordId = Cache.storePurchase(event)
        mockService.responseCode = 401

        val inputData = buildInputData(recordId, EventType.Purchase)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(Cache.readPurchase(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportPurchase")
    }

    @Test
    fun doWork_purchase_5xx_error_returns_retry() {
        val event = getTestPurchaseEvent()
        val recordId = Cache.storePurchase(event)
        mockService.responseCode = 502

        val inputData = buildInputData(recordId, EventType.Purchase)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(Cache.readPurchase(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportPurchase")
    }

    @Test
    fun doWork_purchase_nonexistent_returns_success() {
        val inputData = buildInputData(999997L, EventType.Purchase)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Not just "returned success" - success has five sources in doWork() (input guard,
        // age cap, record absent, unparseable body, real send). Without this the test passes
        // whichever one fired, including a genuine delivery.
        assertThat(mockService.lastMethod).isNull()
    }

    // ==================== PageView tests ====================

    @Test
    fun doWork_pageview_success_deletes_event_from_cache() {
        val event = getTestPageViewEvent()
        val recordId = Cache.storePageView(event)
        mockService.responseCode = 200

        val inputData = buildInputData(recordId, EventType.PageView)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(Cache.readPageView(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportPageView")
    }

    @Test
    fun doWork_pageview_4xx_error_returns_failure_and_deletes_event() {
        val event = getTestPageViewEvent()
        val recordId = Cache.storePageView(event)
        mockService.responseCode = 404

        val inputData = buildInputData(recordId, EventType.PageView)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(Cache.readPageView(recordId)).isNull()
        assertThat(mockService.lastMethod).isEqualTo("reportPageView")
    }

    @Test
    fun doWork_pageview_5xx_error_returns_retry() {
        val event = getTestPageViewEvent()
        val recordId = Cache.storePageView(event)
        mockService.responseCode = 500

        val inputData = buildInputData(recordId, EventType.PageView)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat(Cache.readPageView(recordId)).isNotNull
        assertThat(mockService.lastMethod).isEqualTo("reportPageView")
    }

    @Test
    fun doWork_pageview_nonexistent_returns_success() {
        val inputData = buildInputData(999996L, EventType.PageView)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Not just "returned success" - success has five sources in doWork() (input guard,
        // age cap, record absent, unparseable body, real send). Without this the test passes
        // whichever one fired, including a genuine delivery.
        assertThat(mockService.lastMethod).isNull()
    }

    // ==================== Exception handling tests ====================

    @Test
    fun doWork_exception_returns_retry() {
        val event = getTestImpressionEvent()
        val recordId = Cache.storeImpression(event)
        mockService.shouldThrowException = true

        val inputData = buildInputData(recordId, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        // Event should NOT be deleted on exception
        assertThat(Cache.readImpression(recordId)).isNotNull
    }

    // ==================== Helper methods ====================

    private fun buildWorker(inputData: Data, attempt: Int = 0): EventEmitterWorker {
        return TestListenableWorkerBuilder<EventEmitterWorker>(context)
            .setInputData(inputData)
            .setRunAttemptCount(attempt)
            .build()
    }

    private fun buildInputData(recordId: Long, eventType: EventType): Data {
        return Data.Builder()
            .putLong(EventEmitterWorker.EXTRA_RECORD_ID, recordId)
            .putInt(EventEmitterWorker.EXTRA_EVENT_TYPE, eventType.ordinal)
            .build()
    }

    /**
     * Mock implementation of the analytics service for testing
     */
    private class MockAnalyticsService : TopsortAnalyticsHttpService.Service {
        var responseCode: Int = 200
        var shouldThrowException: Boolean = false
        var lastMethod: String? = null

        override fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse {
            lastMethod = "reportImpression"
            return mockResponse()
        }

        override fun reportClick(clickEvent: ClickEvent): HttpResponse {
            lastMethod = "reportClick"
            return mockResponse()
        }

        override fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse {
            lastMethod = "reportPurchase"
            return mockResponse()
        }

        override fun reportPageView(pageViewEvent: PageViewEvent): HttpResponse {
            lastMethod = "reportPageView"
            return mockResponse()
        }

        override fun reportEvent(event: Event): HttpResponse {
            lastMethod = "reportEvent"
            return mockResponse()
        }

        private fun mockResponse(): HttpResponse {
            if (shouldThrowException) {
                throw RuntimeException("Mock network exception")
            }
            return HttpResponse(
                code = responseCode,
                message = if (responseCode in 200..299) "OK" else "Error",
                body = null
            )
        }
    }
}
