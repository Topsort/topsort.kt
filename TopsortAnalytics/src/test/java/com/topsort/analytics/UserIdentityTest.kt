package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UserIdentityTest {

    @Test
    fun `a marketplace identity keeps the id it was given`() {
        assertThat(UserIdentity.Marketplace.of("user-1")?.id).isEqualTo("user-1")
    }

    /**
     * The point of the type: a blank id cannot be mistaken for one. Null rather than an exception
     * because this is SDK public API that must never crash its host - it still refuses to guess.
     */
    @Test
    fun `a blank marketplace id yields null rather than throwing`() {
        assertThat(UserIdentity.Marketplace.of("")).isNull()
    }

    @Test
    fun `a whitespace-only marketplace id yields null`() {
        assertThat(UserIdentity.Marketplace.of("   ")).isNull()
    }

    /** The idiom the SDK documents for an id the caller is not certain of. */
    @Test
    fun `a blank id falls back to Unidentified when the caller elects to`() {
        val identity: UserIdentity = UserIdentity.Marketplace.of("") ?: UserIdentity.Unidentified

        assertThat(identity).isSameAs(UserIdentity.Unidentified)
    }

    @Test
    fun `unidentified is a singleton so it can be compared by identity`() {
        assertThat(UserIdentity.Unidentified).isSameAs(UserIdentity.Unidentified)
    }

    @Test
    fun `marketplace identities compare by id`() {
        assertThat(UserIdentity.Marketplace.of("a")).isEqualTo(UserIdentity.Marketplace.of("a"))
        assertThat(UserIdentity.Marketplace.of("a")).isNotEqualTo(UserIdentity.Marketplace.of("b"))
        assertThat(UserIdentity.Marketplace.of("a")).isNotEqualTo(UserIdentity.Unidentified)
    }

    @Test
    fun `marketplace identities hash by id`() {
        assertThat(UserIdentity.Marketplace.of("a").hashCode())
            .isEqualTo(UserIdentity.Marketplace.of("a").hashCode())
    }
}
