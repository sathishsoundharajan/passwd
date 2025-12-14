package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.util.Base64
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultManager: VaultManager,
    private val cryptoManager: CryptoManager,
    private val errorRecovery: ErrorRecovery
) {
    private val passwordDao: PasswordDao?
        get() = vaultManager.getDatabase()?.passwordDao()

    fun getAllPasswords(): Flow<List<PasswordEntry>>? {
        return passwordDao?.getAllPasswords()?.map { list ->
            list.map { it.copy(password = decryptPassword(it.password)) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun getPasswordsPaged(limit: Int, offset: Int): List<PasswordEntry>? {
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            passwordDao?.getPasswordsPaged(limit, offset)?.map { it.copy(password = decryptPassword(it.password)) }
        }
    }

    fun getActivePasswordCount(): Flow<Int>? {
        return passwordDao?.getActivePasswordCount()
    }

    fun getArchivedPasswords(): Flow<List<PasswordEntry>>? {
        return passwordDao?.getArchivedPasswords()?.map { list ->
            list.map { it.copy(password = decryptPassword(it.password)) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun getArchivedPasswordsPaged(limit: Int, offset: Int): List<PasswordEntry>? {
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
             passwordDao?.getArchivedPasswordsPaged(limit, offset)?.map { it.copy(password = decryptPassword(it.password)) }
        }
    }

    fun getArchivedPasswordCount(): Flow<Int>? {
        return passwordDao?.getArchivedPasswordCount()
    }

    fun searchPasswords(query: String): Flow<List<PasswordEntry>>? {
        return passwordDao?.searchPasswords(query)?.map { list ->
            list.map { it.copy(password = decryptPassword(it.password)) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun searchPasswordsPaged(query: String, limit: Int, offset: Int): List<PasswordEntry>? {
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            passwordDao?.searchPasswordsPaged(query, limit, offset)?.map { it.copy(password = decryptPassword(it.password)) }
        }
    }

    fun getSearchResultCount(query: String): Flow<Int>? {
        return passwordDao?.getSearchResultCount(query)
    }

    fun getRecentlyDeletedPasswords(): Flow<List<PasswordEntry>>? {
        return passwordDao?.getRecentlyDeletedPasswords()?.map { list ->
            list.map { it.copy(password = decryptPassword(it.password)) }
        }?.flowOn(Dispatchers.Default)
    }

    fun getRecentlyDeletedCount(): Flow<Int>? {
        return passwordDao?.getRecentlyDeletedCount()
    }

    suspend fun insertPassword(password: PasswordEntry): Result<Long> {
        return errorRecovery.executeWithRecovery("insert_password") {
            val encrypted = password.copy(password = encryptPassword(password.password))
            passwordDao?.insertPassword(encrypted) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun updatePassword(password: PasswordEntry): Result<Unit> {
        return errorRecovery.executeWithRecovery("update_password") {
            val encrypted = password.copy(password = encryptPassword(password.password))
            passwordDao?.updatePassword(encrypted) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun deletePassword(password: PasswordEntry): Result<Unit> {
        return errorRecovery.executeWithRecovery("delete_password") {
            passwordDao?.deletePassword(password) ?: throw IllegalStateException("Database not initialized")
            Unit
        }
    }



    suspend fun archivePassword(id: Long, archived: Boolean): Result<Unit> {
        return errorRecovery.executeWithRecovery("archive_password") {
            passwordDao?.archivePassword(id, archived) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun softDeletePassword(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("soft_delete_password") {
            passwordDao?.softDeletePassword(id, System.currentTimeMillis()) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun restorePassword(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("restore_password") {
            passwordDao?.restorePassword(id) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun permanentlyDeletePassword(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("permanently_delete_password") {
            passwordDao?.permanentlyDeletePassword(id) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun deleteAllPasswords(): Result<Unit> {
        return errorRecovery.executeWithRecovery("delete_all_passwords") {
            passwordDao?.deleteAllPasswords()
        }
    }

    suspend fun getAllPasswordsOnce(): List<PasswordEntry>? {
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            passwordDao?.getAllPasswordsOnce()?.map { it.copy(password = decryptPassword(it.password)) }
        }
    }

    suspend fun getAllArchivedPasswordsOnce(): List<PasswordEntry>? {
        return kotlinx.coroutines.withContext(Dispatchers.Default) {
            passwordDao?.getAllArchivedPasswordsOnce()?.map { it.copy(password = decryptPassword(it.password)) }
        }
    }

    private fun encryptPassword(plain: String): String {
        val (encrypted, iv) = cryptoManager.encrypt(plain.toByteArray())
        val ivStr = Base64.encodeToString(iv, Base64.DEFAULT)
        val encStr = Base64.encodeToString(encrypted, Base64.DEFAULT)
        return "$ivStr:$encStr"
    }

    private fun decryptPassword(encrypted: String): String {
        try {
            val parts = encrypted.split(":")
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.DEFAULT)
            val enc = Base64.decode(parts[1], Base64.DEFAULT)
            val plain = cryptoManager.decrypt(enc, iv)
            return String(plain)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    suspend fun changeMasterPassword(
        oldPassword: String,
        newPassword: String,
        onProgress: ((current: Int, total: Int, step: String) -> Unit)? = null
    ): Result<Unit> {
        return errorRecovery.executeWithRecovery("change_master_password") {
            // Get all passwords that need to be re-encrypted (stored encrypted in DB)
            val allPasswords = passwordDao?.getAllPasswordsForReEncryption() ?: emptyList()
            val totalPasswords = allPasswords.size

            onProgress?.invoke(0, totalPasswords, "Preparing to re-encrypt passwords...")

            // Process passwords in batches to avoid blocking UI for too long
            val batchSize = 10
            val decryptedPasswords = mutableListOf<PasswordEntry>()

            // Phase 1: Decrypt all passwords in batches
            for (i in allPasswords.indices step batchSize) {
                val batch = allPasswords.subList(i, minOf(i + batchSize, allPasswords.size))
                val decryptedBatch = batch.map { password ->
                    val decryptedPassword = decryptPassword(password.password)
                    password.copy(password = decryptedPassword)
                }
                decryptedPasswords.addAll(decryptedBatch)

                onProgress?.invoke(
                    minOf(i + batchSize, totalPasswords),
                    totalPasswords,
                    "Decrypting passwords... (${minOf(i + batchSize, totalPasswords)}/$totalPasswords)"
                )
            }

            // Phase 2: Change the master password (this updates the CryptoManager key)
            onProgress?.invoke(totalPasswords, totalPasswords, "Updating master password...")
            vaultManager.changeMasterPassword(oldPassword, newPassword)

            // Phase 3: Re-encrypt all passwords with the new key in batches
            for (i in decryptedPasswords.indices step batchSize) {
                val batch = decryptedPasswords.subList(i, minOf(i + batchSize, decryptedPasswords.size))
                val reEncryptedBatch = batch.map { password ->
                    password.copy(password = encryptPassword(password.password))
                }

                // Update each password in the batch
                for (password in reEncryptedBatch) {
                    passwordDao?.updatePassword(password)
                }

                onProgress?.invoke(
                    totalPasswords + minOf(i + batchSize, decryptedPasswords.size),
                    totalPasswords * 2,
                    "Re-encrypting passwords... (${minOf(i + batchSize, decryptedPasswords.size)}/$totalPasswords)"
                )
            }

            onProgress?.invoke(totalPasswords * 2, totalPasswords * 2, "Master password changed successfully!")
        }
    }
}
