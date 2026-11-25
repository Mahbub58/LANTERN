package com.softvinax.lantern.di

import com.softvinax.lantern.data.fake.FakeCloudInference
import com.softvinax.lantern.data.fake.FakeOnDeviceInference
import com.softvinax.lantern.domain.ai.CloudInference
import com.softvinax.lantern.domain.ai.OnDeviceInference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PHASE 2 SWAP POINT: bind Firebase AI / Gemini Nano adapters here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindOnDeviceInference(impl: FakeOnDeviceInference): OnDeviceInference

    @Binds
    @Singleton
    abstract fun bindCloudInference(impl: FakeCloudInference): CloudInference
}
