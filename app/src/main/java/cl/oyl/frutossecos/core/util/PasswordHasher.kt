package cl.oyl.frutossecos.core.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hash de contraseñas con PBKDF2-HMAC-SHA256 y sal aleatoria. Nunca se guarda texto plano. */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class Hashed(val hash: String, val salt: String)

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String = newSalt()): Hashed {
        val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH_BITS)
        try {
            val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            return Hashed(Base64.getEncoder().encodeToString(key), salt)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, expectedHash: String, salt: String): Boolean {
        val actual = Base64.getDecoder().decode(hash(password, salt).hash)
        val expected = Base64.getDecoder().decode(expectedHash)
        return MessageDigest.isEqual(actual, expected)
    }
}
