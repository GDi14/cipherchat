package com.example.crypto

import com.example.data.local.AppDatabase
import com.example.data.local.SignalPreKeyEntity
import com.example.data.local.SignalSenderKeyEntity
import com.example.data.local.SignalSessionEntity
import com.example.data.local.SignalSignedPreKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.groups.state.SenderKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.util.concurrent.ConcurrentHashMap

class AppSignalStore(
    private val identityKeyPair: IdentityKeyPair,
    private val registrationId: Int,
    private val database: AppDatabase
) : SignalProtocolStore, SenderKeyStore {

    private val sessionCache = ConcurrentHashMap<String, SessionRecord>()
    private val preKeyCache = ConcurrentHashMap<Int, PreKeyRecord>()
    private val signedPreKeyCache = ConcurrentHashMap<Int, SignedPreKeyRecord>()
    private val senderKeyCache = ConcurrentHashMap<String, SenderKeyRecord>()
    private val trustedKeys = ConcurrentHashMap<SignalProtocolAddress, IdentityKey>()

    // --- IdentityKeyStore ---
    override fun getIdentityKeyPair(): IdentityKeyPair = identityKeyPair

    override fun getLocalRegistrationId(): Int = registrationId

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val existing = trustedKeys[address]
        trustedKeys[address] = identityKey
        return existing == null || existing != identityKey
    }

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: org.whispersystems.libsignal.state.IdentityKeyStore.Direction): Boolean {
        val existing = trustedKeys[address]
        return existing == null || existing == identityKey
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        return trustedKeys[address]
    }

    // --- SessionStore ---
    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val key = "${address.name}:${address.deviceId}"
        sessionCache[key]?.let { return it }

        val entity = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getSession(address.name, address.deviceId)
        }
        val record = if (entity != null) {
            try {
                SessionRecord(entity.recordBytes)
            } catch (e: Exception) {
                SessionRecord()
            }
        } else {
            SessionRecord()
        }
        sessionCache[key] = record
        return record
    }

    override fun getSubDeviceSessions(name: String): List<Int> {
        val entities = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getSubDeviceSessions(name)
        }
        return entities.map { it.deviceId }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        val key = "${address.name}:${address.deviceId}"
        sessionCache[key] = record
        runBlocking(Dispatchers.IO) {
            database.signalStoreDao().storeSession(
                SignalSessionEntity(
                    addressName = address.name,
                    deviceId = address.deviceId,
                    recordBytes = record.serialize()
                )
            )
        }
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val key = "${address.name}:${address.deviceId}"
        if (sessionCache.containsKey(key)) return true
        val entity = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getSession(address.name, address.deviceId)
        }
        return entity != null
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        val key = "${address.name}:${address.deviceId}"
        sessionCache.remove(key)
        runBlocking(Dispatchers.IO) {
            database.signalStoreDao().deleteSession(address.name, address.deviceId)
        }
    }

    override fun deleteAllSessions(name: String) {
        sessionCache.keys.filter { it.startsWith("$name:") }.forEach { sessionCache.remove(it) }
        val sessions = runBlocking(Dispatchers.IO) { database.signalStoreDao().getSubDeviceSessions(name) }
        runBlocking(Dispatchers.IO) {
            sessions.forEach {
                database.signalStoreDao().deleteSession(it.addressName, it.deviceId)
            }
        }
    }

    // --- PreKeyStore ---
    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        preKeyCache[preKeyId]?.let { return it }
        val entity = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getPreKey(preKeyId)
        } ?: throw InvalidKeyIdException("PreKey not found: $preKeyId")
        val record = PreKeyRecord(entity.recordBytes)
        preKeyCache[preKeyId] = record
        return record
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        preKeyCache[preKeyId] = record
        runBlocking(Dispatchers.IO) {
            database.signalStoreDao().storePreKey(
                SignalPreKeyEntity(keyId = preKeyId, recordBytes = record.serialize())
            )
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        if (preKeyCache.containsKey(preKeyId)) return true
        val entity = runBlocking(Dispatchers.IO) { database.signalStoreDao().getPreKey(preKeyId) }
        return entity != null
    }

    override fun removePreKey(preKeyId: Int) {
        preKeyCache.remove(preKeyId)
        runBlocking(Dispatchers.IO) { database.signalStoreDao().deletePreKey(preKeyId) }
    }

    // --- SignedPreKeyStore ---
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        signedPreKeyCache[signedPreKeyId]?.let { return it }
        val entity = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getSignedPreKey(signedPreKeyId)
        } ?: throw InvalidKeyIdException("SignedPreKey not found: $signedPreKeyId")
        val record = SignedPreKeyRecord(entity.recordBytes)
        signedPreKeyCache[signedPreKeyId] = record
        return record
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        val entities = runBlocking(Dispatchers.IO) { database.signalStoreDao().getAllSignedPreKeys() }
        return entities.mapNotNull {
            try {
                SignedPreKeyRecord(it.recordBytes)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        signedPreKeyCache[signedPreKeyId] = record
        runBlocking(Dispatchers.IO) {
            database.signalStoreDao().storeSignedPreKey(
                SignalSignedPreKeyEntity(keyId = signedPreKeyId, recordBytes = record.serialize())
            )
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        if (signedPreKeyCache.containsKey(signedPreKeyId)) return true
        val entity = runBlocking(Dispatchers.IO) { database.signalStoreDao().getSignedPreKey(signedPreKeyId) }
        return entity != null
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        signedPreKeyCache.remove(signedPreKeyId)
    }

    // --- SenderKeyStore (for Group Messaging) ---
    override fun storeSenderKey(senderKeyName: SenderKeyName, record: SenderKeyRecord) {
        val key = "${senderKeyName.groupId}:${senderKeyName.sender.name}:${senderKeyName.sender.deviceId}"
        senderKeyCache[key] = record
        runBlocking(Dispatchers.IO) {
            database.signalStoreDao().storeSenderKey(
                SignalSenderKeyEntity(
                    groupId = senderKeyName.groupId,
                    senderAddress = senderKeyName.sender.name,
                    deviceId = senderKeyName.sender.deviceId,
                    recordBytes = record.serialize()
                )
            )
        }
    }

    override fun loadSenderKey(senderKeyName: SenderKeyName): SenderKeyRecord {
        val key = "${senderKeyName.groupId}:${senderKeyName.sender.name}:${senderKeyName.sender.deviceId}"
        senderKeyCache[key]?.let { return it }
        val entity = runBlocking(Dispatchers.IO) {
            database.signalStoreDao().getSenderKey(
                groupId = senderKeyName.groupId,
                senderAddress = senderKeyName.sender.name,
                deviceId = senderKeyName.sender.deviceId
            )
        }
        val record = if (entity != null) {
            try {
                SenderKeyRecord(entity.recordBytes)
            } catch (e: Exception) {
                SenderKeyRecord()
            }
        } else {
            SenderKeyRecord()
        }
        senderKeyCache[key] = record
        return record
    }
}
