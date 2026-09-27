package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SignalIdentityEntity
import com.example.data.local.entity.SignalPreKeyEntity
import com.example.data.local.entity.SignalSessionEntity
import com.example.data.local.entity.SignalSignedPreKeyEntity

@Dao
interface SignalStoreDao {

    // Sessions
    @Query("SELECT * FROM signal_sessions WHERE address = :address LIMIT 1")
    fun getSession(address: String): SignalSessionEntity?

    @Query("SELECT address FROM signal_sessions WHERE address LIKE :namePrefix || ':%'")
    fun getSubDeviceSessionAddresses(namePrefix: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SignalSessionEntity)

    @Query("DELETE FROM signal_sessions WHERE address = :address")
    fun deleteSession(address: String)

    @Query("DELETE FROM signal_sessions WHERE address LIKE :namePrefix || ':%'")
    fun deleteAllSessionsForName(namePrefix: String)

    // PreKeys
    @Query("SELECT * FROM signal_prekeys WHERE preKeyId = :preKeyId LIMIT 1")
    fun getPreKey(preKeyId: Int): SignalPreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPreKey(preKey: SignalPreKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPreKeys(preKeys: List<SignalPreKeyEntity>)

    @Query("DELETE FROM signal_prekeys WHERE preKeyId = :preKeyId")
    fun deletePreKey(preKeyId: Int)

    // Signed PreKeys
    @Query("SELECT * FROM signal_signed_prekeys WHERE signedPreKeyId = :signedPreKeyId LIMIT 1")
    fun getSignedPreKey(signedPreKeyId: Int): SignalSignedPreKeyEntity?

    @Query("SELECT * FROM signal_signed_prekeys")
    fun getAllSignedPreKeys(): List<SignalSignedPreKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSignedPreKey(signedPreKey: SignalSignedPreKeyEntity)

    @Query("DELETE FROM signal_signed_prekeys WHERE signedPreKeyId = :signedPreKeyId")
    fun deleteSignedPreKey(signedPreKeyId: Int)

    // Identities
    @Query("SELECT * FROM signal_identities WHERE address = :address LIMIT 1")
    fun getIdentity(address: String): SignalIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIdentity(identity: SignalIdentityEntity)

    @Query("DELETE FROM signal_identities WHERE address = :address")
    fun deleteIdentity(address: String)
}
