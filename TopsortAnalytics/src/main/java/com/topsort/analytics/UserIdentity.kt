package com.topsort.analytics

/**
 * Who the events reported after [Analytics.setup] belong to. Replaces a plain-`String` identity
 * in which a blank silently meant "mint one for me" - a value that is easy to produce by accident
 * and yields events that never audience-match.
 *
 * ```
 * Analytics.setup(application, UserIdentity.of(userId), token)
 * ```
 */
sealed class UserIdentity {

    companion object {
        /** [Identified] for a usable [id], [Unidentified] when it is null or blank. */
        @JvmStatic
        fun of(id: String?): UserIdentity =
            if (id.isNullOrBlank()) Unidentified else Identified(id)
    }

    /**
     * The marketplace's own identifier for this user. Audience matching resolves it against the
     * marketplace's records, so pass one whenever available - logged in or not.
     */
    class Identified internal constructor(val id: String) : UserIdentity()

    /**
     * No marketplace identifier is available. The SDK keeps the id already in effect if there is
     * one - [Unidentified] never downgrades an identity we already hold - and otherwise mints one
     * and persists it, so the device keeps the same identity across launches. Events under a
     * minted id are delivered and billed normally but will not audience-match.
     *
     * A minted id is a persistent, device-scoped pseudonymous identifier, which most privacy
     * regimes treat as personal data. It is not anonymisation.
     *
     * This is not a logout: because it never downgrades, calling [Analytics.setup] with it after
     * signing a user out leaves their id in effect, which on a shared device attributes one
     * person's activity to another. To represent a new person, pass their id via [of].
     */
    object Unidentified : UserIdentity()
}
