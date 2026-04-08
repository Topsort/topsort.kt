package com.topsort.analytics.model

import com.topsort.analytics.core.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the page context where an event occurred.
 *
 * Use the [Factory] methods to create instances:
 * - [Factory.build] for simple page types
 * - [Factory.buildWithId] for pages with an identifier
 * - [Factory.buildWithValues] for pages with multiple values (e.g., category paths)
 */
data class Page private constructor(
    /**
     * The type of page. Typically one of: "home", "category", "PDP", "search", "cart", "other".
     */
    val type: String,

    /**
     * Optional page identifier.
     */
    val pageId: String? = null,

    /**
     * Optional single value associated with the page (e.g., search query, product ID).
     * Mutually exclusive with [values].
     */
    val value: String? = null,

    /**
     * Optional list of values associated with the page (e.g., category hierarchy).
     * Mutually exclusive with [value].
     */
    val values: List<String>? = null,
) : JsonSerializable {

    init {
        require(!(value != null && values != null)) {
            "Page cannot have both 'value' and 'values' set. They are mutually exclusive."
        }
    }

    override fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("type", type)
            pageId?.let { put("pageId", it) }
            value?.let { put("value", it) }
            values?.let { put("value", JSONArray(it)) }
        }
    }

    object Factory {

        /**
         * Build a page with just a type.
         *
         * @param type The page type (e.g., "home", "cart", "other")
         */
        fun build(type: String): Page {
            return Page(type = type)
        }

        /**
         * Build a page with a type and page ID.
         *
         * @param type The page type
         * @param pageId The page identifier
         * @param value Optional single value
         */
        @JvmOverloads
        fun buildWithId(
            type: String,
            pageId: String,
            value: String? = null,
        ): Page {
            return Page(
                type = type,
                pageId = pageId,
                value = value,
            )
        }

        /**
         * Build a page with multiple values (e.g., category hierarchy).
         *
         * @param type The page type
         * @param pageId Optional page identifier
         * @param values List of values (e.g., category path)
         */
        @JvmOverloads
        fun buildWithValues(
            type: String,
            values: List<String>,
            pageId: String? = null,
        ): Page {
            return Page(
                type = type,
                pageId = pageId,
                values = values,
            )
        }

        fun fromJsonObject(json: JSONObject): Page {
            val type = json.getString("type")
            val pageId = json.getStringOrNull("pageId")

            // Handle value which can be string or array
            val valueObj = json.opt("value")
            val value: String?
            val values: List<String>?

            when (valueObj) {
                is String -> {
                    value = valueObj
                    values = null
                }
                is JSONArray -> {
                    value = null
                    values = (0 until valueObj.length()).map { valueObj.getString(it) }
                }
                else -> {
                    value = null
                    values = null
                }
            }

            return Page(
                type = type,
                pageId = pageId,
                value = value,
                values = values,
            )
        }
    }

    companion object {
        /** Page type constants for convenience */
        const val TYPE_HOME = "home"
        const val TYPE_CATEGORY = "category"
        const val TYPE_PDP = "PDP"
        const val TYPE_SEARCH = "search"
        const val TYPE_CART = "cart"
        const val TYPE_OTHER = "other"
    }
}
