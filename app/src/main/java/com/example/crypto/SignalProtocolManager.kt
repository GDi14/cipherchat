package com.example.crypto

import android.content.Context
import android.util.Base64
import com.example.data.local.SecurePreferencesManager
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
@Serializable
data class PublicPrekeyBundle(
    @Json(name = "identityKey") @SerialName("identityKey") val identityKey: String,
    @Json(name = "registrationId") @SerialName("registrationId") val registrationId: Int,
    @Json(name = "signedPrekeyId") @SerialName("signedPrekeyId") val signedPrekeyId: Int,
    @Json(name = "signedPrekey") @SerialName("signedPrekey") val signedPrekey: String,
    @Json(name = "signedPrekeySignature") @SerialName("signedPrekeySignature") val signedPrekeySignature: String,
    @Json(name = "oneTimePrekeys") @SerialName("oneTimePrekeys") val oneTimePrekeys: List<OneTimePrekeyItem> = emptyList()
)

@JsonClass(generateAdapter = true)
@Serializable
data class OneTimePrekeyItem(
    @Json(name = "id") @SerialName("id") val id: Int = 0,
    @Json(name = "key") @SerialName("key") val key: String = ""
)

@JsonClass(generateAdapter = true)
@Serializable
data class EncryptedMessageResult(
    val type: Int,
    val ciphertextBase64: String
)

/**
 * High-level manager orchestrating official libsignal X3DH + Double Ratchet for DMs
 * and Sender Keys for group encryption.
 */
class SignalProtocolManager(
    context: Context,
    private val securePrefs: SecurePreferencesManager
) {
    val store: KeystoreSignalProtocolStore
    val localUserId: String = securePrefs.getOrCreateUserId()
    val localRegistrationId: Int
    val localIdentityKeyPair: IdentityKeyPair

    init {
        val savedIdentityB64 = securePrefs.encryptedPrefs.getString(PREF_IDENTITY_KEY_PAIR, null)
        val savedRegId = securePrefs.encryptedPrefs.getInt(PREF_REGISTRATION_ID, -1)

        val pair = if (savedIdentityB64 != null) {
            try {
                IdentityKeyPair(Base64.decode(savedIdentityB64, Base64.NO_WRAP))
            } catch (e: Exception) {
                val newPair = KeyHelper.generateIdentityKeyPair()
                securePrefs.encryptedPrefs.edit()
                    .putString(PREF_IDENTITY_KEY_PAIR, Base64.encodeToString(newPair.serialize(), Base64.NO_WRAP))
                    .apply()
                newPair
            }
        } else {
            val newPair = KeyHelper.generateIdentityKeyPair()
            securePrefs.encryptedPrefs.edit()
                .putString(PREF_IDENTITY_KEY_PAIR, Base64.encodeToString(newPair.serialize(), Base64.NO_WRAP))
                .apply()
            newPair
        }

        val regId = if (savedRegId > 0) {
            savedRegId
        } else {
            val newId = KeyHelper.generateRegistrationId(false)
            securePrefs.encryptedPrefs.edit().putInt(PREF_REGISTRATION_ID, newId).apply()
            newId
        }

        localIdentityKeyPair = pair
        localRegistrationId = regId
        store = KeystoreSignalProtocolStore(securePrefs.encryptedPrefs, pair, regId)
    }

    /**
     * Generates a fresh batch of prekeys and signed prekey to upload to Supabase.
     */
    fun generatePublicPrekeyBundle(): PublicPrekeyBundle {
        val signedPreKeyId = 1
        val signedPreKey = KeyHelper.generateSignedPreKey(localIdentityKeyPair, signedPreKeyId)
        store.storeSignedPreKey(signedPreKeyId, signedPreKey)

        val preKeys = KeyHelper.generatePreKeys(1, 20)
        val oneTimeList = mutableListOf<OneTimePrekeyItem>()
        for (pk in preKeys) {
            store.storePreKey(pk.id, pk)
            val pkB64 = Base64.encodeToString(pk.keyPair.publicKey.serialize(), Base64.NO_WRAP)
            oneTimeList.add(OneTimePrekeyItem(pk.id, pkB64))
        }

        val idKeyB64 = Base64.encodeToString(localIdentityKeyPair.publicKey.serialize(), Base64.NO_WRAP)
        val signedPreKeyB64 = Base64.encodeToString(signedPreKey.keyPair.publicKey.serialize(), Base64.NO_WRAP)
        val signatureB64 = Base64.encodeToString(signedPreKey.signature, Base64.NO_WRAP)

        return PublicPrekeyBundle(
            identityKey = idKeyB64,
            registrationId = localRegistrationId,
            signedPrekeyId = signedPreKeyId,
            signedPrekey = signedPreKeyB64,
            signedPrekeySignature = signatureB64,
            oneTimePrekeys = oneTimeList
        )
    }

    /**
     * Initializes a 1:1 X3DH session with a remote recipient using their public PreKeyBundle.
     */
    fun initializeSessionWithRecipient(recipientUserId: String, bundle: PublicPrekeyBundle) {
        val remoteAddress = SignalProtocolAddress(recipientUserId, 1)
        val sessionBuilder = SessionBuilder(store, remoteAddress)

        val remoteIdentityKey = IdentityKey(Base64.decode(bundle.identityKey, Base64.NO_WRAP), 0)
        val remoteSignedPreKey = Curve.decodePoint(Base64.decode(bundle.signedPrekey, Base64.NO_WRAP), 0)
        val remoteSignature = Base64.decode(bundle.signedPrekeySignature, Base64.NO_WRAP)

        val firstOneTime = bundle.oneTimePrekeys.firstOrNull()
        val preKeyId = firstOneTime?.id ?: -1
        val preKey = if (firstOneTime != null) {
            Curve.decodePoint(Base64.decode(firstOneTime.key, Base64.NO_WRAP), 0)
        } else {
            null
        }

        val preKeyBundle = PreKeyBundle(
            bundle.registrationId,
            1,
            preKeyId,
            preKey,
            bundle.signedPrekeyId,
            remoteSignedPreKey,
            remoteSignature,
            remoteIdentityKey
        )

        sessionBuilder.process(preKeyBundle)
    }

    /**
     * Encrypts a 1:1 message using libsignal Double Ratchet.
     */
    fun encryptDirectMessage(recipientUserId: String, plaintextBytes: ByteArray): EncryptedMessageResult {
        val remoteAddress = SignalProtocolAddress(recipientUserId, 1)
        val sessionCipher = SessionCipher(store, remoteAddress)
        val ciphertextMessage = sessionCipher.encrypt(plaintextBytes)
        val type = ciphertextMessage.type
        val b64 = Base64.encodeToString(ciphertextMessage.serialize(), Base64.NO_WRAP)
        return EncryptedMessageResult(type, b64)
    }

    /**
     * Decrypts a 1:1 message using libsignal Double Ratchet.
     */
    fun decryptDirectMessage(senderUserId: String, messageType: Int, ciphertextBase64: String): ByteArray {
        val remoteAddress = SignalProtocolAddress(senderUserId, 1)
        val sessionCipher = SessionCipher(store, remoteAddress)
        val rawBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        return when (messageType) {
            CiphertextMessage.PREKEY_TYPE -> {
                val preKeyMessage = PreKeySignalMessage(rawBytes)
                sessionCipher.decrypt(preKeyMessage)
            }
            CiphertextMessage.WHISPER_TYPE -> {
                val signalMessage = SignalMessage(rawBytes)
                sessionCipher.decrypt(signalMessage)
            }
            else -> {
                // Fallback attempt
                try {
                    sessionCipher.decrypt(SignalMessage(rawBytes))
                } catch (e: Exception) {
                    sessionCipher.decrypt(PreKeySignalMessage(rawBytes))
                }
            }
        }
    }

    /**
     * Creates or retrieves a group SenderKeyDistributionMessage for this device in the group.
     */
    fun createGroupDistributionMessage(groupId: String): String {
        val senderAddress = SignalProtocolAddress(localUserId, 1)
        val senderKeyName = SenderKeyName(groupId, senderAddress)
        val groupSessionBuilder = GroupSessionBuilder(store)
        val distributionMessage = groupSessionBuilder.create(senderKeyName)
        return Base64.encodeToString(distributionMessage.serialize(), Base64.NO_WRAP)
    }

    /**
     * Processes an incoming SenderKeyDistributionMessage from a group participant.
     */
    fun processGroupDistributionMessage(groupId: String, senderUserId: String, distributionMessageBase64: String) {
        val senderAddress = SignalProtocolAddress(senderUserId, 1)
        val senderKeyName = SenderKeyName(groupId, senderAddress)
        val groupSessionBuilder = GroupSessionBuilder(store)
        val bytes = Base64.decode(distributionMessageBase64, Base64.NO_WRAP)
        val distributionMessage = SenderKeyDistributionMessage(bytes)
        groupSessionBuilder.process(senderKeyName, distributionMessage)
    }

    /**
     * Encrypts a message to the group using Sender Keys.
     */
    fun encryptGroupMessage(groupId: String, plaintextBytes: ByteArray): EncryptedMessageResult {
        val senderAddress = SignalProtocolAddress(localUserId, 1)
        val senderKeyName = SenderKeyName(groupId, senderAddress)
        val groupCipher = GroupCipher(store, senderKeyName)
        val ciphertext = groupCipher.encrypt(plaintextBytes)
        val b64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return EncryptedMessageResult(CiphertextMessage.SENDERKEY_TYPE, b64)
    }

    /**
     * Decrypts a group message from a participant using their Sender Key.
     */
    fun decryptGroupMessage(groupId: String, senderUserId: String, ciphertextBase64: String): ByteArray {
        val senderAddress = SignalProtocolAddress(senderUserId, 1)
        val senderKeyName = SenderKeyName(groupId, senderAddress)
        val groupCipher = GroupCipher(store, senderKeyName)
        val bytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        return groupCipher.decrypt(bytes)
    }

    companion object {
        private const val PREF_IDENTITY_KEY_PAIR = "pref_signal_local_identity_key_pair"
        private const val PREF_REGISTRATION_ID = "pref_signal_local_registration_id"
    }
}
