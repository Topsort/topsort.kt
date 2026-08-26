package com.topsort.analytics

/**
 * Who the events reported after [Analytics.setup] belong to.
 *
 * This exists so that "I have no identifier for this user" has to be said out loud. The identity
 * used to be a plain `String` in which a blank value silently meant "mint one and persist it".
 * Nothing in that signature said blank was special, and the most common way to produce one -
 * `prefs.getString(key, "")` returning its default, an id that has not loaded yet, a bug upstream -
 * is also the case you least want handled silently: the events still report, but they never
 * audience-match, and that only surfaces much later in someone's reporting rather than at the call
 * site.
 *
 * So a blank identifier can no longer be mistaken for one, and minting one is something a caller
 * has to ask for by name.
 */
sealed class UserIdentity {

    /**
     * The marketplace's own identifier for this user.
     *
     * Audience matching resolves this against the marketplace's records, so it has to be an id the
     * marketplace actually knows. Prefer it whenever one is available, logged in or not.
     *
     * Build one with [of], which returns null rather than throwing for a blank id - a caller
     * holding a value that might be blank has to decide what that means, and the usual answer is
     * spelled `?: UserIdentity.Unidentified`.
     */
    class Marketplace private constructor(val id: String) : UserIdentity() {

        override fun equals(other: Any?): Boolean = other is Marketplace && other.id == id

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String = "Marketplace(id=$id)"

        public companion object {
            /**
             * A [Marketplace] identity for [id], or null if [id] is blank.
             *
             * Null rather than an exception because this is public API of an SDK that must never
             * crash its host, and blank is a value integrators genuinely hold at runtime rather
             * than only a typo. Null still refuses to guess, which is the point.
             */
            @JvmStatic
            public fun of(id: String): Marketplace? =
                if (id.isBlank()) null else Marketplace(id)
        }
    }

    /**
     * No marketplace identifier is available for this user.
     *
     * The SDK reuses the id already in effect if there is one - including a marketplace id
     * supplied earlier, which [Unidentified] never downgrades - and otherwise mints one and
     * persists it, so the same device keeps the same identity across launches.
     *
     * Events reported under a minted id are delivered and billed normally but will not
     * audience-match, because the id corresponds to nothing in the marketplace's records. Call
     * [Analytics.setup] again with a [Marketplace] id once one is available.
     *
     * Named for what it says about your knowledge of the user, not about the data: a minted id is
     * a persistent, device-scoped pseudonymous identifier, which most privacy regimes treat as
     * personal data. It is not anonymisation, and choosing this case does not remove whatever
     * consent obligations you already have.
     */
    public object Unidentified : UserIdentity()
}
