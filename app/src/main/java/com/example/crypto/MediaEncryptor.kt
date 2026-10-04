package com.example.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedMediaResult(
    val encryptedBytes: ByteArray,
    val keyBase64: String,
    val ivBase64: String
)

/**
 * Standard AES-256-GCM media encryptor for photos, videos, and voice recordings.
 * Media files are encrypted on the client device prior to upload.
 * Keys and IVs are transmitted ONLY inside the end-to-end encrypted libsignal message payload.
 */
object MediaEncryptor {

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encrypt(plaintext: ByteArray): EncryptedMediaResult {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE)
        val secretKey: SecretKey = keyGen.generateKey()

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedMediaResult(
            encryptedBytes = ciphertext,
            keyBase64 = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(ciphertext: ByteArray, keyBase64: String, ivBase64: String): ByteArray {
        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes))
        return cipher.doFinal(ciphertext)
    }
}
