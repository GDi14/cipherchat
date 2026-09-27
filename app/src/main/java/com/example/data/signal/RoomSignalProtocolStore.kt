package com.example.data.signal

import com.example.data.local.dao.SignalStoreDao
import com.example.data.local.entity.SignalIdentityEntity
import com.example.data.local.entity.SignalPreKeyEntity
import com.example.data.local.entity.SignalSessionEntity
import com.example.data.local.entity.SignalSignedPreKeyEntity
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore
import java.io.IOException

class RoomSignalProtocolStore(
    private val signalStoreDao: SignalStoreDao,
    private val identityKeyPair: IdentityKeyPair,
    private val localRegistrationId: Int
) : SignalProtocolStore {

    // ==========================================
    // IdentityKeyStore
    // ==========================================
    override fun getIdentityKeyPair(): IdentityKeyPair {
        return identityKeyPair
    }

    override fun getLocalRegistrationId(): Int {
        return localRegistrationId
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = address.name + ":" + address.deviceId
        val existing = signalStoreDao.getIdentity(key)
        signalStoreDao.insertIdentity(SignalIdentityEntity(key, identityKey.serialize()))
        return existing == null || !existing.identityKeyBytes.contentEquals(identityKey.serialize())
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        // In this trusted circle of friends app, auto-trust verified identities
        return true
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = address.name + ":" + address.deviceId
        val entity = signalStoreDao.getIdentity(key) ?: return null
        return try {
            IdentityKey(entity.identityKeyBytes, 0)
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // PreKeyStore
    // ==========================================
    @Throws(InvalidKeyIdException::class)
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val entity = signalStoreDao.getPreKey(preKeyId)
            ?: throw InvalidKeyIdException("No prekey for ID: $preKeyId")
        return try {
            PreKeyRecord(entity.recordBytes)
        } catch (e: IOException) {
            throw InvalidKeyIdException(e)
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        signalStoreDao.insertPreKey(SignalPreKeyEntity(preKeyId, record.serialize()))
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return signalStoreDao.getPreKey(preKeyId) != null
    }

    override fun removePreKey(preKeyId: Int) {
        signalStoreDao.deletePreKey(preKeyId)
    }

    // ==========================================
    // SignedPreKeyStore
    // ==========================================
    @Throws(InvalidKeyIdException::class)
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val entity = signalStoreDao.getSignedPreKey(signedPreKeyId)
            ?: throw InvalidKeyIdException("No signed prekey for ID: $signedPreKeyId")
        return try {
            SignedPreKeyRecord(entity.recordBytes)
        } catch (e: IOException) {
            throw InvalidKeyIdException(e)
        }
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return signalStoreDao.getAllSignedPreKeys().mapNotNull {
            try {
                SignedPreKeyRecord(it.recordBytes)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        signalStoreDao.insertSignedPreKey(SignalSignedPreKeyEntity(signedPreKeyId, record.serialize()))
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return signalStoreDao.getSignedPreKey(signedPreKeyId) != null
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        signalStoreDao.deleteSignedPreKey(signedPreKeyId)
    }

    // ==========================================
    // SessionStore
    // ==========================================
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val key = address.name + ":" + address.deviceId
        val entity = signalStoreDao.getSession(key)
        return if (entity != null) {
            try {
                SessionRecord(entity.recordBytes)
            } catch (e: IOException) {
                SessionRecord()
            }
        } else {
            SessionRecord()
        }
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        val addresses = signalStoreDao.getSubDeviceSessionAddresses(name)
        val devices = mutableListOf<Int>()
        for (addr in addresses) {
            val parts = addr.split(":")
            if (parts.size >= 2) {
                parts[1].toIntOrNull()?.let { deviceId ->
                    if (deviceId != 1) devices.add(deviceId)
                }
            }
        }
        return devices
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val key = address.name + ":" + address.deviceId
        signalStoreDao.insertSession(SignalSessionEntity(key, record.serialize()))
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val key = address.name + ":" + address.deviceId
        val entity = signalStoreDao.getSession(key) ?: return false
        val session = loadSession(address)
        return !session.isFresh
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = address.name + ":" + address.deviceId
        signalStoreDao.deleteSession(key)
    }

    override fun deleteAllSessions(name: String) {
        signalStoreDao.deleteAllSessionsForName(name)
    }
}
