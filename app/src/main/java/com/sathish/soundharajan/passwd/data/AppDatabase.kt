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
    entities = [PasswordEntry::class, VaultEntry::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun vaultDao(): VaultDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new vault_entries table
                database.execSQL("""
                    CREATE TABLE vault_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        sensitiveData TEXT NOT NULL,
                        documentPaths TEXT NOT NULL,
                        isArchived INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Migrate existing password entries to vault entries
                database.execSQL("""
                    INSERT INTO vault_entries (
                        id, type, title, category, tags, notes, sensitiveData,
                        documentPaths, isArchived, isDeleted, deletedAt,
                        createdAt, updatedAt
                    )
                    SELECT
                        id,
                        'PASSWORD' as type,
                        service as title,
                        '' as category,
                        tags,
                        notes,
                        sensitiveData,
                        '' as documentPaths,
                        isArchived,
                        0 as isDeleted,
                        NULL as deletedAt,
                        createdAt,
                        updatedAt
                    FROM password_entries
                """.trimIndent())

                // Create sensitiveData column by encrypting password data
                // Note: This migration assumes the app will handle the actual encryption
                // during the first app launch after migration
            }
        }

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "passwords.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
