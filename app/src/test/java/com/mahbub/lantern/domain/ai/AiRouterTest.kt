package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.OnDeviceIssue
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.model.SafetyCard
import com.mahbub.lantern.testutil.earthquakeCard
import com.mahbub.lantern.testutil.floodCard
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Matching is Gemini Nano only. These tests pin every observable outcome of
 * that policy, including the two user-visible error states.
 */
class AiRouterTest {

    private val onDevice: OnDeviceInference = mockk()
    private val cloud: CloudInference = mockk()
    private lateinit var router: AiRouter

    private val card: SafetyCard = floodCard()
    private val cards = listOf(card, earthquakeCard())

    @Before
    fun setUp() {
        router = AiRouter(
            onDeviceInference = onDevice,
            cloudInference = cloud,
            piiScrubber = PiiScrubber(),
            answerValidator = GroundedAnswerValidator(SafetyGuard()),
            safetyGuard = SafetyGuard(),
        )

        coEvery { onDevice.isAvailable() } returns false
        coEvery { onDevice.matchCard(any(), any()) } returns null
    }

    @Test
    fun `nano unavailable returns NoMatch and never calls another matcher`() = runTest {
        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertNull(answer.card)
        assertEquals(OnDeviceIssue.UNAVAILABLE, answer.onDeviceIssue)
        coVerify(exactly = 0) { onDevice.matchCard(any(), any()) }
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
    }

    @Test
    fun `nano match failure returns NoMatch with a failed issue`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } throws IllegalStateException("nano load failed")

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertEquals(OnDeviceIssue.FAILED, answer.onDeviceIssue)
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
    }

    @Test
    fun `nano matched card is answered on-device`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns """
            {"cardId":"flood-1","answer":"Move to the highest floor."}
        """.trimIndent()

        val answer = router.route(
            question = "chhat tapak rahi hai",
            language = AppLanguage.ROMAN_UR,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertEquals(card, answer.card)
        assertEquals("Move to the highest floor.", answer.text)
        assertNull(answer.onDeviceIssue)
        coVerify(exactly = 1) { onDevice.matchCard(any(), any()) }
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
    }

    @Test
    fun `nano finding no card returns NoMatch without an error`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns null

        val answer = router.route(
            question = "my neighbour has a fever",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertNull(answer.onDeviceIssue)
    }

    @Test
    fun `nano generation failure falls back to cloud and is reported`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns "I am not JSON"
        coEvery { cloud.answer(any(), any(), any()) } returns "Move to higher ground now."

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(OnDeviceIssue.FAILED, answer.onDeviceIssue)
    }

    @Test
    fun `offline generation failure returns the matched card text`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns "I am not JSON"

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        assertEquals(card.title.en, answer.text)
        assertEquals(OnDeviceIssue.FAILED, answer.onDeviceIssue)
    }

    @Test
    fun `deeper answer still matches with nano then forces cloud`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { cloud.answer(any(), any(), any()) } returns "Move to higher ground now."

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
            forceCloud = true,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertTrue(answer.isDeeper)
        assertNull(answer.onDeviceIssue)
        coVerify(exactly = 1) { onDevice.matchCard(any(), any()) }
        coVerify(exactly = 0) { onDevice.answer(any(), any(), any()) }
    }

    @Test
    fun `cloud answer with a dosage is rejected in favour of the card`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns "I am not JSON"
        coEvery { cloud.answer(any(), any(), any()) } returns "Take 2 tablets of paracetamol."

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        assertFalse(answer.text.isBlank())
    }

    @Test
    fun `cloud disabled online still returns the card text`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns "I am not JSON"

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(cloudFallbackEnabled = false),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
    }

    @Test
    fun `language is forwarded to the card text fallback`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchCard(any(), any()) } returns "flood-1"
        coEvery { onDevice.answer(any(), any(), any()) } returns "I am not JSON"

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.ROMAN_UR,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(card.title.roman, answer.text)
    }

    @Test
    fun `blank question is a NoMatch`() = runTest {
        val answer = router.route(
            question = "   ",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        coVerify(exactly = 0) { onDevice.isAvailable() }
    }
}
