package com.topsort.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * The AAR must stay linkable from Kotlin 2.1: a Kotlin compiler reads metadata at most one minor
 * version ahead of itself, and Sympla compiles with 2.1. The version written follows the language
 * level pinned in build.gradle, not the compiler AGP bundles, and nothing else would notice it
 * drifting - 3.0.0 through 3.3.0 shipped 2.3.0 metadata unremarked. Read from the compiled class,
 * so this measures what ships rather than what the build file says.
 */
class CompiledMetadataVersionTest {

    @Test
    fun `compiled classes carry metadata a Kotlin 2 1 compiler can read`() {
        val metadata = Analytics::class.java.getAnnotation(Metadata::class.java)

        assertThat(metadata).isNotNull
        assertThat(metadata!!.metadataVersion).containsExactly(2, 1, 0)
    }
}
