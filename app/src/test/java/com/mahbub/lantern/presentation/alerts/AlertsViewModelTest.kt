package com.mahbub.lantern.presentation.alerts

import app.cash.turbine.test
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.usecase.ObserveAlertsUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.RefreshAlertsUseCase
import com.mahbub.lantern.testutil.MainDispatcherRule
import com.mahbub.lantern.testutil.awaitUntil
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

class AlertsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val alert = Alert(
        id = "a1",
        level = AlertLevel.URGENT,
        title = LocalizedText(en = "Flood water rising"),
        message = LocalizedText(en = "Move to higher ground"),
        region = "Sector 4",
        issuedAt = 1L,
    )

    @Test
    fun `alerts, connectivity and language are combined`() = runTest {
        val observeAlerts = mockk<ObserveAlertsUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        val refreshAlerts = mockk<RefreshAlertsUseCase>()
        every { observeAlerts() } returns flowOf(listOf(alert))
        every { observeConnectivity() } returns flowOf(true)
        every { observeLanguage() } returns flowOf(AppLanguage.UR)
        coEvery { refreshAlerts() } returns Unit

        val vm = AlertsViewModel(observeAlerts, observeConnectivity, observeLanguage, refreshAlerts)

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals(1, loaded.alerts.size)
            assertEquals(AppLanguage.UR, loaded.language)
            assertFalse(loaded.hasError)
        }
        coVerify { refreshAlerts() }
    }
}
