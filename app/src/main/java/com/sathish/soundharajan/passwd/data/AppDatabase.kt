package com.sathish.soundharajan.passwd.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [PasswordEntry::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to existing table
                database.execSQL("ALTER TABLE password_entries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE password_entries ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "passwords.db"
                )
                .addMigrations(MIGRATION_1_2)
                .openHelperFactory(SupportFactory(passphrase))
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
