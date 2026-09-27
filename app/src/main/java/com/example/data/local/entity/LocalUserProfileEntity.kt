package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class LocalUserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val inviteCode: String,
    val registrationId: Int,
    val identityKeyPairBytes: ByteArray,
    val signedPreKeyId: Int,
    val preKeyId: Int,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalUserProfileEntity

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (inviteCode != other.inviteCode) return false
        if (registrationId != other.registrationId) return false
        if (!identityKeyPairBytes.contentEquals(other.identityKeyPairBytes)) return false
        if (signedPreKeyId != other.signedPreKeyId) return false
        if (preKeyId != other.preKeyId) return false
        if (fcmToken != other.fcmToken) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + inviteCode.hashCode()
        result = 31 * result + registrationId
        result = 31 * result + identityKeyPairBytes.contentHashCode()
        result = 31 * result + signedPreKeyId
        result = 31 * result + preKeyId
        result = 31 * result + (fcmToken?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
