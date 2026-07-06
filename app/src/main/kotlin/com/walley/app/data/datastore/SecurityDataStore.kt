package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security")

class SecurityDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val pinSaltKey = stringPreferencesKey("pin_salt")
    private val fingerprintEnabledKey = booleanPreferencesKey("fingerprint_unlock_enabled")

    val pinSet: Flow<Boolean> = context.securityDataStore.data
        .map { preferences -> preferences[pinHashKey] != null }

    val fingerprintEnabled: Flow<Boolean> = context.securityDataStore.data
        .map { preferences -> preferences[fingerprintEnabledKey] ?: false }

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        context.securityDataStore.edit { preferences ->
            preferences[pinSaltKey] = salt.toHex()
            preferences[pinHashKey] = hash(pin, salt)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val preferences = context.securityDataStore.data.first()
        val salt = preferences[pinSaltKey]?.fromHex() ?: return false
        val storedHash = preferences[pinHashKey] ?: return false
        return hash(pin, salt) == storedHash
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.securityDataStore.edit { preferences -> preferences[fingerprintEnabledKey] = enabled }
    }

    private fun hash(pin: String, salt: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(salt + pin.encodeToByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
