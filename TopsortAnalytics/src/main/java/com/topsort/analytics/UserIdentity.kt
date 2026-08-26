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
 * So a blank identifier is now rejected where it is written, and generating one is something a
 * caller has to ask for by name.
 */
sealed class UserIdentity {

    /**
     * The marketplace's own identifier for this user.
     *
     * Audience matching resolves this against the marketplace's records, so it has to be an id the
     * marketplace actually knows. Prefer it whenever one is available, logged in or not.
     *
     * @throws IllegalArgumentException if [id] is blank. That is deliberate: a blank here is a bug
     * at the call site, and failing loudly beats reporting events that can never match anything.
     */
    data class Marketplace(val id: String) : UserIdentity() {
        init {
            require(id.isNotBlank()) {
                "A Marketplace id must not be blank. If no identifier is available for this user, " +
                    "pass UserIdentity.Anonymous instead."
            }
        }
    }

    /**
     * No marketplace identifier is available for this user.
     *
     * The SDK reuses the id already in effect if there is one - including a marketplace id supplied
     * earlier, which [Anonymous] never downgrades - and otherwise mints one and persists it, so the
     * same device keeps the same identity across launches.
     *
     * Events reported under a minted id are delivered and billed normally but will not
     * audience-match, because the id corresponds to nothing in the marketplace's records. Call
     * [Analytics.setup] again with a [Marketplace] id once one is available.
     */
    object Anonymous : UserIdentity()
}
