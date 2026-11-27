package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.data.fake.FakeConfigRepository
import com.mahbub.lantern.data.fake.FakeConnectivityRepository
import com.mahbub.lantern.data.fake.FakeOnDeviceInference
import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.RoutingConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigUseCasesTest {

    private val config = FakeConfigRepository()
    private val connectivity = FakeConnectivityRepository()
    private val onDevice = FakeOnDeviceInference()

    @Test
    fun `choosing a language also marks the first run complete`() = runTest {
        assertFalse(IsLanguageChosenUseCase(config)().first())

        SetLanguageUseCase(config)(AppLanguage.UR)

        assertEquals(AppLanguage.UR, ObserveLanguageUseCase(config)().first())
        assertTrue(IsLanguageChosenUseCase(config)().first())
    }

    @Test
    fun `routing config is sanitized before storage`() = runTest {
        SetRoutingConfigUseCase(config)(
            RoutingConfig(confidenceThreshold = 4.2f, cloudFallbackEnabled = false),
        )

        val stored = ObserveRoutingConfigUseCase(config)().first()

        assertEquals(1f, stored.confidenceThreshold, 0.0001f)
        assertFalse(stored.cloudFallbackEnabled)
    }

    @Test
    fun `alert level is stored`() = runTest {
        SetAlertLevelUseCase(config)(AlertLevel.URGENT)

        assertEquals(AlertLevel.URGENT, ObserveAlertLevelUseCase(config)().first())
    }

    @Test
    fun `emergency numbers are exposed`() = runTest {
        val numbers = GetEmergencyNumbersUseCase(config)().first()

        assertTrue(numbers.isNotEmpty())
        assertTrue(numbers.any { it.number == "1122" })
    }

    @Test
    fun `cards version is exposed`() = runTest {
        assertTrue(ObserveCardsVersionUseCase(config)().first().isNotBlank())
    }

    @Test
    fun `nano status is exposed`() = runTest {
        assertEquals(NanoStatus.READY, ObserveNanoStatusUseCase(onDevice)().first())
    }

    @Test
    fun `simulated online flips connectivity`() = runTest {
        assertFalse(ObserveConnectivityUseCase(connectivity)().first())

        SetSimulatedOnlineUseCase(connectivity)(true)

        assertTrue(ObserveConnectivityUseCase(connectivity)().first())
    }
}
