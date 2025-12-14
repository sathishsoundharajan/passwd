package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

@Singleton
class ExportImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val passwordRepository: PasswordRepository
) {

    companion object {
        private const val EXPORT_VERSION = "1.0"
        private const val EXPORT_FORMAT_JSON = "json"
        private const val EXPORT_FORMAT_CSV = "csv"
    }

    @Serializable
    data class ExportData(
        val version: String,
        val timestamp: Long,
        val passwordCount: Int,
        val passwords: List<ExportedPassword>
    )

    @Serializable
    data class ExportedPassword(
        val id: Long,
        val service: String,
        val username: String,
        val password: String,
        val notes: String,
        val tags: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isArchived: Boolean
    )

    suspend fun exportPasswords(
        passwords: List<PasswordEntry>,
        format: String = EXPORT_FORMAT_JSON,
        masterPassword: String,
        destinationUri: Uri
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val exportData = createExportData(passwords)
            val serializedData = serializeData(exportData, format)
            val encryptedData = encryptExportData(serializedData, masterPassword)

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(encryptedData)
            } ?: throw Exception("Failed to open output stream")

            Result.success(ExportResult(
                passwordCount = passwords.size,
                fileSize = encryptedData.size,
                format = format,
                timestamp = System.currentTimeMillis()
            ))

        } catch (e: Exception) {
            Result.failure(Exception("Export failed: ${e.localizedMessage}", e))
        }
    }

    suspend fun importPasswords(
        sourceUri: Uri,
        masterPassword: String,
        conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val encryptedData = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("Failed to read file")

            val decryptedData = decryptImportData(encryptedData, masterPassword)
            val exportData = deserializeData(decryptedData)
            val importedPasswords = processImportData(exportData, conflictStrategy)

            Result.success(ImportResult(
                totalPasswords = exportData.passwordCount,
                importedPasswords = importedPasswords.size,
                skippedPasswords = exportData.passwordCount - importedPasswords.size,
                conflictsResolved = importedPasswords.count { it.isUpdate }
            ))

        } catch (e: Exception) {
            Result.failure(Exception("Import failed: ${e.localizedMessage}", e))
        }
    }

    private fun createExportData(passwords: List<PasswordEntry>): ExportData {
        val exportedPasswords = passwords.map { password ->
            ExportedPassword(
                id = password.id,
                service = password.service,
                username = password.username,
                password = password.password,
                notes = password.notes,
                tags = password.tags,
                createdAt = password.createdAt,
                updatedAt = password.updatedAt,
                isArchived = password.isArchived
            )
        }

        return ExportData(
            version = EXPORT_VERSION,
            timestamp = System.currentTimeMillis(),
            passwordCount = passwords.size,
            passwords = exportedPasswords
        )
    }

    private fun serializeData(exportData: ExportData, format: String): String {
        return when (format.lowercase()) {
            EXPORT_FORMAT_JSON -> Json.encodeToString(exportData)
            EXPORT_FORMAT_CSV -> createCSVData(exportData)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun createCSVData(exportData: ExportData): String {
        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("Service,Username,Password,Notes,Tags,CreatedAt,UpdatedAt,IsArchived")
        exportData.passwords.forEach { password ->
            val escapedService = escapeCSV(password.service)
            val escapedUsername = escapeCSV(password.username)
            val escapedPassword = escapeCSV(password.password)
            val escapedNotes = escapeCSV(password.notes)
            val escapedTags = escapeCSV(password.tags)
            csvBuilder.appendLine("$escapedService,$escapedUsername,$escapedPassword,$escapedNotes,$escapedTags,${password.createdAt},${password.updatedAt},${password.isArchived}")
        }
        return csvBuilder.toString()
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun encryptExportData(data: String, masterPassword: String): ByteArray {
        // Use standard AES-GCM with derived key for portability
        val salt = cryptoManager.generateSalt() 
        val keyBytes = cryptoManager.deriveDatabaseKey(masterPassword.toCharArray(), salt)
        val key = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        val envelope = JSONObject().apply {
            put("version", EXPORT_VERSION)
            put("iv", Base64.encodeToString(iv, Base64.DEFAULT))
            put("salt", Base64.encodeToString(salt, Base64.DEFAULT))
            put("data", Base64.encodeToString(encrypted, Base64.DEFAULT))
            put("timestamp", System.currentTimeMillis())
        }

        return envelope.toString().toByteArray(Charsets.UTF_8)
    }

    private fun decryptImportData(encryptedData: ByteArray, masterPassword: String): String {
        val envelopeJson = String(encryptedData, Charsets.UTF_8)
        val envelope = JSONObject(envelopeJson)

        val iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT)
        val encrypted = Base64.decode(envelope.getString("data"), Base64.DEFAULT)
        // Handle migration: older exports might not have salt/used hardcoded salt or keystore.
        // But since I'm fixing the bug now, let's assume new format or standard.
        // Ideally we check version.
        val saltStr = if (envelope.has("salt")) envelope.getString("salt") else null
        
        if (saltStr == null) {
            // Legacy fallback or error. Given previous code was broken (using device keystore), 
            // old exports explicitly FAILED to traverse devices. 
            // Here we try to recover if it was the same device.
            // But I will stick to the new portable format.
            throw Exception("Invalid export format: missing salt")
        }
        
        val salt = Base64.decode(saltStr, Base64.DEFAULT)
        val keyBytes = cryptoManager.deriveDatabaseKey(masterPassword.toCharArray(), salt)
        val key = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deserializeData(data: String): ExportData {
        return try {
            Json.decodeFromString<ExportData>(data)
        } catch (e: Exception) {
            try {
                parseCSVData(data)
            } catch (csvException: Exception) {
                throw Exception("Unsupported or corrupted file format")
            }
        }
    }

    private fun parseCSVData(csvData: String): ExportData {
         val lines = csvData.lines().filter { it.isNotBlank() }
        if (lines.size < 2) throw Exception("Invalid CSV format")

        val passwords = lines.drop(1).map { line ->
            val columns = parseCSVLine(line)
            if (columns.size < 8) throw Exception("Invalid CSV row format")

            ExportedPassword(
                id = columns[0].toLongOrNull() ?: 0L,
                service = unescapeCSV(columns[1]),
                username = unescapeCSV(columns[2]),
                password = unescapeCSV(columns[3]),
                notes = unescapeCSV(columns[4]),
                tags = unescapeCSV(columns[5]),
                createdAt = columns[6].toLongOrNull() ?: System.currentTimeMillis(),
                updatedAt = columns[7].toLongOrNull() ?: System.currentTimeMillis(),
                isArchived = columns[8].toBoolean()
            )
        }

        return ExportData(
            version = EXPORT_VERSION,
            timestamp = System.currentTimeMillis(),
            passwordCount = passwords.size,
            passwords = passwords
        )
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && !inQuotes -> inQuotes = true
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' && inQuotes -> inQuotes = false
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun unescapeCSV(value: String): String {
        return if (value.startsWith("\"") && value.endsWith("\"")) {
            value.substring(1, value.length - 1).replace("\"\"", "\"")
        } else {
            value
        }
    }

    private suspend fun processImportData(
        exportData: ExportData,
        conflictStrategy: ConflictStrategy
    ): List<ImportEntry> {
        val importedEntries = mutableListOf<ImportEntry>()

        // Get all existing passwords for duplicate checking
        val existingPasswords = passwordRepository.getAllPasswordsOnce().orEmpty() +
                               passwordRepository.getAllArchivedPasswordsOnce().orEmpty()

        for (exportedPassword in exportData.passwords) {
            val passwordEntry = PasswordEntry(
                service = exportedPassword.service,
                username = exportedPassword.username,
                password = exportedPassword.password,
                notes = exportedPassword.notes,
                tags = exportedPassword.tags,
                createdAt = exportedPassword.createdAt,
                updatedAt = exportedPassword.updatedAt,
                isArchived = exportedPassword.isArchived
            )

            // Check for existing password by service + username combination
            val existingPassword = existingPasswords.find {
                it.service.equals(passwordEntry.service, ignoreCase = true) &&
                it.username.equals(passwordEntry.username, ignoreCase = true)
            }

            when {
                existingPassword == null -> {
                    // No duplicate found, insert as new
                    passwordRepository.insertPassword(passwordEntry).fold(
                        onSuccess = { importedEntries.add(ImportEntry(passwordEntry, false)) },
                        onFailure = { /* Skip on failure */ }
                    )
                }
                conflictStrategy == ConflictStrategy.OVERWRITE -> {
                    // Update existing password
                    val updatedEntry = existingPassword.copy(
                        password = passwordEntry.password,
                        notes = passwordEntry.notes,
                        tags = passwordEntry.tags,
                        updatedAt = System.currentTimeMillis(),
                        isArchived = passwordEntry.isArchived
                    )
                    passwordRepository.updatePassword(updatedEntry).fold(
                        onSuccess = { importedEntries.add(ImportEntry(updatedEntry, true)) },
                        onFailure = { /* Skip on failure */ }
                    )
                }
                conflictStrategy == ConflictStrategy.SKIP -> {
                    // Skip duplicate
                    continue
                }
            }
        }
        return importedEntries
    }

    fun generateExportFilename(format: String = EXPORT_FORMAT_JSON): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "passwd_export_$timestamp.$format"
    }

    suspend fun validateExportFile(uri: Uri, masterPassword: String): Result<ExportValidation> {
        return try {
            val encryptedData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("Cannot read file")

            val decryptedData = decryptImportData(encryptedData, masterPassword)
            val exportData = deserializeData(decryptedData)

            Result.success(ExportValidation(
                isValid = true,
                version = exportData.version,
                passwordCount = exportData.passwordCount,
                timestamp = exportData.timestamp
            ))
        } catch (e: Exception) {
            Result.success(ExportValidation(isValid = false, errorMessage = e.localizedMessage ?: "Unknown error"))
        }
    }

    enum class ConflictStrategy { SKIP, OVERWRITE }

    data class ExportResult(val passwordCount: Int, val fileSize: Int, val format: String, val timestamp: Long)
    data class ImportResult(val totalPasswords: Int, val importedPasswords: Int, val skippedPasswords: Int, val conflictsResolved: Int)
    data class ImportEntry(val password: PasswordEntry, val isUpdate: Boolean)
    data class ExportValidation(val isValid: Boolean, val version: String? = null, val passwordCount: Int? = null, val timestamp: Long? = null, val errorMessage: String? = null)
}
