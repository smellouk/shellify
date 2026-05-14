package io.shellify.app.core.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val password = "correct-horse-battery-staple"
    private val plaintext = "Hello, Shellify backup!".toByteArray()

    @Test
    fun `encrypt and decrypt round-trip produces original data`() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        val decrypted = BackupCrypto.decrypt(encrypted, password)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt produces output larger than input due to salt, iv, and GCM tag`() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        // salt(32) + iv(12) + ciphertext + GCM tag(16) > plaintext
        assertTrue(encrypted.size > plaintext.size + 32 + 12)
    }

    @Test
    fun `encrypt produces different ciphertext each call due to random salt`() {
        val enc1 = BackupCrypto.encrypt(plaintext, password)
        val enc2 = BackupCrypto.encrypt(plaintext, password)
        // Salts are random; ciphertexts must differ
        assertTrue(!enc1.contentEquals(enc2))
    }

    @Test
    fun `decrypt with wrong password throws exception`() {
        val encrypted = BackupCrypto.encrypt(plaintext, password)
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(encrypted, "wrong-password")
        }
    }

    @Test
    fun `decrypt with truncated data throws IllegalArgumentException`() {
        val tooShort = ByteArray(10)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(tooShort, password)
        }
    }

    @Test
    fun `decrypt with data exactly at boundary throws IllegalArgumentException`() {
        // Boundary is SALT_LENGTH(32) + IV_LENGTH(12) = 44 bytes; must be strictly greater
        val boundary = ByteArray(44)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decrypt(boundary, password)
        }
    }

    @Test
    fun `encrypt and decrypt work with empty plaintext`() {
        val empty = ByteArray(0)
        val encrypted = BackupCrypto.encrypt(empty, password)
        val decrypted = BackupCrypto.decrypt(encrypted, password)
        assertArrayEquals(empty, decrypted)
    }

    @Test
    fun `encrypt and decrypt work with large data`() {
        val large = ByteArray(1024 * 1024) { it.toByte() }
        val encrypted = BackupCrypto.encrypt(large, password)
        val decrypted = BackupCrypto.decrypt(encrypted, password)
        assertArrayEquals(large, decrypted)
    }

    @Test
    fun `encrypt and decrypt work with unicode password`() {
        val unicodePassword = "パスワード123"
        val encrypted = BackupCrypto.encrypt(plaintext, unicodePassword)
        val decrypted = BackupCrypto.decrypt(encrypted, unicodePassword)
        assertArrayEquals(plaintext, decrypted)
    }
}
