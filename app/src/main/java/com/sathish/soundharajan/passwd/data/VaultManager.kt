package com.sathish.soundharajan.passwd.data

import android.content.Context
import com.sathish.soundharajan.passwd.security.AuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) {
    private var database: AppDatabase? = null

    // Removed manual initialize(context) as we inject context

    fun openVault(password: String) {
        if (database == null) {
            val key = authManager.getDatabaseKey(password)
            database = AppDatabase.getInstance(context, key)
        }
    }

    fun closeVault() {
        database = null
        AppDatabase.clearInstance()
    }

    fun getDatabase(): AppDatabase? = database

    fun clearDatabase() {
        database = null
        AppDatabase.clearInstance()
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String) {
        // Validate old password
        if (!authManager.verifyMasterPassword(oldPassword)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Update the master password in AuthManager first
        authManager.setMasterPassword(newPassword)

        // Close current database
        closeVault()

        // Reopen with new password
        openVault(newPassword)
    }

    /**
     * Safely change master password by recreating the database
     * This method should be called while the database is still open with the old password
     */
    fun recreateDatabaseWithNewPassword(newPassword: String) {
        // Close current database
        closeVault()

        // Delete the old database file to prevent key mismatch
        try {
            val dbFile = context.getDatabasePath("passwords.db")
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                if (!deleted) {
                    throw IllegalStateException("Failed to delete old database file")
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to clean up old database: ${e.message}")
        }

        // Clear the singleton instance to force recreation
        AppDatabase.clearInstance()

        // Open new database with new password
        openVault(newPassword)
    }
}
