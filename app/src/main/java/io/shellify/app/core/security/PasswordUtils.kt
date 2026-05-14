package io.shellify.app.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val PBKDF2_ITERATIONS = 310_000
private const val KEY_LENGTH_BITS = 256
private const val SALT_LENGTH = 32
private const val V2_PREFIX = "v2:"

/**
 * Hash a password with PBKDF2-HMAC-SHA256 + random salt.
 * Output format: "v2:<base64(salt)>:<base64(hash)>"
 */
fun hashPassword(password: String): String {
    val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
    val hash = deriveKey(password, salt)
    val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
    val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
    return "$V2_PREFIX$saltB64:$hashB64"
}

fun verifyPassword(input: String, stored: String): Boolean = when {
    stored.startsWith(V2_PREFIX) -> verifyPbkdf2(input, stored)
    else -> verifyLegacySha256(input, stored)   // migrate on next setPassword call
}

/** Returns true when the stored hash uses the old unsalted SHA-256 format. */
fun isLegacyHash(stored: String): Boolean = !stored.startsWith(V2_PREFIX)

// ── internal ─────────────────────────────────────────────────────────────────

private fun verifyPbkdf2(input: String, stored: String): Boolean {
    val parts = stored.removePrefix(V2_PREFIX).split(":")
    if (parts.size != 2) return false
    val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
    val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
    val actual = deriveKey(input, salt)
    return MessageDigest.isEqual(actual, expected)
}

private fun verifyLegacySha256(input: String, stored: String): Boolean {
    val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return MessageDigest.isEqual(
        hash.toByteArray(Charsets.UTF_8),
        stored.toByteArray(Charsets.UTF_8),
    )
}

private fun deriveKey(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}
