package com.sathish.soundharajan.passwd.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "password_entries")
@Serializable
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val service: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val tags: String = "", // Comma-separated tags
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
