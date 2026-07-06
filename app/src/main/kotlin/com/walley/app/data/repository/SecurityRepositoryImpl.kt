package com.walley.app.data.repository

import com.walley.app.data.datastore.SecurityDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SecurityRepositoryImpl @Inject constructor(
    private val securityDataStore: SecurityDataStore
) : SecurityRepository {

    override fun observePinSet(): Flow<Boolean> = securityDataStore.pinSet

    override suspend fun setPin(pin: String) {
        securityDataStore.setPin(pin)
    }

    override suspend fun verifyPin(pin: String): Boolean = securityDataStore.verifyPin(pin)

    override fun observeFingerprintUnlock(): Flow<Boolean> = securityDataStore.fingerprintEnabled

    override suspend fun setFingerprintUnlock(enabled: Boolean) {
        securityDataStore.setFingerprintEnabled(enabled)
    }
}
