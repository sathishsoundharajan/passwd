package com.sathish.soundharajan.passwd.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY service ASC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY service ASC LIMIT :limit OFFSET :offset")
    suspend fun getPasswordsPaged(limit: Int, offset: Int): List<PasswordEntry>

    @Query("SELECT COUNT(*) FROM password_entries WHERE isDeleted = 0 AND isArchived = 0")
    fun getActivePasswordCount(): Flow<Int>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY service ASC")
    fun getArchivedPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY service ASC LIMIT :limit OFFSET :offset")
    suspend fun getArchivedPasswordsPaged(limit: Int, offset: Int): List<PasswordEntry>

    @Query("SELECT COUNT(*) FROM password_entries WHERE isDeleted = 0 AND isArchived = 1")
    fun getArchivedPasswordCount(): Flow<Int>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 AND (service LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY service ASC")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 AND (service LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY service ASC LIMIT :limit OFFSET :offset")
    suspend fun searchPasswordsPaged(query: String, limit: Int, offset: Int): List<PasswordEntry>

    @Query("SELECT COUNT(*) FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 AND (service LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')")
    fun getSearchResultCount(query: String): Flow<Int>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecentlyDeletedPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT COUNT(*) FROM password_entries WHERE isDeleted = 1")
    fun getRecentlyDeletedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntry): Long

    @Update
    suspend fun updatePassword(password: PasswordEntry)

    @Delete
    suspend fun deletePassword(password: PasswordEntry)

    @Query("UPDATE password_entries SET isArchived = :archived WHERE id = :id")
    suspend fun archivePassword(id: Long, archived: Boolean)

    @Query("UPDATE password_entries SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeletePassword(id: Long, deletedAt: Long)

    @Query("UPDATE password_entries SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restorePassword(id: Long)

    @Query("DELETE FROM password_entries WHERE id = :id")
    suspend fun permanentlyDeletePassword(id: Long)

    @Query("DELETE FROM password_entries")
    suspend fun deleteAllPasswords()

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY service ASC")
    suspend fun getAllPasswordsOnce(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE isDeleted = 0 AND isArchived = 1 ORDER BY service ASC")
    suspend fun getAllArchivedPasswordsOnce(): List<PasswordEntry>

    @Query("SELECT * FROM password_entries ORDER BY service ASC")
    suspend fun getAllPasswordsForReEncryption(): List<PasswordEntry>
}
