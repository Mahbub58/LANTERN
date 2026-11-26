package com.softvinax.lantern.di

import com.softvinax.lantern.data.ai.FirebaseCloudInference
import com.softvinax.lantern.data.ai.FirebaseOnDeviceInference
import com.softvinax.lantern.domain.ai.CloudInference
import com.softvinax.lantern.domain.ai.OnDeviceInference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase AI Logic adapters. The router still owns every routing decision:
 * on-device is ONLY_ON_DEVICE, cloud is ONLY_IN_CLOUD, and the router decides
 * when to escalate.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindOnDeviceInference(impl: FirebaseOnDeviceInference): OnDeviceInference

    @Binds
    @Singleton
    abstract fun bindCloudInference(impl: FirebaseCloudInference): CloudInference
}
