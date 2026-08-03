package com.walley.app.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val WRAP_KEY_ALIAS = "walley_backup_wrap_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12
private const val DATA_KEY_LENGTH_BITS = 256

/**
 * Handles the encryption side of Backup & Restore. The snapshot itself is encrypted with a
 * randomly generated AES-256 "data key" rather than a device-bound one, so a fresh install on
 * any device can decrypt it once the raw data key — uploaded alongside the snapshot in the same
 * private Drive folder — is fetched back. A Keystore-backed key here only wraps a local copy of
 * that data key, so it doesn't sit as plaintext in this device's own preferences; it plays no
 * part in decrypting a restore fetched fresh from Drive.
 */
@Singleton
class BackupCrypto @Inject constructor() {

    // Lazy so plain encrypt/decrypt (a fixed AES-GCM transformation with no Android dependency)
    // stays unit-testable on the JVM — only wrap/unwrap touch the Android-only Keystore provider.
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    fun generateDataKey(): ByteArray = ByteArray(DATA_KEY_LENGTH_BITS / 8).also { SecureRandom().nextBytes(it) }

    /** Encrypts [plaintext] with [dataKey]; output is `iv || ciphertext+tag`. */
    fun encrypt(dataKey: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dataKey, "AES"))
        return cipher.iv + cipher.doFinal(plaintext)
    }

    /** Reverses [encrypt]: [payload] is `iv || ciphertext+tag`. */
    fun decrypt(dataKey: ByteArray, payload: ByteArray): ByteArray {
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dataKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    /** Wraps [dataKey] with a Keystore-backed AES key for local-only storage — this output is never uploaded. */
    fun wrapDataKeyForLocalStorage(dataKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapKey())
        return cipher.iv + cipher.doFinal(dataKey)
    }

    fun unwrapDataKeyFromLocalStorage(wrapped: ByteArray): ByteArray {
        val iv = wrapped.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = wrapped.copyOfRange(GCM_IV_LENGTH_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateWrapKey(): SecretKey {
        (keyStore.getKey(WRAP_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(WRAP_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(DATA_KEY_LENGTH_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
