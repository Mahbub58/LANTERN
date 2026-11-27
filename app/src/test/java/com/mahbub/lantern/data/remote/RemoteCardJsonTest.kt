package com.mahbub.lantern.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteCardJsonTest {

    @Test
    fun `blank and null values mean no remote pack`() {
        assertEquals("", RemoteCardJson.normalize(null))
        assertEquals("", RemoteCardJson.normalize(""))
        assertEquals("", RemoteCardJson.normalize("   "))
        assertEquals("", RemoteCardJson.normalize("null"))
        assertEquals("", RemoteCardJson.normalize(" NULL "))
    }

    @Test
    fun `plain json object is returned unchanged`() {
        val raw = """{"version":"1.0.0","cards":[]}"""
        assertEquals(raw, RemoteCardJson.normalize(raw))
    }

    @Test
    fun `quoted json string is unwrapped once`() {
        val escaped = "\"{\\\"version\\\":\\\"1.1.0\\\",\\\"cards\\\":[]}\""
        assertEquals("""{"version":"1.1.0","cards":[]}""", RemoteCardJson.normalize(escaped))
    }

    @Test
    fun `quoted json string with surrounding whitespace is unwrapped`() {
        val escaped = "  \"{\\\"version\\\":\\\"2.0.0\\\"}\"  "
        assertEquals("""{"version":"2.0.0"}""", RemoteCardJson.normalize(escaped))
    }

    @Test
    fun `describe never throws on fetched values`() {
        assertEquals("empty", RemoteCardJson.describe(""))
        assertEquals(
            "17 chars, starts={\"version\":\"1.0\"}",
            RemoteCardJson.describe("""{"version":"1.0"}"""),
        )
    }
}
