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

        // Update the master password in AuthManager
        authManager.setMasterPassword(newPassword)

        // Close current database
        closeVault()

        // Reopen with new password
        openVault(newPassword)
    }
}
