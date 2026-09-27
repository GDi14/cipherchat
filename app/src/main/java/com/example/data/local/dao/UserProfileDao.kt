package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LocalUserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): LocalUserProfileEntity?

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfileFlow(): Flow<LocalUserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: LocalUserProfileEntity)

    @Query("UPDATE user_profile SET fcmToken = :token WHERE userId = :userId")
    suspend fun updateFcmToken(userId: String, token: String)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}
