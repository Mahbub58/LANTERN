package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.data.fake.FakeCloudInference
import com.mahbub.lantern.data.fake.FakeConfigRepository
import com.mahbub.lantern.data.fake.FakeConnectivityRepository
import com.mahbub.lantern.data.fake.FakeOnDeviceInference
import com.mahbub.lantern.domain.ai.AiRouter
import com.mahbub.lantern.domain.ai.GroundedAnswerValidator
import com.mahbub.lantern.domain.ai.LexicalSafetyCardRetriever
import com.mahbub.lantern.domain.ai.PiiScrubber
import com.mahbub.lantern.domain.ai.SafetyGuard
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.testutil.InMemorySafetyCardRepository
import com.mahbub.lantern.testutil.earthquakeCard
import com.mahbub.lantern.testutil.floodCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskLanternUseCaseTest {

    private val config = FakeConfigRepository()
    private val connectivity = FakeConnectivityRepository()
    private val cards = InMemorySafetyCardRepository(listOf(floodCard(), earthquakeCard()))

    private val useCase = AskLanternUseCase(
        safetyCardRepository = cards,
        configRepository = config,
        connectivityRepository = connectivity,
        aiRouter = AiRouter(
            retriever = LexicalSafetyCardRetriever(),
            onDeviceInference = FakeOnDeviceInference(),
            cloudInference = FakeCloudInference(),
            piiScrubber = PiiScrubber(),
            answerValidator = GroundedAnswerValidator(SafetyGuard()),
            safetyGuard = SafetyGuard(),
        ),
    )

    @Test
    fun `confident question is answered on-device and the source is recorded`() = runTest {
        val answer = useCase(
            question = "flood water entering my home",
            language = AppLanguage.EN,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertFalse(answer.isNoMatch)
        assertEquals(InferenceSource.ON_DEVICE, config.lastInferenceSource.first())
    }

    @Test
    fun `no match returns the local emergency numbers`() = runTest {
        val answer = useCase(
            question = "my neighbour has a fever",
            language = AppLanguage.EN,
        )

        assertTrue(answer.isNoMatch)
        assertTrue(answer.emergencyNumbers.isNotEmpty())
        assertEquals(4, answer.emergencyNumbers.size)
    }

    @Test
    fun `explicit language is used for the answer`() = runTest {
        val answer = useCase(
            question = "flood water entering my home",
            language = AppLanguage.ROMAN_UR,
        )

        assertEquals(InferenceSource.ON_DEVICE, answer.source)
        assertTrue(answer.text.contains("Pani aap ke ghar"))
    }

    @Test
    fun `offline with an unreachable threshold returns the vetted card text`() = runTest {
        config.setRoutingConfig(RoutingConfig(confidenceThreshold = 0.99f))

        val answer = useCase(
            question = "flood",
            language = AppLanguage.EN,
        )

        assertEquals(InferenceSource.CARD_TEXT, answer.source)
        assertEquals("Water is entering your home", answer.text)
    }

    @Test
    fun `deeper answer uses cloud when online`() = runTest {
        connectivity.setSimulatedOnline(true)

        val answer = useCase(
            question = "flood water entering my home",
            language = AppLanguage.EN,
            deeper = true,
        )

        assertEquals(InferenceSource.CLOUD, answer.source)
        assertTrue(answer.isDeeper)
    }
}
