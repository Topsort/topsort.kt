package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UserIdentityTest {

    @Test
    fun `an identified identity keeps the id it was given`() {
        assertThat(UserIdentity.Identified.of("user-1")?.id).isEqualTo("user-1")
    }

    /**
     * The point of the type: an unusable id cannot be mistaken for one. Null rather than an
     * exception because this is SDK public API that must never crash its host - it still refuses
     * to guess.
     */
    @Test
    fun `a blank identified id yields null rather than throwing`() {
        assertThat(UserIdentity.Identified.of("")).isNull()
    }

    @Test
    fun `a whitespace-only identified id yields null`() {
        assertThat(UserIdentity.Identified.of("   ")).isNull()
    }

    /** Null is as common as blank for "the id has not loaded yet", especially from Java. */
    @Test
    fun `a null identified id yields null`() {
        assertThat(UserIdentity.Identified.of(null)).isNull()
    }

    @Test
    fun `of returns an identified identity for a usable id`() {
        assertThat(UserIdentity.of("user-1")).isEqualTo(UserIdentity.Identified.of("user-1"))
    }

    /** The conversion callers reach for, and the one the SDK's own sample and README use. */
    @Test
    fun `of folds a blank id into Unidentified`() {
        assertThat(UserIdentity.of("")).isSameAs(UserIdentity.Unidentified)
        assertThat(UserIdentity.of("   ")).isSameAs(UserIdentity.Unidentified)
    }

    /** Java hands us null far more readily than blank, and it must not reach a null-check. */
    @Test
    fun `of folds a null id into Unidentified`() {
        assertThat(UserIdentity.of(null)).isSameAs(UserIdentity.Unidentified)
    }

    @Test
    fun `unidentified is a singleton so it can be compared by identity`() {
        assertThat(UserIdentity.Unidentified).isSameAs(UserIdentity.Unidentified)
    }

    @Test
    fun `identified identities compare by id`() {
        assertThat(UserIdentity.Identified.of("a")).isEqualTo(UserIdentity.Identified.of("a"))
        assertThat(UserIdentity.Identified.of("a")).isNotEqualTo(UserIdentity.Identified.of("b"))
        assertThat(UserIdentity.Identified.of("a")).isNotEqualTo(UserIdentity.Unidentified)
    }

    @Test
    fun `identified identities hash by id`() {
        assertThat(UserIdentity.Identified.of("a").hashCode())
            .isEqualTo(UserIdentity.Identified.of("a").hashCode())
    }

    /** Written by hand rather than generated, so it needs a test like any other code. */
    @Test
    fun `toString names the type and the id`() {
        assertThat(UserIdentity.Identified.of("user-1").toString()).isEqualTo("Identified(id=user-1)")
    }
}
