package com.mahbub.lantern.di

import com.mahbub.lantern.data.appcheck.AppCheckMonitor
import com.mahbub.lantern.data.remote.AndroidConnectivityRepository
import com.mahbub.lantern.data.remote.AndroidLocationRepository
import com.mahbub.lantern.data.remote.FirebaseAlertRepository
import com.mahbub.lantern.data.remote.FirebaseAuthRepository
import com.mahbub.lantern.data.remote.FirebaseConfigRepository
import com.mahbub.lantern.data.remote.FirebaseReportRepository
import com.mahbub.lantern.data.remote.FirebaseSafetyCardRepository
import com.mahbub.lantern.domain.repository.AlertRepository
import com.mahbub.lantern.domain.repository.AppCheckRepository
import com.mahbub.lantern.domain.repository.AuthRepository
import com.mahbub.lantern.domain.repository.ConfigRepository
import com.mahbub.lantern.domain.repository.ConnectivityRepository
import com.mahbub.lantern.domain.repository.LocationRepository
import com.mahbub.lantern.domain.repository.NetworkSimulator
import com.mahbub.lantern.domain.repository.ReportRepository
import com.mahbub.lantern.domain.repository.SafetyCardRepository
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
    abstract fun bindAppCheckRepository(impl: AppCheckMonitor): AppCheckRepository

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
