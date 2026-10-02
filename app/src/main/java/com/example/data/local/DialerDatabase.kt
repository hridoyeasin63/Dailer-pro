package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class,
        BlockedNumberEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DialerDatabase : RoomDatabase() {
    abstract fun dialerDao(): DialerDao

    companion object {
        @Volatile
        private var INSTANCE: DialerDatabase? = null

        fun getInstance(context: Context): DialerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DialerDatabase::class.java,
                    "phone_dialer.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun setTestInstance(database: DialerDatabase) {
            INSTANCE = database
        }
    }
}
