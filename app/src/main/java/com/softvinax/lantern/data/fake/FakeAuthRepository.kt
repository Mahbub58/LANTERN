package com.softvinax.lantern.data.fake

import com.softvinax.lantern.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val uidFlow = MutableStateFlow<String?>(null)

    override val uid: Flow<String?> = uidFlow.asStateFlow()

    override suspend fun ensureAnonymousUid(): String {
        uidFlow.value?.let { return it }
        delay(120)
        val uid = "anon-demo-7f3a9c"
        uidFlow.value = uid
        return uid
    }
}
