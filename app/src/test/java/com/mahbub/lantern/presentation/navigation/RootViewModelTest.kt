package com.mahbub.lantern.presentation.navigation

import app.cash.turbine.test
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.repository.AppCheckFailure
import com.mahbub.lantern.domain.usecase.DismissAppCheckFailureUseCase
import com.mahbub.lantern.domain.usecase.IsLanguageChosenUseCase
import com.mahbub.lantern.domain.usecase.ObserveAppCheckFailureUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.RetryAppCheckUseCase
import com.mahbub.lantern.testutil.MainDispatcherRule
import com.mahbub.lantern.testutil.awaitUntil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val appCheckFailure = MutableStateFlow<AppCheckFailure?>(null)
    private val observeAppCheckFailure = mockk<ObserveAppCheckFailureUseCase>()
    private val retryAppCheck = mockk<RetryAppCheckUseCase>(relaxed = true)
    private val dismissAppCheck = mockk<DismissAppCheckFailureUseCase>(relaxed = true)

    private fun viewModel(
        observeLanguage: ObserveLanguageUseCase,
        isLanguageChosen: IsLanguageChosenUseCase,
    ): RootViewModel {
        every { observeAppCheckFailure() } returns appCheckFailure
        return RootViewModel(
            observeLanguage,
            isLanguageChosen,
            observeAppCheckFailure,
            retryAppCheck,
            dismissAppCheck,
        )
    }

    @Test
    fun `first run shows the language picker`() = runTest {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val isLanguageChosen = mockk<IsLanguageChosenUseCase>()
        every { observeLanguage() } returns flowOf(AppLanguage.EN)
        every { isLanguageChosen() } returns flowOf(false)

        val vm = viewModel(observeLanguage, isLanguageChosen)

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

        val vm = viewModel(observeLanguage, isLanguageChosen)

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertTrue(loaded.languageChosen)
            assertEquals(AppLanguage.UR, loaded.language)
        }
    }

    @Test
    fun `app check failure is exposed to the snackbar`() = runTest {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val isLanguageChosen = mockk<IsLanguageChosenUseCase>()
        every { observeLanguage() } returns flowOf(AppLanguage.EN)
        every { isLanguageChosen() } returns flowOf(true)
        val vm = viewModel(observeLanguage, isLanguageChosen)

        appCheckFailure.value = AppCheckFailure(reason = "token failed")

        vm.appCheckFailure.test {
            val failure = awaitUntil { it?.reason == "token failed" }
            assertEquals("token failed", failure?.reason)
        }
    }

    @Test
    fun `snackbar actions reach the repository`() = runTest {
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val isLanguageChosen = mockk<IsLanguageChosenUseCase>()
        every { observeLanguage() } returns flowOf(AppLanguage.EN)
        every { isLanguageChosen() } returns flowOf(true)
        val vm = viewModel(observeLanguage, isLanguageChosen)

        vm.retryAppCheck()
        vm.dismissAppCheckFailure()

        verify(exactly = 1) { retryAppCheck() }
        verify(exactly = 1) { dismissAppCheck() }
    }
}
