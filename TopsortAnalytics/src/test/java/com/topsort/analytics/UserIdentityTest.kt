package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class UserIdentityTest {

    @Test
    fun `a marketplace identity keeps the id it was given`() {
        assertThat(UserIdentity.Marketplace("user-1").id).isEqualTo("user-1")
    }

    /** The whole point of the type: a blank id fails where it is written, not months later. */
    @Test
    fun `a blank marketplace id is rejected`() {
        assertThatThrownBy { UserIdentity.Marketplace("") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Anonymous")
    }

    @Test
    fun `a whitespace-only marketplace id is rejected`() {
        assertThatThrownBy { UserIdentity.Marketplace("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `anonymous is a singleton so it can be compared by identity`() {
        assertThat(UserIdentity.Anonymous).isSameAs(UserIdentity.Anonymous)
    }

    @Test
    fun `marketplace identities compare by id`() {
        assertThat(UserIdentity.Marketplace("a")).isEqualTo(UserIdentity.Marketplace("a"))
        assertThat(UserIdentity.Marketplace("a")).isNotEqualTo(UserIdentity.Marketplace("b"))
        assertThat(UserIdentity.Marketplace("a")).isNotEqualTo(UserIdentity.Anonymous)
    }
}
