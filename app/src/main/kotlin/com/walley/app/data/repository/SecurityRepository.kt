package com.walley.app.data.repository

import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun observePinSet(): Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    fun observeFingerprintUnlock(): Flow<Boolean>
    suspend fun setFingerprintUnlock(enabled: Boolean)
}
