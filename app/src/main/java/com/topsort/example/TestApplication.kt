package com.topsort.example

import android.app.Application
import com.topsort.analytics.Analytics
import com.topsort.analytics.banners.BannerConfig

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val sessionId = "ebeaf802-6d0a-41a3-ae59-661887c4f6cb"

        Analytics.setup(
            application = this,
            sessionId = sessionId,
            token = BuildConfig.TOKEN
        )

        val config = BannerConfig.LandingPage(slotId = "app", ids = listOf("p1", "p2"))
        //  val banner = BannerView(context = , config = config)


    }
}
