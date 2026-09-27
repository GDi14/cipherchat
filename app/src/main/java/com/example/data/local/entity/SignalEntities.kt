package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signal_sessions")
data class SignalSessionEntity(
    @PrimaryKey val address: String, // e.g. "userId:1"
    val recordBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalSessionEntity
        return address == other.address && recordBytes.contentEquals(other.recordBytes)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + recordBytes.contentHashCode()
        return result
    }
}

@Entity(tableName = "signal_prekeys")
data class SignalPreKeyEntity(
    @PrimaryKey val preKeyId: Int,
    val recordBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalPreKeyEntity
        return preKeyId == other.preKeyId && recordBytes.contentEquals(other.recordBytes)
    }

    override fun hashCode(): Int {
        var result = preKeyId
        result = 31 * result + recordBytes.contentHashCode()
        return result
    }
}

@Entity(tableName = "signal_signed_prekeys")
data class SignalSignedPreKeyEntity(
    @PrimaryKey val signedPreKeyId: Int,
    val recordBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalSignedPreKeyEntity
        return signedPreKeyId == other.signedPreKeyId && recordBytes.contentEquals(other.recordBytes)
    }

    override fun hashCode(): Int {
        var result = signedPreKeyId
        result = 31 * result + recordBytes.contentHashCode()
        return result
    }
}

@Entity(tableName = "signal_identities")
data class SignalIdentityEntity(
    @PrimaryKey val address: String,
    val identityKeyBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignalIdentityEntity
        return address == other.address && identityKeyBytes.contentEquals(other.identityKeyBytes)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + identityKeyBytes.contentHashCode()
        return result
    }
}
