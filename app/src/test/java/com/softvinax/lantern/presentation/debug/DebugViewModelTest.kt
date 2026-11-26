package com.softvinax.lantern.presentation.debug

import app.cash.turbine.test
import com.softvinax.lantern.domain.ai.NanoStatus
import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.RoutingConfig
import com.softvinax.lantern.domain.usecase.GetAnonymousUidUseCase
import com.softvinax.lantern.domain.usecase.ObserveAlertLevelUseCase
import com.softvinax.lantern.domain.usecase.ObserveCardsVersionUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLastInferenceSourceUseCase
import com.softvinax.lantern.domain.usecase.ObserveNanoStatusUseCase
import com.softvinax.lantern.domain.usecase.ObserveRoutingConfigUseCase
import com.softvinax.lantern.domain.usecase.SetAlertLevelUseCase
import com.softvinax.lantern.domain.usecase.SetRoutingConfigUseCase
import com.softvinax.lantern.domain.usecase.SetSimulatedOnlineUseCase
import com.softvinax.lantern.testutil.MainDispatcherRule
import com.softvinax.lantern.testutil.awaitUntil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class DebugViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val setSimulatedOnline = mockk<SetSimulatedOnlineUseCase>()
    private val setRoutingConfig = mockk<SetRoutingConfigUseCase>()
    private val setAlertLevel = mockk<SetAlertLevelUseCase>()

    private fun viewModel(): DebugViewModel {
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val observeLastSource = mockk<ObserveLastInferenceSourceUseCase>()
        val observeRoutingConfig = mockk<ObserveRoutingConfigUseCase>()
        val observeNanoStatus = mockk<ObserveNanoStatusUseCase>()
        val observeCardsVersion = mockk<ObserveCardsVersionUseCase>()
        val observeAlertLevel = mockk<ObserveAlertLevelUseCase>()
        val getAnonymousUid = mockk<GetAnonymousUidUseCase>()

        every { observeConnectivity() } returns flowOf(false)
        every { observeLastSource() } returns flowOf<InferenceSource?>(InferenceSource.CARD_TEXT)
        every { observeRoutingConfig() } returns flowOf(RoutingConfig())
        every { observeNanoStatus() } returns flowOf(NanoStatus.READY)
        every { observeCardsVersion() } returns flowOf("1.0.0")
        every { observeAlertLevel() } returns flowOf(AlertLevel.WARNING)
        coEvery { getAnonymousUid() } returns "anon-1"
        coEvery { setSimulatedOnline(any()) } returns Unit
        coEvery { setRoutingConfig(any()) } returns Unit
        coEvery { setAlertLevel(any()) } returns Unit

        return DebugViewModel(
            observeConnectivity = observeConnectivity,
            observeLastInferenceSource = observeLastSource,
            observeRoutingConfig = observeRoutingConfig,
            observeNanoStatus = observeNanoStatus,
            observeCardsVersion = observeCardsVersion,
            observeAlertLevel = observeAlertLevel,
            setSimulatedOnline = setSimulatedOnline,
            setRoutingConfig = setRoutingConfig,
            setAlertLevel = setAlertLevel,
            getAnonymousUid = getAnonymousUid,
        )
    }

    @Test
    fun `debug state loads every diagnostic`() = runTest {
        val vm = viewModel()

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals(InferenceSource.CARD_TEXT, loaded.lastSource)
            assertEquals(NanoStatus.READY, loaded.nanoStatus)
            assertEquals("anon-1", loaded.uid)
            assertFalse(loaded.hasError)
        }
    }

    @Test
    fun `turning the simulated network on is delegated`() = runTest {
        val vm = viewModel()
        vm.state.test { awaitUntil { !it.isLoading } }

        vm.setOnline(true)

        assertEquals(true, vm.state.value.isOnline)
        coVerify { setSimulatedOnline(true) }
    }

    @Test
    fun `threshold edits are clamped to zero and one`() = runTest {
        val vm = viewModel()
        vm.state.test { awaitUntil { !it.isLoading } }

        vm.setThreshold(4.5f)

        assertEquals(1f, vm.state.value.config.confidenceThreshold, 0.0001f)
        coVerify { setRoutingConfig(match { it.confidenceThreshold == 1f }) }
    }

    @Test
    fun `reset restores defaults and emits a confirmation`() = runTest {
        val vm = viewModel()
        vm.state.test { awaitUntil { !it.isLoading } }
        vm.setOnline(true)
        vm.updateAlertLevel(AlertLevel.URGENT)

        vm.events.test {
            vm.reset()
            assertEquals(DebugEvent.ResetDone, awaitItem())
        }

        assertFalse(vm.state.value.isOnline)
        assertEquals(AlertLevel.WARNING, vm.state.value.alertLevel)
        assertEquals(RoutingConfig(), vm.state.value.config)
    }
}
