package com.softvinax.lantern.domain.ai

import com.softvinax.lantern.domain.model.LocalizedList
import com.softvinax.lantern.domain.model.LocalizedText
import com.softvinax.lantern.domain.model.SafetyCard
import com.softvinax.lantern.testutil.floodCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LexicalSafetyCardRetrieverTest {

    private val retriever = LexicalSafetyCardRetriever()

    @Test
    fun `blank question returns null`() {
        assertNull(retriever.bestCard("   ", listOf(floodCard())))
    }

    @Test
    fun `empty card list returns null`() {
        assertNull(retriever.bestCard("flood water", emptyList()))
    }

    @Test
    fun `unrelated question returns null`() {
        assertNull(retriever.bestCard("my neighbour has a fever", listOf(floodCard())))
    }

    @Test
    fun `exact keyword phrase scores one`() {
        val card = cardWithKeywords(listOf("flood water"))
        assertEquals(1f, retriever.score("flood water", card), 0.0001f)
    }

    @Test
    fun `partial keyword scores above the minimum but below the default threshold`() {
        val score = retriever.score("flood", floodCard())

        assertTrue(score >= com.softvinax.lantern.domain.model.RoutingConfig.MINIMUM_MATCH_SCORE)
        assertTrue(score < com.softvinax.lantern.domain.model.RoutingConfig.DEFAULT_THRESHOLD)
    }

    @Test
    fun `best card wins when several match`() {
        val weak = cardWithKeywords(listOf("flood"), id = "weak")
        val strong = cardWithKeywords(listOf("flood water"), id = "strong")

        val best = retriever.bestCard("flood water", listOf(weak, strong))

        assertEquals("strong", best?.card?.id)
    }

    @Test
    fun `roman urdu phrase matches`() {
        val card = cardWithKeywords(listOf("pani ghar"))
        val best = retriever.bestCard("pani ghar mein aa raha hai", listOf(card))

        assertEquals(card.id, best?.card?.id)
        assertEquals(1f, best?.score ?: 0f, 0.0001f)
    }

    @Test
    fun `urdu phrase matches`() {
        val card = cardWithKeywords(listOf("سیلاب"), roman = listOf("selab"))
        val best = retriever.bestCard("سیلاب آ گیا ہے", listOf(card))

        assertEquals(card.id, best?.card?.id)
        assertTrue((best?.score ?: 0f) > 0f)
    }

    private fun cardWithKeywords(
        english: List<String>,
        roman: List<String> = emptyList(),
        id: String = "card-1",
    ) = SafetyCard(
        id = id,
        title = LocalizedText(en = "Safety card"),
        steps = LocalizedList(en = listOf("Stay safe.")),
        never = LocalizedList(),
        source = "TO VERIFY",
        keywords = LocalizedList(en = english, roman = roman),
    )
}
