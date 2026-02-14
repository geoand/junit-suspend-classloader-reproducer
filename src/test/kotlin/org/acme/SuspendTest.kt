package org.acme

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SuspendTest {
    @Test
    suspend fun suspendingTestShouldRun() {
        assertTrue(true)
    }

    @Test
    fun normalTestShouldRun() {
        assertTrue(true)
    }
}
