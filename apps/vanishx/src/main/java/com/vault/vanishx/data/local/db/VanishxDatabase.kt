package com.vault.vanishx.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MetaEntity::class,
        MailboxRoomEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class VanishxDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao
    abstract fun mailboxRoomDao(): MailboxRoomDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val NAME = "vanishx.db"
    }
}
