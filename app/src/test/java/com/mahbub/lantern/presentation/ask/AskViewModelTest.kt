package com.mahbub.lantern.presentation.ask

import app.cash.turbine.test
import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.usecase.ObserveAlertsUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.ObserveNanoStatusUseCase
import com.mahbub.lantern.domain.usecase.RefreshAlertsUseCase
import com.mahbub.lantern.testutil.MainDispatcherRule
import com.mahbub.lantern.testutil.awaitUntil
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AskViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val alert = Alert(
        id = "a1",
        level = AlertLevel.WARNING,
        title = LocalizedText(en = "Flood water rising"),
        message = LocalizedText(en = "Move to higher ground"),
        region = "Sector 4",
        issuedAt = 100L,
    )

    private val alerts = MutableStateFlow(listOf(alert))
    private val online = MutableStateFlow(false)
    private val language = MutableStateFlow(AppLanguage.EN)

    private val observeAlerts = mockk<ObserveAlertsUseCase>()
    private val observeConnectivity = mockk<ObserveConnectivityUseCase>()
    private val observeLanguage = mockk<ObserveLanguageUseCase>()
    private val refreshAlerts = mockk<RefreshAlertsUseCase>()
    private val observeNanoStatus = mockk<ObserveNanoStatusUseCase>()
    private val nanoStatus = MutableStateFlow(NanoStatus.READY)

    private fun viewModel(): AskViewModel {
        every { observeAlerts() } returns alerts
        every { observeConnectivity() } returns online
        every { observeLanguage() } returns language
        every { observeNanoStatus() } returns nanoStatus
        coEvery { refreshAlerts() } returns Unit
        return AskViewModel(
            observeAlerts,
            observeConnectivity,
            observeLanguage,
            refreshAlerts,
            observeNanoStatus,
        )
    }

    @Test
    fun `state loads the latest alert, connectivity and language`() = runTest {
        val vm = viewModel()

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertFalse(loaded.hasError)
            assertFalse(loaded.isOnline)
            assertEquals(AppLanguage.EN, loaded.language)
            assertEquals("a1", loaded.latestAlert?.id)
        }
    }

    @Test
    fun `connectivity changes are reflected`() = runTest {
        val vm = viewModel()

        online.value = true

        vm.state.test {
            val loaded = awaitUntil { it.isOnline }
            assertTrue(loaded.isOnline)
        }
    }

    @Test
    fun `blank question sets the blank error and emits no event`() = runTest {
        val vm = viewModel()
        vm.onQueryChange("   ")

        vm.ask()

        assertTrue(vm.state.value.blankError)
    }

    @Test
    fun `asking emits the trimmed question`() = runTest {
        val vm = viewModel()
        vm.onQueryChange("  flood water rising  ")

        vm.events.test {
            vm.ask()
            assertEquals(AskEvent.OpenAnswer("flood water rising"), awaitItem())
        }
    }

    @Test
    fun `quick chip fills the query and asks immediately`() = runTest {
        val vm = viewModel()

        vm.events.test {
            vm.onQuickChip("Earthquake now")
            assertEquals(AskEvent.OpenAnswer("Earthquake now"), awaitItem())
        }
        assertEquals("Earthquake now", vm.state.value.query)
    }

    @Test
    fun `nano unavailable still opens the answer page for the cloud fallback`() = runTest {
        nanoStatus.value = NanoStatus.UNAVAILABLE
        val vm = viewModel()
        vm.onQueryChange("flood water rising")

        vm.events.test {
            vm.ask()
            assertEquals(AskEvent.OpenAnswer("flood water rising"), awaitItem())
        }

        assertEquals(NanoStatus.UNAVAILABLE, vm.state.value.nanoStatus)
    }

    @Test
    fun `a failing alert stream surfaces the error state`() = runTest {
        every { observeAlerts() } returns flow { throw IllegalStateException("boom") }
        every { observeConnectivity() } returns online
        every { observeLanguage() } returns language
        every { observeNanoStatus() } returns nanoStatus
        coEvery { refreshAlerts() } returns Unit
        val vm = AskViewModel(
            observeAlerts,
            observeConnectivity,
            observeLanguage,
            refreshAlerts,
            observeNanoStatus,
        )

        vm.state.test {
            val failed = awaitUntil { it.hasError }
            assertTrue(failed.hasError)
        }
    }
}
