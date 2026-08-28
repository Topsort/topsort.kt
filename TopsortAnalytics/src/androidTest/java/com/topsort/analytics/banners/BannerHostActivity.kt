package com.topsort.analytics.banners

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.ScrollView

/** A scrollable page with a tall spacer, so a banner added below it starts below the fold. */
class BannerHostActivity : Activity() {

    lateinit var scrollView: ScrollView
    lateinit var column: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(View(this@BannerHostActivity), MATCH_PARENT, SPACER_HEIGHT_PX)
        }
        scrollView = ScrollView(this).apply { addView(column, MATCH_PARENT, MATCH_PARENT) }
        setContentView(scrollView)
    }

    companion object {
        const val SPACER_HEIGHT_PX = 4000
    }
}
