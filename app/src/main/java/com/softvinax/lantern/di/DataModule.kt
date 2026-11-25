package com.softvinax.lantern.di

import com.softvinax.lantern.data.fake.FakeAlertRepository
import com.softvinax.lantern.data.fake.FakeAuthRepository
import com.softvinax.lantern.data.fake.FakeConfigRepository
import com.softvinax.lantern.data.fake.FakeConnectivityRepository
import com.softvinax.lantern.data.fake.FakeLocationRepository
import com.softvinax.lantern.data.fake.FakeReportRepository
import com.softvinax.lantern.data.fake.FakeSafetyCardRepository
import com.softvinax.lantern.domain.repository.AlertRepository
import com.softvinax.lantern.domain.repository.AuthRepository
import com.softvinax.lantern.domain.repository.ConfigRepository
import com.softvinax.lantern.domain.repository.ConnectivityRepository
import com.softvinax.lantern.domain.repository.LocationRepository
import com.softvinax.lantern.domain.repository.NetworkSimulator
import com.softvinax.lantern.domain.repository.ReportRepository
import com.softvinax.lantern.domain.repository.SafetyCardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PHASE 2 SWAP POINT: replace each fake with the real implementation here.
 * Presentation and domain never see these classes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSafetyCardRepository(impl: FakeSafetyCardRepository): SafetyCardRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: FakeReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: FakeAlertRepository): AlertRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: FakeConfigRepository): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(impl: FakeConnectivityRepository): ConnectivityRepository

    @Binds
    @Singleton
    abstract fun bindNetworkSimulator(impl: FakeConnectivityRepository): NetworkSimulator

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: FakeLocationRepository): LocationRepository
}
