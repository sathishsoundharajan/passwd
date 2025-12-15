package com.sathish.soundharajan.passwd.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY title ASC")
    fun getAllVaultEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getVaultEntriesPaged(limit: Int, offset: Int): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 AND type = :type ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getVaultEntriesByTypePaged(type: VaultEntryType, limit: Int, offset: Int): List<VaultEntry>

    @Query("SELECT COUNT(*) FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0")
    fun getActiveVaultEntryCount(): Flow<Int>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY title ASC")
    fun getArchivedVaultEntries(): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getArchivedVaultEntriesPaged(limit: Int, offset: Int): List<VaultEntry>

    @Query("SELECT COUNT(*) FROM vault_entries WHERE isDeleted = 0 AND isArchived = 1")
    fun getArchivedVaultEntryCount(): Flow<Int>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 AND type = :type ORDER BY title ASC")
    fun getVaultEntriesByType(type: VaultEntryType): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchVaultEntries(query: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun searchVaultEntriesPaged(query: String, limit: Int, offset: Int): List<VaultEntry>

    @Query("SELECT COUNT(*) FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')")
    fun getSearchResultCount(query: String): Flow<Int>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecentlyDeletedVaultEntries(): Flow<List<VaultEntry>>

    @Query("SELECT COUNT(*) FROM vault_entries WHERE isDeleted = 1")
    fun getRecentlyDeletedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultEntry(vaultEntry: VaultEntry): Long

    @Update
    suspend fun updateVaultEntry(vaultEntry: VaultEntry)

    @Delete
    suspend fun deleteVaultEntry(vaultEntry: VaultEntry)

    @Query("UPDATE vault_entries SET isArchived = :archived WHERE id = :id")
    suspend fun archiveVaultEntry(id: Long, archived: Boolean)

    @Query("UPDATE vault_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteVaultEntry(id: Long, deletedAt: Long)

    @Query("UPDATE vault_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreVaultEntry(id: Long)

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun permanentlyDeleteVaultEntry(id: Long)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAllVaultEntries()

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY title ASC")
    suspend fun getAllVaultEntriesOnce(): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY title ASC")
    suspend fun getAllArchivedVaultEntriesOnce(): List<VaultEntry>

    @Query("SELECT * FROM vault_entries WHERE isDeleted = 0 ORDER BY title ASC")
    suspend fun getAllVaultEntriesForReEncryption(): List<VaultEntry>

    // Migration helper queries
    @Query("SELECT * FROM password_entries ORDER BY service ASC")
    suspend fun getAllPasswordEntriesForMigration(): List<PasswordEntry>

    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun getPasswordEntryCount(): Int
}
