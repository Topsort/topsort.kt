package com.topsort.analytics

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How [UserIdentity] resolves to the id events are reported under.
 *
 * The behaviour is the one the blank-String contract used to have; what changed is that a caller
 * now has to say which of the two they mean, so a blank id produced by accident fails at the call
 * site instead of silently becoming an unidentified session.
 */
@RunWith(AndroidJUnit4::class)
class UserIdentityResolutionTest {

    @Before
    fun setUp() {
        EventPipelineHarness.install()
    }

    @After
    fun tearDown() {
        EventPipelineHarness.uninstall()
    }

    private fun setup(identity: UserIdentity) =
        Analytics.setup(EventPipelineHarness.application, identity, EventPipelineHarness.TOKEN)

    @Test
    fun a_marketplace_id_is_used_as_given() {
        setup(UserIdentity.of("marketplace-id"))

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    @Test
    fun unidentified_mints_an_id_when_there_is_nothing_to_fall_back_on() {
        setup(UserIdentity.Unidentified)

        assertThat(Analytics.opaqueUserId).isNotBlank()
    }

    /** Minting once and keeping it is the whole reason the SDK should own this, not the caller. */
    @Test
    fun unidentified_reuses_the_id_it_minted() {
        setup(UserIdentity.Unidentified)
        val minted = Analytics.opaqueUserId

        setup(UserIdentity.Unidentified)

        assertThat(Analytics.opaqueUserId).isEqualTo(minted)
    }

    /**
     * Identified.of rejects a blank id, but the constructor behind it is internal - and Kotlin
     * emits internal constructors as public JVM methods, so Java can reach one directly. The
     * cache must not trust the invariant it cannot enforce: a blank must never reach the wire,
     * because the API rejects it and the event is lost.
     */
    @Test
    fun a_blank_identified_id_is_treated_as_unidentified() {
        setup(UserIdentity.Identified(""))

        assertThat(Analytics.opaqueUserId).isNotBlank()
    }

    /** And it must not clobber an identity we already hold. */
    @Test
    fun a_blank_identified_id_does_not_replace_a_real_one() {
        setup(UserIdentity.of("marketplace-id"))

        setup(UserIdentity.Identified(""))

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    /**
     * The deprecated String overload has to keep behaving exactly as it did, or upgrading to this
     * release would change identity handling under callers who have not migrated.
     */
    @Suppress("DEPRECATION")
    @Test
    fun the_deprecated_overload_maps_a_supplied_id_to_marketplace() {
        Analytics.setup(EventPipelineHarness.application, "marketplace-id", EventPipelineHarness.TOKEN)

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }

    @Suppress("DEPRECATION")
    @Test
    fun the_deprecated_overload_maps_blank_to_unidentified_rather_than_throwing() {
        Analytics.setup(EventPipelineHarness.application, "", EventPipelineHarness.TOKEN)

        assertThat(Analytics.opaqueUserId).isNotBlank()
    }

    @Suppress("DEPRECATION")
    @Test
    fun the_deprecated_overload_still_keeps_an_id_across_a_blank_call() {
        Analytics.setup(EventPipelineHarness.application, "marketplace-id", EventPipelineHarness.TOKEN)

        Analytics.setup(EventPipelineHarness.application, "", EventPipelineHarness.TOKEN)

        assertThat(Analytics.opaqueUserId).isEqualTo("marketplace-id")
    }
}
