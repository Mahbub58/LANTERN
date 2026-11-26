package com.softvinax.lantern.presentation.settings

import app.cash.turbine.test
import com.softvinax.lantern.domain.ai.NanoStatus
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyNumber
import com.softvinax.lantern.domain.usecase.GetAnonymousUidUseCase
import com.softvinax.lantern.domain.usecase.GetEmergencyNumbersUseCase
import com.softvinax.lantern.domain.usecase.ObserveCardsVersionUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLanguageUseCase
import com.softvinax.lantern.domain.usecase.ObserveNanoStatusUseCase
import com.softvinax.lantern.domain.usecase.SetLanguageUseCase
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

class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val numbers = listOf(EmergencyNumber("Rescue 1122", "1122", "Rescue"))
    private val setLanguage = mockk<SetLanguageUseCase>()

    private fun viewModel(): SettingsViewModel {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val observeNanoStatus = mockk<ObserveNanoStatusUseCase>()
        val observeCardsVersion = mockk<ObserveCardsVersionUseCase>()
        val getEmergencyNumbers = mockk<GetEmergencyNumbersUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val getAnonymousUid = mockk<GetAnonymousUidUseCase>()

        every { observeLanguage() } returns flowOf(AppLanguage.EN)
        every { observeNanoStatus() } returns flowOf(NanoStatus.READY)
        every { observeCardsVersion() } returns flowOf("1.0.0")
        every { getEmergencyNumbers() } returns flowOf(numbers)
        every { observeConnectivity() } returns flowOf(false)
        coEvery { getAnonymousUid() } returns "anon-1"
        coEvery { setLanguage(any()) } returns Unit

        return SettingsViewModel(
            observeLanguage = observeLanguage,
            observeNanoStatus = observeNanoStatus,
            observeCardsVersion = observeCardsVersion,
            getEmergencyNumbers = getEmergencyNumbers,
            observeConnectivity = observeConnectivity,
            setLanguage = setLanguage,
            getAnonymousUid = getAnonymousUid,
        )
    }

    @Test
    fun `settings combine every source`() = runTest {
        val vm = viewModel()

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals(AppLanguage.EN, loaded.language)
            assertEquals(NanoStatus.READY, loaded.nanoStatus)
            assertEquals("1.0.0", loaded.cardsVersion)
            assertEquals(numbers, loaded.emergencyNumbers)
            assertEquals("anon-1", loaded.uid)
            assertFalse(loaded.hasError)
        }
    }

    @Test
    fun `selecting a language saves it and emits a confirmation`() = runTest {
        val vm = viewModel()
        vm.state.test { awaitUntil { !it.isLoading } }

        vm.events.test {
            vm.selectLanguage(AppLanguage.UR)
            assertEquals(SettingsEvent.LanguageSaved(AppLanguage.UR), awaitItem())
        }

        coVerify { setLanguage(AppLanguage.UR) }
        assertFalse(vm.state.value.isSaving)
    }
}
