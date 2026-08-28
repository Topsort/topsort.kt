package com.topsort.analytics.banners

import android.content.Context
import android.util.AttributeSet
import android.util.Xml
import com.topsort.analytics.model.auctions.EntityType
import org.xmlpull.v1.XmlPullParser

/**
 * Constructs BannerView the way XML inflation does, with a real AttributeSet. Any real one will do;
 * this parses a stock platform layout and advances to its first tag, which is where the
 * attributes live. The code-created shape, BannerView(context), is covered separately.
 */
internal fun attributeSet(context: Context): AttributeSet {
    val parser = context.resources.getXml(android.R.layout.simple_list_item_1)
    var eventType = parser.eventType
    while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
        eventType = parser.next()
    }
    return Xml.asAttributeSet(parser)
}

/** A winner with a stand-in creative; the URL never loads, which none of these tests need. */
internal fun bannerWinner(resolvedBidId: String) = BannerResponse(
    id = "p_SA0238",
    type = EntityType.PRODUCT,
    url = "https://example.invalid/creative.png",
    resolvedBidId = resolvedBidId,
)
