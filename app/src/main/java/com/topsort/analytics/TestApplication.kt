package com.topsort.analytics

import android.app.Application

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val sessionId = "ebeaf802-6d0a-41a3-ae59-661887c4f6cb"

        Analytics.setup(
            application = this,
            opaqueUserId = sessionId,
            token = BuildConfig.TOKEN
        )
    }
}
