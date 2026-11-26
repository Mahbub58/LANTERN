package com.softvinax.lantern.presentation.language

import app.cash.turbine.test
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.SetLanguageUseCase
import com.softvinax.lantern.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LanguageViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `selecting and confirming saves the language`() = runTest {
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val setLanguage = mockk<SetLanguageUseCase>()
        every { observeConnectivity() } returns flowOf(false)
        coEvery { setLanguage(AppLanguage.ROMAN_UR) } returns Unit

        val vm = LanguageViewModel(observeConnectivity, setLanguage)
        vm.select(AppLanguage.ROMAN_UR)

        vm.events.test {
            vm.confirm()
            assertEquals(LanguageEvent.Saved(AppLanguage.ROMAN_UR), awaitItem())
        }
        assertFalse(vm.state.value.isSaving)
        assertEquals(AppLanguage.ROMAN_UR, vm.state.value.selected)
    }

    @Test
    fun `a failed save surfaces the error`() = runTest {
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val setLanguage = mockk<SetLanguageUseCase>()
        every { observeConnectivity() } returns flowOf(false)
        coEvery { setLanguage(any()) } throws IllegalStateException("disk full")

        val vm = LanguageViewModel(observeConnectivity, setLanguage)
        vm.select(AppLanguage.UR)

        vm.confirm()

        assertEquals(true, vm.state.value.hasError)
        assertFalse(vm.state.value.isSaving)
    }
}
