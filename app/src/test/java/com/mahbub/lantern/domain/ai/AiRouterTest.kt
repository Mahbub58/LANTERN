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
 * Both engines return the same [TriageResult]. These tests pin the routing:
 * Nano first, cloud fallback, and the user-visible no-match state.
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
            safetyGuard = SafetyGuard(),
        )

        coEvery { onDevice.isAvailable() } returns false
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult.NoMatch
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult.NoMatch
    }

    @Test
    fun `nano matched result is shown as an on-device answer`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to the highest floor.",
        )

        val answer = router.route(
            question = "pani ghar mein aa raha hai",
            language = AppLanguage.ROMAN_UR,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertEquals(card, answer.card)
        assertEquals("Move to the highest floor.", answer.text)
        assertNull(answer.onDeviceIssue)
        coVerify(exactly = 1) { onDevice.matchAndAnswer(any(), any(), any()) }
        coVerify(exactly = 0) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `a valid card id is accepted even when the model says matched false`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = false,
            cardId = "flood-1",
            answer = "Move to the highest floor.",
        )

        val answer = router.route(
            question = "flood water rising",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertEquals(card, answer.card)
        assertFalse(answer.isNoMatch)
    }

    @Test
    fun `a card title returned instead of an id is resolved`() = runTest {
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "Water is entering your home",
            answer = "Move to the highest floor.",
        )

        val answer = router.route(
            question = "flood water rising",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(card, answer.card)
    }

    @Test
    fun `nano unavailable falls back to cloud triage`() = runTest {
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to higher ground now.",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertFalse(answer.isNoMatch)
        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(card, answer.card)
        assertEquals("Move to higher ground now.", answer.text)
        assertEquals(OnDeviceIssue.UNAVAILABLE, answer.onDeviceIssue)
        coVerify(exactly = 0) { onDevice.matchAndAnswer(any(), any(), any()) }
        coVerify(exactly = 1) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `nano unavailable and cloud false returns NoMatch`() = runTest {
        val answer = router.route(
            question = "my neighbour has a fever",
            language = AppLanguage.UR,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertNull(answer.card)
        coVerify(exactly = 1) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `nano unavailable offline never calls the cloud`() = runTest {
        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
        assertEquals(OnDeviceIssue.UNAVAILABLE, answer.onDeviceIssue)
        coVerify(exactly = 0) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `nano failure falls back to cloud triage`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } throws
            IllegalStateException("nano load failed")
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to higher ground now.",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(OnDeviceIssue.FAILED, answer.onDeviceIssue)
        coVerify(exactly = 1) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `nano no match falls back to cloud triage`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to higher ground now.",
        )

        val answer = router.route(
            question = "pani ghar mein aa raha hai",
            language = AppLanguage.ROMAN_UR,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(card, answer.card)
        assertNull(answer.onDeviceIssue)
        coVerify(exactly = 1) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `unsafe on-device answer falls back to the vetted card title`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Take 2 tablets of paracetamol.",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = false,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertEquals(card.title.en, answer.text)
    }

    @Test
    fun `unsafe cloud answer falls back to the vetted card title`() = runTest {
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Take 2 tablets of paracetamol.",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertEquals(card.title.en, answer.text)
    }

    @Test
    fun `blank on-device answer falls back to the card title in the selected language`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "",
        )

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
    fun `deeper answer skips nano and uses cloud triage`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to higher ground now.",
        )

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
        coVerify(exactly = 0) { onDevice.matchAndAnswer(any(), any(), any()) }
        coVerify(exactly = 1) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `deeper cloud failure falls back to nano`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to the highest floor.",
        )
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } throws
            IllegalStateException("cloud down")

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
            forceCloud = true,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertFalse(answer.isDeeper)
        assertNull(answer.onDeviceIssue)
    }

    @Test
    fun `cloud disabled offline still uses nano`() = runTest {
        coEvery { onDevice.isAvailable() } returns true
        coEvery { onDevice.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "flood-1",
            answer = "Move to the highest floor.",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(cloudFallbackEnabled = false),
            isOnline = true,
            cards = cards,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        coVerify(exactly = 0) { cloud.matchAndAnswer(any(), any(), any()) }
    }

    @Test
    fun `a cloud request failure surfaces an error instead of a false no-match`() = runTest {
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } throws
            IllegalStateException("network down")

        val failure = runCatching {
            router.route(
                question = "flood water rising",
                language = AppLanguage.EN,
                config = RoutingConfig(),
                isOnline = true,
                cards = cards,
            )
        }

        assertTrue(failure.isFailure)
    }

    @Test
    fun `cloud match with an unknown card id is treated as no match`() = runTest {
        coEvery { cloud.matchAndAnswer(any(), any(), any()) } returns TriageResult(
            matched = true,
            cardId = "not-a-real-card",
            answer = "Some answer",
        )

        val answer = router.route(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            config = RoutingConfig(),
            isOnline = true,
            cards = cards,
        )

        assertTrue(answer.isNoMatch)
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
