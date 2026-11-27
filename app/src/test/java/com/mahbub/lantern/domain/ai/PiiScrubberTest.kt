package com.mahbub.lantern.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PiiScrubberTest {

    private val scrubber = PiiScrubber()

    @Test
    fun `blank input is returned unchanged`() {
        assertEquals("", scrubber.scrub(""))
        assertEquals("   ", scrubber.scrub("   "))
    }

    @Test
    fun `plain emergency text is preserved`() {
        val input = "Flood water is rising fast, we are on the roof"
        assertEquals(input, scrubber.scrub(input))
    }

    @Test
    fun `email is redacted`() {
        val result = scrubber.scrub("Email me at ali.khan@example.com for details")
        assertFalse(result.contains("ali.khan"))
        assertTrue(result.contains("[email]"))
    }

    @Test
    fun `phone number is redacted`() {
        val result = scrubber.scrub("Call me on 0300 1234567 please")
        assertFalse(result.contains("0300 1234567"))
        assertTrue(result.contains("[phone]"))
    }

    @Test
    fun `cnic is redacted`() {
        val result = scrubber.scrub("My CNIC is 35202-1234567-1")
        assertFalse(result.contains("35202"))
        assertTrue(result.contains("[id]"))
    }

    @Test
    fun `coordinates are redacted`() {
        val result = scrubber.scrub("We are at 24.8607, 67.0011 near the school")
        assertFalse(result.contains("24.8607"))
        assertTrue(result.contains("[location]"))
    }

    @Test
    fun `url is redacted`() {
        val result = scrubber.scrub("See https://example.com/flood for updates")
        assertFalse(result.contains("example.com"))
        assertTrue(result.contains("[link]"))
    }

    @Test
    fun `house number is redacted`() {
        val result = scrubber.scrub("House no. 42 is fully under water")
        assertFalse(result.contains("42"))
        assertTrue(result.contains("[location]"))
    }

    @Test
    fun `name after label is redacted`() {
        val result = scrubber.scrub("My name is Ayesha and I am trapped")
        assertFalse(result.contains("Ayesha"))
        assertTrue(result.contains("[name]"))
    }

    @Test
    fun `roman urdu address label is redacted`() {
        val result = scrubber.scrub("mera pata Gulshan Colony hai")
        assertFalse(result.contains("Gulshan Colony"))
        assertTrue(result.contains("[location]"))
    }

    @Test
    fun `scrubbing is idempotent enough to run twice`() {
        val once = scrubber.scrub("Call 0300 1234567 or ali@example.com")
        val twice = scrubber.scrub(once)
        assertEquals(once, twice)
    }
}
