package com.example.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedFileData(
    val encryptedBytes: ByteArray,
    val base64Key: String,
    val base64Iv: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedFileData
        return encryptedBytes.contentEquals(other.encryptedBytes) &&
                base64Key == other.base64Key &&
                base64Iv == other.base64Iv
    }

    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + base64Key.hashCode()
        result = 31 * result + base64Iv.hashCode()
        return result
    }
}

object AttachmentCrypto {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun encryptBytes(plainBytes: ByteArray): EncryptedFileData {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, SecureRandom())
        val secretKey: SecretKey = keyGen.generateKey()

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainBytes)

        val base64Key = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
        val base64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)

        return EncryptedFileData(
            encryptedBytes = cipherBytes,
            base64Key = base64Key,
            base64Iv = base64Iv
        )
    }

    fun decryptBytes(cipherBytes: ByteArray, base64Key: String, base64Iv: String): ByteArray {
        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        val ivBytes = Base64.decode(base64Iv, Base64.NO_WRAP)

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherBytes)
    }
}
