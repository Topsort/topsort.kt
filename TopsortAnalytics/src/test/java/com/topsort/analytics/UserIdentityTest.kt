package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UserIdentityTest {

    @Test
    fun `of yields an identified identity carrying the id`() {
        val identity = UserIdentity.of("user-1")

        assertThat(identity).isInstanceOf(UserIdentity.Identified::class.java)
        assertThat((identity as UserIdentity.Identified).id).isEqualTo("user-1")
    }

    /**
     * The whole point of the type. Blank is the value integrators produce by accident;
     * null is the same mistake spelled the way Java spells it.
     */
    @Test
    fun `of folds an unusable id into Unidentified`() {
        assertThat(UserIdentity.of("")).isSameAs(UserIdentity.Unidentified)
        assertThat(UserIdentity.of("   ")).isSameAs(UserIdentity.Unidentified)
        assertThat(UserIdentity.of(null)).isSameAs(UserIdentity.Unidentified)
    }

    @Test
    fun `an identified identity is not an unidentified one`() {
        assertThat(UserIdentity.of("user-1")).isNotEqualTo(UserIdentity.Unidentified)
    }
}
