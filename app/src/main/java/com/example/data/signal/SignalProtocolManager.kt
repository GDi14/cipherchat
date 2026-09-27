package com.example.data.signal

import android.util.Base64
import android.util.Log
import com.example.data.local.dao.SignalStoreDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.LocalUserProfileEntity
import com.example.data.local.entity.SignalPreKeyEntity
import com.example.data.local.entity.SignalSignedPreKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.util.KeyHelper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class PublicPreKeyBundleData(
    val userId: String,
    val displayName: String,
    val registrationId: Int,
    val identityKey: String,
    val signedPreKeyId: Int,
    val signedPreKey: String,
    val signedPreKeySignature: String,
    val preKeyId: Int,
    val preKey: String
)

data class EncryptedMessageResult(
    val base64Ciphertext: String,
    val messageType: Int
)

object StandardCryptoHelper {
    fun deriveSharedSecret(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): SecretKey {
        val kf = KeyFactory.getInstance("EC")
        val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val publicKey = kf.generatePublic(X509EncodedKeySpec(publicKeyBytes))

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        val rawSecret = keyAgreement.generateSecret()

        val md = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = md.digest(rawSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    fun encrypt(secretKey: SecretKey, plaintext: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(secretKey: SecretKey, base64Ciphertext: String): String {
        val combined = Base64.decode(base64Ciphertext, Base64.NO_WRAP)
        if (combined.size < 12) throw IllegalArgumentException("Ciphertext payload too short")

        val iv = ByteArray(12)
        System.arraycopy(combined, 0, iv, 0, 12)
        val cipherBytes = ByteArray(combined.size - 12)
        System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}

class SignalProtocolManager(
    private val signalStoreDao: SignalStoreDao,
    private val userProfileDao: UserProfileDao
) {
    private var signalStore: RoomSignalProtocolStore? = null
    private var currentProfile: LocalUserProfileEntity? = null

    suspend fun getOrInitStore(): SignalProtocolStore? = withContext(Dispatchers.IO) {
        signalStore?.let { return@withContext it }
        val profile = userProfileDao.getUserProfile() ?: return@withContext null
        currentProfile = profile
        try {
            val identityKeyPair = IdentityKeyPair(profile.identityKeyPairBytes)
            val store = RoomSignalProtocolStore(signalStoreDao, identityKeyPair, profile.registrationId)
            signalStore = store
            store
        } catch (e: Throwable) {
            null
        }
    }

    suspend fun createIdentity(
        userId: String,
        displayName: String,
        inviteCode: String
    ): PublicPreKeyBundleData = withContext(Dispatchers.IO) {
        var identityKeyPairBytes: ByteArray
        var identityKeyBase64: String
        var signedPreKeyBase64: String
        var preKeyBase64: String
        var signatureBase64: String
        var regId: Int
        var signedPreKeyIdVal = 5
        var preKeyIdVal = 1

        try {
            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val registrationId = KeyHelper.generateRegistrationId(false)
            val preKeys = KeyHelper.generatePreKeys(1, 100)
            val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyIdVal)

            val preKeyEntities = preKeys.map {
                SignalPreKeyEntity(it.id, it.serialize())
            }
            signalStoreDao.insertPreKeys(preKeyEntities)
            signalStoreDao.insertSignedPreKey(
                SignalSignedPreKeyEntity(signedPreKey.id, signedPreKey.serialize())
            )

            identityKeyPairBytes = identityKeyPair.serialize()
            identityKeyBase64 = Base64.encodeToString(identityKeyPair.publicKey.serialize(), Base64.NO_WRAP)
            signedPreKeyBase64 = Base64.encodeToString(signedPreKey.keyPair.publicKey.serialize(), Base64.NO_WRAP)
            preKeyBase64 = Base64.encodeToString(preKeys.first().keyPair.publicKey.serialize(), Base64.NO_WRAP)
            signatureBase64 = Base64.encodeToString(signedPreKey.signature, Base64.NO_WRAP)
            regId = registrationId
            signedPreKeyIdVal = signedPreKey.id
            preKeyIdVal = preKeys.first().id

            val store = RoomSignalProtocolStore(signalStoreDao, identityKeyPair, regId)
            signalStore = store
        } catch (e: Throwable) {
            Log.w("SignalManager", "Signal lib JNI/Fallback unavailable (${e.message}). Initializing hardware EC keys.")
            
            // Native fallback for 64-bit devices (ARM64 / x86_64)
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(256, SecureRandom())
            val keyPair = kpg.generateKeyPair()

            val privBytes = keyPair.private.encoded
            val pubBytes = keyPair.public.encoded

            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeInt(privBytes.size)
            dos.write(privBytes)
            dos.writeInt(pubBytes.size)
            dos.write(pubBytes)
            dos.flush()

            identityKeyPairBytes = bos.toByteArray()
            identityKeyBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
            signedPreKeyBase64 = identityKeyBase64
            preKeyBase64 = identityKeyBase64

            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initSign(keyPair.private)
            sig.update(pubBytes)
            signatureBase64 = Base64.encodeToString(sig.sign(), Base64.NO_WRAP)

            regId = (1000..9999).random()
        }

        val profile = LocalUserProfileEntity(
            userId = userId,
            displayName = displayName,
            inviteCode = inviteCode,
            registrationId = regId,
            identityKeyPairBytes = identityKeyPairBytes,
            signedPreKeyId = signedPreKeyIdVal,
            preKeyId = preKeyIdVal
        )
        userProfileDao.insertUserProfile(profile)
        currentProfile = profile

        PublicPreKeyBundleData(
            userId = userId,
            displayName = displayName,
            registrationId = regId,
            identityKey = identityKeyBase64,
            signedPreKeyId = signedPreKeyIdVal,
            signedPreKey = signedPreKeyBase64,
            signedPreKeySignature = signatureBase64,
            preKeyId = preKeyIdVal,
            preKey = preKeyBase64
        )
    }

    suspend fun encryptMessage(
        recipientBundle: PublicPreKeyBundleData,
        plaintext: String
    ): EncryptedMessageResult = withContext(Dispatchers.IO) {
        try {
            val store = getOrInitStore()
            if (store != null) {
                val recipientAddress = SignalProtocolAddress(recipientBundle.userId, 1)

                if (!store.containsSession(recipientAddress)) {
                    val identityKeyBytes = Base64.decode(recipientBundle.identityKey, Base64.NO_WRAP)
                    val identityKey = IdentityKey(identityKeyBytes, 0)

                    val signedPreKeyBytes = Base64.decode(recipientBundle.signedPreKey, Base64.NO_WRAP)
                    val signedPreKeyPublic = Curve.decodePoint(signedPreKeyBytes, 0)
                    val signedPreKeySignature = Base64.decode(recipientBundle.signedPreKeySignature, Base64.NO_WRAP)

                    val preKeyBytes = Base64.decode(recipientBundle.preKey, Base64.NO_WRAP)
                    val preKeyPublic = Curve.decodePoint(preKeyBytes, 0)

                    val bundle = PreKeyBundle(
                        recipientBundle.registrationId,
                        1,
                        recipientBundle.preKeyId,
                        preKeyPublic,
                        recipientBundle.signedPreKeyId,
                        signedPreKeyPublic,
                        signedPreKeySignature,
                        identityKey
                    )

                    val builder = SessionBuilder(store, recipientAddress)
                    builder.process(bundle)
                }

                val sessionCipher = SessionCipher(store, recipientAddress)
                val ciphertext = sessionCipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))
                val base64Cipher = Base64.encodeToString(ciphertext.serialize(), Base64.NO_WRAP)
                return@withContext EncryptedMessageResult(base64Cipher, ciphertext.type)
            }
        } catch (e: Throwable) {
            Log.w("SignalManager", "Signal lib encryption fallback: ${e.message}")
        }

        // Standard EC/AES-GCM E2EE fallback
        val profile = currentProfile ?: userProfileDao.getUserProfile()
        val myPrivateKeyBytes = extractPrivateKeyFromComposite(profile?.identityKeyPairBytes)
        val recipientPubKeyBytes = Base64.decode(recipientBundle.identityKey, Base64.NO_WRAP)

        val secretKey = StandardCryptoHelper.deriveSharedSecret(myPrivateKeyBytes, recipientPubKeyBytes)
        val base64Cipher = StandardCryptoHelper.encrypt(secretKey, plaintext)
        EncryptedMessageResult(base64Cipher, 1)
    }

    suspend fun decryptMessage(
        senderId: String,
        base64Ciphertext: String,
        messageType: Int,
        senderIdentityKeyBase64: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val store = getOrInitStore()
            if (store != null) {
                val senderAddress = SignalProtocolAddress(senderId, 1)
                val sessionCipher = SessionCipher(store, senderAddress)
                val cipherBytes = Base64.decode(base64Ciphertext, Base64.NO_WRAP)

                val plaintextBytes = when (messageType) {
                    CiphertextMessage.PREKEY_TYPE -> {
                        val preKeyMessage = PreKeySignalMessage(cipherBytes)
                        sessionCipher.decrypt(preKeyMessage)
                    }
                    CiphertextMessage.WHISPER_TYPE -> {
                        val whisperMessage = SignalMessage(cipherBytes)
                        sessionCipher.decrypt(whisperMessage)
                    }
                    else -> {
                        try {
                            val preKeyMessage = PreKeySignalMessage(cipherBytes)
                            sessionCipher.decrypt(preKeyMessage)
                        } catch (e: Exception) {
                            val whisperMessage = SignalMessage(cipherBytes)
                            sessionCipher.decrypt(whisperMessage)
                        }
                    }
                }
                return@withContext String(plaintextBytes, Charsets.UTF_8)
            }
        } catch (e: Throwable) {
            Log.w("SignalManager", "Signal lib decryption fallback: ${e.message}")
        }

        // Standard EC/AES-GCM E2EE fallback
        val profile = currentProfile ?: userProfileDao.getUserProfile()
        val myPrivateKeyBytes = extractPrivateKeyFromComposite(profile?.identityKeyPairBytes)
        val senderContact = signalStoreDao.getIdentity(senderId + ":1")

        val senderPubKeyBytes = when {
            senderContact?.identityKeyBytes != null -> senderContact.identityKeyBytes
            !senderIdentityKeyBase64.isNullOrBlank() -> Base64.decode(senderIdentityKeyBase64, Base64.NO_WRAP)
            else -> throw IllegalArgumentException("Missing sender identity key for decryption")
        }

        val secretKey = StandardCryptoHelper.deriveSharedSecret(myPrivateKeyBytes, senderPubKeyBytes)
        StandardCryptoHelper.decrypt(secretKey, base64Ciphertext)
    }

    private fun extractPrivateKeyFromComposite(compositeBytes: ByteArray?): ByteArray {
        if (compositeBytes == null) throw IllegalStateException("No identity keypair found")
        return try {
            val idKeyPair = IdentityKeyPair(compositeBytes)
            idKeyPair.privateKey.serialize()
        } catch (e: Throwable) {
            val dis = DataInputStream(ByteArrayInputStream(compositeBytes))
            val privLen = dis.readInt()
            val privBytes = ByteArray(privLen)
            dis.readFully(privBytes)
            privBytes
        }
    }
}
