package com.mahbub.lantern.presentation.navigation

import app.cash.turbine.test
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.usecase.IsLanguageChosenUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.testutil.MainDispatcherRule
import com.mahbub.lantern.testutil.awaitUntil
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `first run shows the language picker`() = runTest {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val isLanguageChosen = mockk<IsLanguageChosenUseCase>()
        every { observeLanguage() } returns flowOf(AppLanguage.EN)
        every { isLanguageChosen() } returns flowOf(false)

        val vm = RootViewModel(observeLanguage, isLanguageChosen)

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertFalse(loaded.languageChosen)
            assertEquals(AppLanguage.EN, loaded.language)
        }
    }

    @Test
    fun `returning user goes straight to the app`() = runTest {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val isLanguageChosen = mockk<IsLanguageChosenUseCase>()
        every { observeLanguage() } returns flowOf(AppLanguage.UR)
        every { isLanguageChosen() } returns flowOf(true)

        val vm = RootViewModel(observeLanguage, isLanguageChosen)

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertTrue(loaded.languageChosen)
            assertEquals(AppLanguage.UR, loaded.language)
        }
    }
}
