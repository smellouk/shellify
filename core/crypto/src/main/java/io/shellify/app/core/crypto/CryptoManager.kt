package io.shellify.app.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption backed by the Android Keystore.
 *
 * The master key never leaves the Keystore (hardware-backed on supported devices).
 * All sensitive data at rest — database passphrase, cookie snapshots — is
 * encrypted with this key and decrypted only when actively needed.
 *
 * Wire format: [IV 12 bytes][GCM ciphertext + 16-byte auth tag]
 */
class CryptoManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "shellify_master_key"
        private const val PREFS_NAME = "shellify_crypto"
        private const val PREF_DB_PASSPHRASE = "db_passphrase_enc"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
    }

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    // ---------- Key management ----------

    private fun masterKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setKeySize(256)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false)
                        .build()
                )
                generateKey()
            }
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    // ---------- Core encrypt / decrypt ----------

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val iv = cipher.iv                        // Keystore generates a fresh IV each call
        val ciphertext = cipher.doFinal(plain)
        return iv + ciphertext                    // prepend IV so decrypt can recover it
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_LENGTH) { "Encrypted blob too short" }
        val iv = blob.copyOfRange(0, IV_LENGTH)
        val ciphertext = blob.copyOfRange(IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    // ---------- String convenience ----------

    /** Encrypts [plain] and returns Base64-encoded ciphertext (safe for DataStore / SharedPrefs). */
    fun encryptString(plain: String): String =
        Base64.encodeToString(encrypt(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    /** Decrypts a Base64-encoded blob produced by [encryptString]. */
    fun decryptString(encoded: String): String =
        decrypt(Base64.decode(encoded, Base64.NO_WRAP)).toString(Charsets.UTF_8)

    // ---------- Database passphrase ----------

    /**
     * Returns the 32-byte SQLCipher passphrase for this installation.
     *
     * On first call: generates a cryptographically random passphrase, encrypts it
     * with the Keystore key, and stores the ciphertext in private SharedPreferences.
     * On subsequent calls: decrypts and returns the stored passphrase.
     *
     * The passphrase itself is never stored in plaintext anywhere on disk.
     */
    fun databasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_DB_PASSPHRASE, null)
        return if (stored != null) {
            decrypt(Base64.decode(stored, Base64.NO_WRAP))
        } else {
            val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val encryptedB64 = Base64.encodeToString(encrypt(passphrase), Base64.NO_WRAP)
            prefs.edit().putString(PREF_DB_PASSPHRASE, encryptedB64).apply()
            passphrase
        }
    }
}
