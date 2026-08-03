package com.walley.app.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {

    private val crypto = BackupCrypto()

    @Test
    fun `decrypt reverses encrypt`() {
        val key = crypto.generateDataKey()
        val plaintext = "walley backup snapshot".toByteArray()

        val encrypted = crypto.encrypt(key, plaintext)
        val decrypted = crypto.decrypt(key, encrypted)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `two encryptions of the same plaintext produce different ciphertext`() {
        val key = crypto.generateDataKey()
        val plaintext = "same content".toByteArray()

        val first = crypto.encrypt(key, plaintext)
        val second = crypto.encrypt(key, plaintext)

        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun `decrypting with the wrong key fails`() {
        val key = crypto.generateDataKey()
        val wrongKey = crypto.generateDataKey()
        val encrypted = crypto.encrypt(key, "secret".toByteArray())

        assertThrows(Exception::class.java) { crypto.decrypt(wrongKey, encrypted) }
    }

    @Test
    fun `tampering with ciphertext is detected`() {
        val key = crypto.generateDataKey()
        val encrypted = crypto.encrypt(key, "secret".toByteArray())
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()

        assertThrows(Exception::class.java) { crypto.decrypt(key, encrypted) }
    }

    @Test
    fun `generateDataKey returns 256 bits`() {
        org.junit.Assert.assertEquals(32, crypto.generateDataKey().size)
    }
}
