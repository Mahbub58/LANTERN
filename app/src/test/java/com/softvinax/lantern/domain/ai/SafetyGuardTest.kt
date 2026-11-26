package com.softvinax.lantern.domain.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuardTest {

    private val guard = SafetyGuard()

    @Test
    fun `dosage numbers are unsafe`() {
        assertTrue(guard.isUnsafe("Take 500 mg of paracetamol"))
        assertTrue(guard.isUnsafe("Give 2 tablets now"))
        assertTrue(guard.isUnsafe("Use 3 drops in each eye"))
    }

    @Test
    fun `diagnosis language is unsafe`() {
        assertTrue(guard.isUnsafe("You probably have dengue"))
        assertTrue(guard.isUnsafe("This looks like cholera"))
        assertTrue(guard.isUnsafe("We can diagnose you here"))
    }

    @Test
    fun `practical safety steps are safe`() {
        assertFalse(guard.isUnsafe("Move to the highest floor and switch off the electricity"))
        assertFalse(guard.isUnsafe("Drop, cover, and hold on under a sturdy table"))
        assertFalse(guard.isUnsafe("Boil or treat drinking water before use"))
    }
}
