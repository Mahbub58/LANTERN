package com.softvinax.lantern.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelJsonTest {

    @Test
    fun `parses a clean json object`() {
        val payload = ModelJson.answer("""{"cardId":"flood-water-rising","answer":"Move up now."}""")
        assertEquals("flood-water-rising", payload?.cardId)
        assertEquals("Move up now.", payload?.answer)
    }

    @Test
    fun `strips a markdown fence`() {
        val payload = ModelJson.answer(
            "```json\n{\"cardId\":\"c1\",\"answer\":\"Go to higher ground.\"}\n```",
        )
        assertEquals("c1", payload?.cardId)
    }

    @Test
    fun `extracts the object when the model adds prose`() {
        val payload = ModelJson.answer(
            "Sure, here it is: {\"cardId\":\"c1\",\"answer\":\"Stay visible.\"} Thanks.",
        )
        assertEquals("Stay visible.", payload?.answer)
    }

    @Test
    fun `handles braces inside a json string`() {
        val payload = ModelJson.answer(
            """{"cardId":"c1","answer":"Use a torch {or cloth} and stay visible."}""",
        )
        assertEquals("Use a torch {or cloth} and stay visible.", payload?.answer)
    }

    @Test
    fun `rejects malformed or empty answers`() {
        assertNull(ModelJson.answer(null))
        assertNull(ModelJson.answer(""))
        assertNull(ModelJson.answer("not json"))
        assertNull(ModelJson.answer("""{"cardId":"","answer":"x"}"""))
        assertNull(ModelJson.answer("""{"cardId":"c1","answer":""}"""))
    }

    @Test
    fun `clamps urgency into one to five`() {
        assertEquals(5, ModelJson.urgency("""{"urgency":9}"""))
        assertEquals(1, ModelJson.urgency("""{"urgency":0}"""))
        assertNull(ModelJson.urgency("nope"))
    }

    @Test
    fun `keeps only known ids in duplicate groups`() {
        val groups = ModelJson.duplicateGroups(
            """{"groups":[["a","b"],["c"],["b","a","ghost"]]}""",
            knownIds = setOf("a", "b", "c"),
        )
        assertEquals(listOf(listOf("a", "b"), listOf("b", "a")), groups)
    }

    @Test
    fun `encodes the exact contract the validator expects`() {
        val encoded = ModelJson.encodeAnswer("c1", "Move to higher ground.")
        assertTrue(encoded.contains("\"cardId\":\"c1\""))
        assertTrue(encoded.contains("\"answer\":\"Move to higher ground.\""))
        assertEquals("Move to higher ground.", ModelJson.answer(encoded)?.answer)
    }
}
