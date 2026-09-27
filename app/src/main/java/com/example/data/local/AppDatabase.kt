package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.SignalStoreDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.LocalContactEntity
import com.example.data.local.entity.LocalMessageEntity
import com.example.data.local.entity.LocalUserProfileEntity
import com.example.data.local.entity.SignalIdentityEntity
import com.example.data.local.entity.SignalPreKeyEntity
import com.example.data.local.entity.SignalSessionEntity
import com.example.data.local.entity.SignalSignedPreKeyEntity

@Database(
    entities = [
        LocalUserProfileEntity::class,
        LocalMessageEntity::class,
        LocalContactEntity::class,
        SignalSessionEntity::class,
        SignalPreKeyEntity::class,
        SignalSignedPreKeyEntity::class,
        SignalIdentityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun signalStoreDao(): SignalStoreDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cipher_chat.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
