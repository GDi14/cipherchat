package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        FriendRequestEntity::class,
        ProfileEntity::class,
        SignalSessionEntity::class,
        SignalPreKeyEntity::class,
        SignalSignedPreKeyEntity::class,
        SignalSenderKeyEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun friendRequestDao(): FriendRequestDao
    abstract fun profileDao(): ProfileDao
    abstract fun signalStoreDao(): SignalStoreDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN textEmphasisJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS hidden_messages (id TEXT NOT NULL PRIMARY KEY, hiddenAt INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE friend_requests ADD COLUMN avatarPath TEXT")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN blurHash TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'synced'")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN is_hidden INTEGER NOT NULL DEFAULT 0")
                runCatching {
                    db.execSQL("UPDATE messages SET is_hidden = 1 WHERE id IN (SELECT id FROM hidden_messages)")
                    db.execSQL("DROP TABLE IF EXISTS hidden_messages")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conv_hidden_time ON messages (conversationId, is_hidden, timestamp)")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN avatarPath TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shieldchat.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            runCatching { db.query("PRAGMA synchronous = NORMAL").close() }
                            runCatching { db.query("PRAGMA temp_store = MEMORY").close() }
                            runCatching { db.query("PRAGMA mmap_size = 30000000").close() }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return getInstance(context)
        }
    }
}
