package com.topsort.analytics.core

import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryVersionTest {

    /**
     * Guards against the constant drifting back to a hand-maintained literal. It was previously a
     * hardcoded `1.0`, so every release reported "topsort.kt/1.0" and server-side logs could not
     * identify which version an install was running.
     */
    @Test
    fun `library version is a full semantic version`() {
        assertTrue(
            "LIBRARY_VERSION should be generated from VERSION_NAME, was '$LIBRARY_VERSION'",
            LIBRARY_VERSION.matches(Regex("""\d+\.\d+\.\d+(-.+)?""")),
        )
    }
}
