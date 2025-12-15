package com.sathish.soundharajan.passwd.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

enum class VaultEntryType {
    PASSWORD,
    BANK_ACCOUNT,
    CREDIT_CARD,
    IDENTITY_CARD
}

@Entity(tableName = "vault_entries")
@Serializable
data class VaultEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Entry metadata
    val type: VaultEntryType,
    val title: String,              // Display name/title
    val category: String = "",      // User-defined category
    val tags: String = "",          // Comma-separated tags
    val notes: String = "",

    // Encrypted sensitive data (JSON format for type-specific data)
    val sensitiveData: String,

    // Document attachments (for identity cards) - encrypted file paths
    val documentPaths: String = "", // Comma-separated encrypted file paths

    // Metadata
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
