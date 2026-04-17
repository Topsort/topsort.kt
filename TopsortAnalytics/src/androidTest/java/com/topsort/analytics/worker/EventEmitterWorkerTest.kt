package com.topsort.analytics.worker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.topsort.analytics.Cache
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
        Cache.setup(context, "test-user", "test-token")
        mockService = MockAnalyticsService()
        TopsortAnalyticsHttpService.setMockService(mockService)
    }

    @After
    fun teardown() {
        TopsortAnalyticsHttpService.resetToDefaultService()
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
    }

    @Test
    fun doWork_impression_nonexistent_returns_success() {
        val inputData = buildInputData(999999L, EventType.Impression)
        val worker = buildWorker(inputData)
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
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

    private fun buildWorker(inputData: Data): EventEmitterWorker {
        return TestListenableWorkerBuilder<EventEmitterWorker>(context)
            .setInputData(inputData)
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

        override fun reportImpression(impressionEvent: ImpressionEvent): HttpResponse {
            return mockResponse()
        }

        override fun reportClick(clickEvent: ClickEvent): HttpResponse {
            return mockResponse()
        }

        override fun reportPurchase(purchaseEvent: PurchaseEvent): HttpResponse {
            return mockResponse()
        }

        override fun reportPageView(pageViewEvent: PageViewEvent): HttpResponse {
            return mockResponse()
        }

        override fun reportEvent(event: Event): HttpResponse {
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
