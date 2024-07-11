package com.topsort.analytics.model.events

import com.topsort.analytics.core.getIntOrNull
import com.topsort.analytics.core.getStringListOrNull
import com.topsort.analytics.core.getStringOrNull
import org.json.JSONObject

data class Placement(

    /**
     * URL path of the page triggering the event.
     *
     * For web apps, this can be obtained in JS using window.location.pathname.
     *
     * For mobile apps, use the deep link for the current view, if available.
     * Otherwise, encode the view from which the event occurred in your app as a path-like string (e.g. /root/categories/:categoryId).
     */
    val path: String,

    /**
     * For components with multiple items (i.e. search results, similar products, etc), this should indicate the index of a given item within that list.
     */
    val position: Int? = null,

    /**
     * For paginated pages, this should indicate which page number triggered the event.
     */
    val page: Int? = null,

    /**
     * For paginated pages this should indicate how many items are in each result page.
     */
    val pageSize: Int? = null,

    /**
     * The ID of the product associated to the page in which this event occurred, if applicable.
     * This ID must match the ID provided through the catalog service.
     */
    val productId: String? = null,

    /**
     * An array of IDs of the categories associated to the page in which this event occurred, if applicable.
     * These IDs must match the IDs provided through the catalog service.
     */
    val categoryIds: List<String>? = null,

    /**
     * The search string provided by the user in the page where this event occurred, if applicable.
     * This search string must match the searchQuery field that was provided in the auction request (if provided).
     */
    val searchQuery : String? = null,

    /**
     * A marketplace defined name for a page part
     */
    val location: String? = null
) {

    fun toJsonObject(): JSONObject {
        return JSONObject()
            .put("path", path)
            .put("position",position)
            .put("page", page)
            .put("pageSize", pageSize)
            .put("productId", productId)
            .put("categoryIds", categoryIds)
            .put("searchQuery", searchQuery)
            .put("location", location)
    }

    companion object {

        @JvmOverloads
        fun build(
            path: String,
            position: Int? = null,
            page: Int? = null,
            pageSize: Int? = null,
            productId: String? = null,
            categoryIds: List<String>? = null,
            searchQuery: String? = null,
            location: String? = null,
        ): Placement {
            return Placement(
                path, position, page, pageSize, productId, categoryIds, searchQuery, location
            )
        }

        fun fromJsonObject(json: JSONObject): Placement {
            return Placement(
                path = json.getString("path"),
                position = json.getIntOrNull("position"),
                page = json.getIntOrNull("page"),
                pageSize = json.getIntOrNull("pageSize"),
                productId = json.getStringOrNull("productId"),
                categoryIds = json.getStringListOrNull("categoryIds"),
                searchQuery = json.getStringOrNull("searchQuery"),
                location = json.getStringOrNull("location"),
            )
        }
    }
}


