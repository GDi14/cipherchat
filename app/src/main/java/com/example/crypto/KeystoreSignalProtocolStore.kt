package com.example.crypto

import android.content.SharedPreferences
import android.util.Base64
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.groups.state.SenderKeyStore
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import java.io.IOException

/**
 * Keystore-backed implementation of libsignal's SignalProtocolStore and SenderKeyStore.
 * All sensitive identity keys, signed prekeys, one-time prekeys, sessions, and group sender keys
 * are persisted in EncryptedSharedPreferences (KeyStore-backed AES-256-GCM).
 */
class KeystoreSignalProtocolStore(
    private val prefs: SharedPreferences,
    private val identityKeyPair: IdentityKeyPair,
    private val registrationId: Int
) : SignalProtocolStore, SenderKeyStore {

    init {
        // Persist local identity key pair and registration ID if not already saved
        if (!prefs.contains(KEY_LOCAL_IDENTITY_KEY_PAIR)) {
            val b64 = Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP)
            prefs.edit().putString(KEY_LOCAL_IDENTITY_KEY_PAIR, b64).apply()
        }
        if (!prefs.contains(KEY_LOCAL_REGISTRATION_ID)) {
            prefs.edit().putInt(KEY_LOCAL_REGISTRATION_ID, registrationId).apply()
        }
    }

    // ==========================================
    // IdentityKeyStore
    // ==========================================

    override fun getIdentityKeyPair(): IdentityKeyPair {
        val b64 = prefs.getString(KEY_LOCAL_IDENTITY_KEY_PAIR, null)
        return if (b64 != null) {
            try {
                IdentityKeyPair(Base64.decode(b64, Base64.NO_WRAP))
            } catch (e: Exception) {
                identityKeyPair
            }
        } else {
            identityKeyPair
        }
    }

    override fun getLocalRegistrationId(): Int {
        return prefs.getInt(KEY_LOCAL_REGISTRATION_ID, registrationId)
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = PREF_PREFIX_IDENTITY + address.name + ":" + address.deviceId
        val existing = prefs.getString(key, null)
        val serialized = Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP)
        if (existing == null || existing != serialized) {
            prefs.edit().putString(key, serialized).apply()
            return true
        }
        return false
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        val key = PREF_PREFIX_IDENTITY + address.name + ":" + address.deviceId
        val existingB64 = prefs.getString(key, null) ?: return true
        return try {
            val storedKey = IdentityKey(Base64.decode(existingB64, Base64.NO_WRAP), 0)
            storedKey == identityKey
        } catch (e: Exception) {
            true
        }
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = PREF_PREFIX_IDENTITY + address.name + ":" + address.deviceId
        val existingB64 = prefs.getString(key, null) ?: return null
        return try {
            IdentityKey(Base64.decode(existingB64, Base64.NO_WRAP), 0)
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // PreKeyStore
    // ==========================================

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val key = PREF_PREFIX_PREKEY + preKeyId
        val b64 = prefs.getString(key, null) ?: throw InvalidKeyIdException("No such prekey: $preKeyId")
        return try {
            PreKeyRecord(Base64.decode(b64, Base64.NO_WRAP))
        } catch (e: IOException) {
            throw InvalidKeyIdException(e)
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        val key = PREF_PREFIX_PREKEY + preKeyId
        val b64 = Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
        prefs.edit().putString(key, b64).apply()
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return prefs.contains(PREF_PREFIX_PREKEY + preKeyId)
    }

    override fun removePreKey(preKeyId: Int) {
        prefs.edit().remove(PREF_PREFIX_PREKEY + preKeyId).apply()
    }

    // ==========================================
    // SignedPreKeyStore
    // ==========================================

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val key = PREF_PREFIX_SIGNED_PREKEY + signedPreKeyId
        val b64 = prefs.getString(key, null) ?: throw InvalidKeyIdException("No such signed prekey: $signedPreKeyId")
        return try {
            SignedPreKeyRecord(Base64.decode(b64, Base64.NO_WRAP))
        } catch (e: IOException) {
            throw InvalidKeyIdException(e)
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        val result = mutableListOf<SignedPreKeyRecord>()
        for ((k, value) in prefs.all) {
            if (k.startsWith(PREF_PREFIX_SIGNED_PREKEY) && value is String) {
                try {
                    result.add(SignedPreKeyRecord(Base64.decode(value, Base64.NO_WRAP)))
                } catch (_: Exception) {}
            }
        }
        return result
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        val key = PREF_PREFIX_SIGNED_PREKEY + signedPreKeyId
        val b64 = Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
        prefs.edit().putString(key, b64).apply()
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return prefs.contains(PREF_PREFIX_SIGNED_PREKEY + signedPreKeyId)
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        prefs.edit().remove(PREF_PREFIX_SIGNED_PREKEY + signedPreKeyId).apply()
    }

    // ==========================================
    // SessionStore
    // ==========================================

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val key = PREF_PREFIX_SESSION + address.name + ":" + address.deviceId
        val b64 = prefs.getString(key, null)
        return if (b64 != null) {
            try {
                SessionRecord(Base64.decode(b64, Base64.NO_WRAP))
            } catch (e: Exception) {
                SessionRecord()
            }
        } else {
            SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        val prefix = PREF_PREFIX_SESSION + name + ":"
        val result = mutableListOf<Int>()
        for (k in prefs.all.keys) {
            if (k.startsWith(prefix)) {
                val deviceIdStr = k.substring(prefix.length)
                deviceIdStr.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val key = PREF_PREFIX_SESSION + address.name + ":" + address.deviceId
        val b64 = Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
        prefs.edit().putString(key, b64).apply()
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val key = PREF_PREFIX_SESSION + address.name + ":" + address.deviceId
        return prefs.contains(key)
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = PREF_PREFIX_SESSION + address.name + ":" + address.deviceId
        prefs.edit().remove(key).apply()
    }

    override fun deleteAllSessions(name: String) {
        val prefix = PREF_PREFIX_SESSION + name + ":"
        val editor = prefs.edit()
        for (k in prefs.all.keys) {
            if (k.startsWith(prefix)) {
                editor.remove(k)
            }
        }
        editor.apply()
    }

    // ==========================================
    // SenderKeyStore (Group messaging)
    // ==========================================

    override fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) {
        val key = PREF_PREFIX_SENDER_KEY + senderKeyName.groupId + ":" + senderKeyName.sender.name + ":" + senderKeyName.sender.deviceId
        val b64 = Base64.encodeToString(record.serialize(), Base64.NO_WRAP)
        prefs.edit().putString(key, b64).apply()
    }

    override fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord {
        val key = PREF_PREFIX_SENDER_KEY + senderKeyName.groupId + ":" + senderKeyName.sender.name + ":" + senderKeyName.sender.deviceId
        val b64 = prefs.getString(key, null)
        return if (b64 != null) {
            try {
                SenderKeyRecord(Base64.decode(b64, Base64.NO_WRAP))
            } catch (e: Exception) {
                SenderKeyRecord()
            }
        } else {
            SenderKeyRecord()
        }
    }

    companion object {
        private const val KEY_LOCAL_IDENTITY_KEY_PAIR = "signal_local_identity_key_pair"
        private const val KEY_LOCAL_REGISTRATION_ID = "signal_local_registration_id"
        private const val PREF_PREFIX_IDENTITY = "signal_identity_"
        private const val PREF_PREFIX_PREKEY = "signal_prekey_"
        private const val PREF_PREFIX_SIGNED_PREKEY = "signal_signed_prekey_"
        private const val PREF_PREFIX_SESSION = "signal_session_"
        private const val PREF_PREFIX_SENDER_KEY = "signal_senderkey_"
    }
}
