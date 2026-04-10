package com.topsort.analytics.model

import android.util.Log
import com.topsort.analytics.core.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject

private const val LOG_TAG = "TopsortAnalytics"

/**
 * Represents the page context where an event occurred.
 *
 * Use the [Factory] methods to create instances:
 * - [Factory.build] for simple page types
 * - [Factory.buildWithId] for pages with an identifier
 * - [Factory.buildWithValues] for pages with multiple values (e.g., category paths)
 *
 * **JSON Serialization Note:** Both [value] (single string) and [values] (list) serialize
 * to the same JSON key `"value"`. The API distinguishes between them by JSON type:
 * - Single value: `"value": "search query"`
 * - Multiple values: `"value": ["Electronics", "Phones"]`
 * This is intentional per the Topsort API contract.
 */
data class Page private constructor(
    /**
     * The type of page. Typically one of: "home", "category", "PDP", "search", "cart", "other".
     * Use [PageType] enum values for type safety.
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
        // Log warning instead of crashing to follow "never crash host app" principle
        if (value != null && values != null) {
            Log.w(LOG_TAG, "Page created with both 'value' and 'values' set. They are mutually exclusive. 'values' will be used.")
        }
    }

    override fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("type", type)
            pageId?.let { put("pageId", it) }
            // Prefer values over value if both are set (shouldn't happen, but graceful degradation)
            when {
                values != null -> put("value", JSONArray(values))
                value != null -> put("value", value)
            }
        }
    }

    object Factory {

        /**
         * Build a page with just a type.
         *
         * @param type The page type (e.g., [PageType.HOME], [PageType.CART])
         */
        fun build(type: String): Page {
            return Page(type = type)
        }

        /**
         * Build a page with a [PageType] enum.
         *
         * @param type The page type enum
         */
        fun build(type: PageType): Page {
            return Page(type = type.value)
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
         * Build a page with a [PageType] enum and page ID.
         *
         * @param type The page type enum
         * @param pageId The page identifier
         * @param value Optional single value
         */
        @JvmOverloads
        fun buildWithId(
            type: PageType,
            pageId: String,
            value: String? = null,
        ): Page {
            return Page(
                type = type.value,
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

        /**
         * Build a page with a [PageType] enum and multiple values.
         *
         * @param type The page type enum
         * @param pageId Optional page identifier
         * @param values List of values (e.g., category path)
         */
        @JvmOverloads
        fun buildWithValues(
            type: PageType,
            values: List<String>,
            pageId: String? = null,
        ): Page {
            return Page(
                type = type.value,
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
}

/**
 * Type of page where an event occurred.
 */
enum class PageType(val value: String) {
    HOME("home"),
    CATEGORY("category"),
    /** Product Detail Page - uppercase per API contract */
    PDP("PDP"),
    SEARCH("search"),
    CART("cart"),
    OTHER("other");

    companion object {
        /**
         * Parse a page type from its string value.
         * Returns null for unrecognized values (graceful degradation).
         */
        fun fromValue(value: String?): PageType? {
            if (value == null) return null
            return entries.find { it.value == value }
        }
    }
}
