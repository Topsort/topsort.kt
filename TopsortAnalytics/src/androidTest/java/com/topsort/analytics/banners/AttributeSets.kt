package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/**
 * BannerView's constructor takes an AttributeSet because it is meant to be inflated from XML.
 * Any real one will do here; this parses a stock platform layout and advances to its first
 * tag, which is where the attributes live.
 */
internal fun attributeSet(context: Context): AttributeSet {
    val parser = context.resources.getXml(android.R.layout.simple_list_item_1)
    var eventType = parser.eventType
    while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
        eventType = parser.next()
    }
    return Xml.asAttributeSet(parser)
}
