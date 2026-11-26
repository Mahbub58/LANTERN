package com.softvinax.lantern.di

import com.softvinax.lantern.data.remote.AndroidConnectivityRepository
import com.softvinax.lantern.data.remote.AndroidLocationRepository
import com.softvinax.lantern.data.remote.FirebaseAlertRepository
import com.softvinax.lantern.data.remote.FirebaseAuthRepository
import com.softvinax.lantern.data.remote.FirebaseConfigRepository
import com.softvinax.lantern.data.remote.FirebaseReportRepository
import com.softvinax.lantern.data.remote.FirebaseSafetyCardRepository
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
 * Real Firebase implementations. Presentation and domain never see these
 * classes; tests keep using the fakes in data/fake directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSafetyCardRepository(impl: FirebaseSafetyCardRepository): SafetyCardRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: FirebaseReportRepository): ReportRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: FirebaseAlertRepository): AlertRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: FirebaseConfigRepository): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityRepository(impl: AndroidConnectivityRepository): ConnectivityRepository

    @Binds
    @Singleton
    abstract fun bindNetworkSimulator(impl: AndroidConnectivityRepository): NetworkSimulator

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: AndroidLocationRepository): LocationRepository
}
