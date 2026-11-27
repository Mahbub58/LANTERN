package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.Answer
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
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
 * The router has five observable outcomes; every one is covered here.
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
            retriever = LexicalSafetyCardRetriever(),
            onDeviceInference = onDevice,
            cloudInference = cloud,
            piiScrubber = PiiScrubber(),
            answerValidator = GroundedAnswerValidator(SafetyGuard()),
            safetyGuard = SafetyGuard(),
        )
    }

    @Test
    fun `no card matches returns NoMatch without calling any model`() = runTest {
        val answer = router.route(
            question = "my neighbour has a fever",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertNull(answer.card)
        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        coVerify(exactly = 0) { onDevice.isAvailable() }
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
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
    }

    @Test
    fun `high confidence uses the on-device model`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.answer(any(), any(), any()) } returns """
            {"cardId":"flood-1","answer":"Move to the highest floor."}
        """.trimIndent()

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertEquals(card, answer.card)
        assertEquals("Move to the highest floor.", answer.text)
        assertTrue(answer.retrievalScore >= RoutingConfig.DEFAULT_THRESHOLD)
        coVerify(exactly = 0) { cloud.answer(any(), any(), any()) }
    }

    @Test
    fun `low confidence falls back to cloud when online and enabled`() = runTest {
        coEvery { cloud.answer(any(), any(), any()) } returns "Move to higher ground now."

        val answer = router.route(
            question = "flood",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals("Move to higher ground now.", answer.text)
        assertFalse(answer.isNoMatch)
        coVerify(exactly = 0) { onDevice.isAvailable() }
    }

    @Test
    fun `on-device failure falls back to cloud when online`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
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
    }

    @Test
    fun `offline returns the vetted card text`() = runTest {
        coEvery { onDevice.isAvailable() } returns false

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        assertEquals(card.title.en, answer.text)
        assertEquals(card, answer.card)
    }

    @Test
    fun `cloud disabled online still returns the card text`() = runTest {
        coEvery { onDevice.isAvailable() } returns false

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
    fun `deeper answer forces cloud and marks isDeeper`() = runTest {
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
        coVerify(exactly = 0) { onDevice.isAvailable() }
    }

    @Test
    fun `cloud answer with a dosage is rejected in favour of the card`() = runTest {
        coEvery { onDevice.isAvailable() } returns false
        coEvery { cloud.answer(any(), any(), any()) } returns "Take 2 tablets of paracetamol."

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
    }

    @Test
    fun `language is forwarded to the card text fallback`() = runTest {
        coEvery { onDevice.isAvailable() } returns false

        val answer: Answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.ROMAN_UR,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(card.title.roman, answer.text)
    }
}
