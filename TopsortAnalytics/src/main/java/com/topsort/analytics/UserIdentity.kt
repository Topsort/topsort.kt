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
 * Most callers want [of], which turns whatever they have into an identity:
 *
 * ```
 * Analytics.setup(application, UserIdentity.of(userId), token)
 * ```
 *
 * A blank or null id becomes [Unidentified] there - not silently, because the case is named and
 * the caller asked for the conversion. Use [Identified.of] instead when you need to branch on
 * whether an id was usable.
 *
 * New cases may be added in a minor release. Consumers matching on this type exhaustively should
 * include an `else` branch so that adding one is not a source-breaking change for them.
 */
sealed class UserIdentity {

    companion object {
        /**
         * [Identified] for a usable [id], [Unidentified] for one that is null or blank.
         *
         * Accepts null because "the id has not loaded yet" is one of the cases this type exists
         * for, and in Java that is spelled `null` at least as often as `""`.
         */
        @JvmStatic
        fun of(id: String?): UserIdentity = Identified.of(id) ?: Unidentified
    }

    /**
     * The marketplace's own identifier for this user.
     *
     * Audience matching resolves this against the marketplace's records, so it has to be an id the
     * marketplace actually knows - an id minted anywhere else matches nothing. Prefer it whenever
     * one is available, logged in or not.
     *
     * Build one with [Identified.of], which returns null rather than throwing for an unusable id.
     * If you do not need to distinguish that case, [UserIdentity.of] folds it into [Unidentified]
     * for you.
     *
     * If this type ever gains a second field, [equals], [hashCode] and [toString] are written by
     * hand and will need updating with it - nothing will fail to compile if they are not.
     */
    class Identified private constructor(val id: String) : UserIdentity() {

        override fun equals(other: Any?): Boolean = other is Identified && other.id == id

        override fun hashCode(): Int = id.hashCode()

        /** Contains [id], which is personal data - avoid logging it in production builds. */
        override fun toString(): String = "Identified(id=$id)"

        companion object {
            /**
             * An [Identified] identity for [id], or null if [id] is null or blank.
             *
             * Null rather than an exception because this is public API of an SDK that must never
             * crash its host, and an unusable id is a value integrators genuinely hold at runtime
             * rather than only a typo. Null still refuses to guess, which is the point.
             */
            @JvmStatic
            fun of(id: String?): Identified? =
                if (id.isNullOrBlank()) null else Identified(id)
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
     * [Analytics.setup] again with an [Identified] id once one is available.
     *
     * This is not a logout. Because it never downgrades, signing a user out and calling
     * [Analytics.setup] with [Unidentified] leaves their marketplace id in effect, and subsequent
     * events are still reported under it - which on a shared device attributes one person's
     * activity to another. There is currently no way to clear an identity; to represent a new
     * person, call [Analytics.setup] with their [Identified] id.
     *
     * Named for what it says about your knowledge of the user, not about the data: a minted id is
     * a persistent, device-scoped pseudonymous identifier, which most privacy regimes treat as
     * personal data. It is not anonymisation, and choosing this case does not remove whatever
     * consent obligations you already have.
     */
    object Unidentified : UserIdentity()
}
