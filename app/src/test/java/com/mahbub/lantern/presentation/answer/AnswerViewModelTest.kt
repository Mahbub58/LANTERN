package com.mahbub.lantern.presentation.answer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mahbub.lantern.domain.model.Answer
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.usecase.AskLanternUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.ObserveRoutingConfigUseCase
import com.mahbub.lantern.testutil.MainDispatcherRule
import com.mahbub.lantern.testutil.awaitUntil
import com.mahbub.lantern.testutil.floodCard
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnswerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val online = MutableStateFlow(false)
    private val config = MutableStateFlow(RoutingConfig())
    private val language = MutableStateFlow(AppLanguage.EN)

    private val askLantern = mockk<AskLanternUseCase>()
    private val observeConnectivity = mockk<ObserveConnectivityUseCase>()
    private val observeRoutingConfig = mockk<ObserveRoutingConfigUseCase>()
    private val observeLanguage = mockk<ObserveLanguageUseCase>()

    private val onDeviceAnswer = Answer(
        card = floodCard(),
        text = "Move to the highest floor.",
        source = InferenceSource.ON_DEVICE,
        retrievalScore = 0.9f,
    )

    private fun viewModel(question: String = "flood water"): AnswerViewModel {
        every { observeConnectivity() } returns online
        every { observeRoutingConfig() } returns config
        every { observeLanguage() } returns language
        return AnswerViewModel(
            savedStateHandle = SavedStateHandle(mapOf("question" to question)),
            askLantern = askLantern,
            observeConnectivity = observeConnectivity,
            observeRoutingConfig = observeRoutingConfig,
            observeLanguage = observeLanguage,
        )
    }

    @Test
    fun `route question is asked on creation`() = runTest {
        coEvery { askLantern(any(), any(), any()) } returns onDeviceAnswer
        val vm = viewModel("flood water")

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading && it.answer != null }
            assertEquals("flood water", loaded.question)
            assertEquals(InferenceSource.ON_DEVICE, loaded.answer?.source)
            assertFalse(loaded.hasError)
        }
    }

    @Test
    fun `askDeeper is blocked while offline`() = runTest {
        coEvery { askLantern(any(), any(), any()) } returns onDeviceAnswer
        val vm = viewModel()
        vm.state.test { awaitUntil { it.answer != null } }

        vm.askDeeper()

        coVerify(exactly = 0) { askLantern(any(), any(), true) }
    }

    @Test
    fun `askDeeper requests a cloud answer when online`() = runTest {
        coEvery { askLantern(any(), any(), any()) } returns onDeviceAnswer
        val vm = viewModel()
        vm.state.test { awaitUntil { it.answer != null } }

        online.value = true
        vm.state.test { awaitUntil { it.canUseCloud } }

        val cloudAnswer = onDeviceAnswer.copy(source = InferenceSource.CLOUD, isDeeper = true)
        coEvery { askLantern(any(), any(), true) } returns cloudAnswer
        vm.askDeeper()

        vm.state.test {
            val loaded = awaitUntil { it.answer?.source == InferenceSource.CLOUD }
            assertTrue(loaded.answer?.isDeeper == true)
            assertFalse(loaded.isAskingDeeper)
        }
    }

    @Test
    fun `a failed ask surfaces the error state`() = runTest {
        coEvery { askLantern(any(), any(), any()) } throws IllegalStateException("boom")
        val vm = viewModel()

        vm.state.test {
            val failed = awaitUntil { it.hasError }
            assertTrue(failed.hasError)
            assertFalse(failed.isLoading)
        }
    }

    @Test
    fun `filing a report emits the original question`() = runTest {
        coEvery { askLantern(any(), any(), any()) } returns onDeviceAnswer
        val vm = viewModel("roof cracked")

        vm.events.test {
            vm.fileReport()
            assertEquals(AnswerEvent.FileReport("roof cracked"), awaitItem())
        }
    }

    @Test
    fun `deeper button is hidden for cloud answers`() = runTest {
        coEvery { askLantern(any(), any(), any()) } returns
            onDeviceAnswer.copy(source = InferenceSource.CLOUD)
        val vm = viewModel()

        vm.state.test {
            val loaded = awaitUntil { it.answer != null }
            assertFalse(loaded.showDeeperButton)
        }
    }
}
