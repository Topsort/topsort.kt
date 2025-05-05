package com.topsort.example.testutil

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/**
 * Utility class to create a valid AttributeSet for testing purposes.
 */
object TestAttributeSet {
    /**
     * Creates a valid AttributeSet from a simple XML layout resource.
     * This method uses the built-in Android XML parser to create a proper AttributeSet instance.
     * 
     * @param context The context to use for accessing resources
     * @return A valid AttributeSet instance
     */
    fun create(context: Context): AttributeSet {
        val resourceId = android.R.layout.simple_list_item_1
        val parser = context.resources.getLayout(resourceId)
        
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Fallback if parsing fails
            return Xml.newPullParser().let {
                it.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                Xml.asAttributeSet(it)
            }
        }
        
        // Convert the parser to an AttributeSet
        return Xml.asAttributeSet(parser)
    }
}

/**
 * Extension function to safely get an XmlPullParser from a layout resource ID
 */
private fun Resources.getLayout(id: Int): XmlPullParser {
    return getXml(id)
} 