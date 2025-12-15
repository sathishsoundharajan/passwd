package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.net.Uri
import com.sathish.soundharajan.passwd.data.models.*
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultManager: VaultManager,
    private val cryptoManager: CryptoManager,
    private val documentManager: DocumentManager,
    private val errorRecovery: ErrorRecovery
) {
    private val vaultDao: VaultDao?
        get() = vaultManager.getDatabase()?.vaultDao()

    fun getAllVaultEntries(): Flow<List<VaultEntry>>? {
        return vaultDao?.getAllVaultEntries()?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }?.flowOn(Dispatchers.Default)
    }

    fun getVaultEntriesByType(type: VaultEntryType): Flow<List<VaultEntry>>? {
        return vaultDao?.getVaultEntriesByType(type)?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun getVaultEntriesPaged(limit: Int, offset: Int): List<VaultEntry>? {
        return withContext(Dispatchers.Default) {
            vaultDao?.getVaultEntriesPaged(limit, offset)?.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }
    }

    suspend fun getVaultEntriesByTypePaged(type: VaultEntryType, limit: Int, offset: Int): List<VaultEntry>? {
        return withContext(Dispatchers.Default) {
            vaultDao?.getVaultEntriesByTypePaged(type, limit, offset)?.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }
    }

    fun getActiveVaultEntryCount(): Flow<Int>? {
        return vaultDao?.getActiveVaultEntryCount()
    }

    fun getArchivedVaultEntries(): Flow<List<VaultEntry>>? {
        return vaultDao?.getArchivedVaultEntries()?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun getArchivedVaultEntriesPaged(limit: Int, offset: Int): List<VaultEntry>? {
        return withContext(Dispatchers.Default) {
            vaultDao?.getArchivedVaultEntriesPaged(limit, offset)?.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }
    }

    fun getArchivedVaultEntryCount(): Flow<Int>? {
        return vaultDao?.getArchivedVaultEntryCount()
    }

    fun searchVaultEntries(query: String): Flow<List<VaultEntry>>? {
        return vaultDao?.getAllVaultEntries()?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
                .filter { entry -> matchesSearchQuery(entry, query) }
        }?.flowOn(Dispatchers.Default)
    }

    suspend fun getAllVaultEntriesOnce(): List<VaultEntry>? {
        return withContext(Dispatchers.Default) {
            vaultDao?.getAllVaultEntriesOnce()?.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }
    }

    suspend fun getVaultEntryById(entryId: Long): VaultEntry? {
        return withContext(Dispatchers.Default) {
            vaultDao?.getAllVaultEntriesOnce()?.find { it.id == entryId }?.let { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }
    }

    suspend fun searchVaultEntriesPaged(query: String, limit: Int, offset: Int): List<VaultEntry>? {
        return withContext(Dispatchers.Default) {
            val allEntries = vaultDao?.getAllVaultEntriesOnce()?.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
            val filteredEntries = allEntries?.filter { entry -> matchesSearchQuery(entry, query) }
            filteredEntries?.drop(offset)?.take(limit)
        }
    }

    fun getSearchResultCount(query: String): Flow<Int>? {
        return vaultDao?.getAllVaultEntries()?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
                .count { entry -> matchesSearchQuery(entry, query) }
        }?.flowOn(Dispatchers.Default)
    }

    fun getRecentlyDeletedVaultEntries(): Flow<List<VaultEntry>>? {
        return vaultDao?.getRecentlyDeletedVaultEntries()?.map { list ->
            list.map { it.copy(sensitiveData = decryptSensitiveData(it)) }
        }?.flowOn(Dispatchers.Default)
    }

    fun getRecentlyDeletedCount(): Flow<Int>? {
        return vaultDao?.getRecentlyDeletedCount()
    }

    // Password Entry Operations
    suspend fun insertPasswordEntry(
        title: String,
        service: String,
        username: String,
        password: String,
        url: String = "",
        category: String = "",
        tags: String = "",
        notes: String = ""
    ): Result<Long> {
        return errorRecovery.executeWithRecovery("insert_password") {
            val passwordData = PasswordData(service, username, password, url)
            val sensitiveDataJson = Json.encodeToString(passwordData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val vaultEntry = VaultEntry(
                type = VaultEntryType.PASSWORD,
                title = title,
                category = category,
                tags = tags,
                notes = notes,
                sensitiveData = encryptedData
            )

            vaultDao?.insertVaultEntry(vaultEntry) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Bank Account Entry Operations
    suspend fun insertBankAccountEntry(
        title: String,
        bankName: String,
        accountHolder: String,
        accountNumber: String,
        routingNumber: String = "",
        iban: String = "",
        accountType: String = "Checking",
        swiftCode: String = "",
        branch: String = "",
        pin: String = "",
        phoneNumber: String = "",
        address: String = "",
        category: String = "",
        tags: String = "",
        notes: String = ""
    ): Result<Long> {
        return errorRecovery.executeWithRecovery("insert_bank_account") {
            val bankData = BankAccountData(
                bankName, accountHolder, accountNumber, routingNumber, iban,
                accountType, swiftCode, branch, pin, phoneNumber, address
            )
            val sensitiveDataJson = Json.encodeToString(bankData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val vaultEntry = VaultEntry(
                type = VaultEntryType.BANK_ACCOUNT,
                title = title,
                category = category,
                tags = tags,
                notes = notes,
                sensitiveData = encryptedData
            )

            vaultDao?.insertVaultEntry(vaultEntry) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Credit Card Entry Operations
    suspend fun insertCreditCardEntry(
        title: String,
        cardholderName: String,
        cardNumber: String,
        expirationMonth: Int,
        expirationYear: Int,
        cvv: String,
        cardType: String = "Unknown",
        issuingBank: String,
        pin: String = "",
        billingAddress: String = "",
        phoneNumber: String = "",
        notes: String = "",
        category: String = "",
        tags: String = ""
    ): Result<Long> {
        return errorRecovery.executeWithRecovery("insert_credit_card") {
            val cardData = CreditCardData(
                cardholderName, cardNumber, expirationMonth, expirationYear,
                cvv, cardType, issuingBank, pin, billingAddress, phoneNumber, notes
            )
            val sensitiveDataJson = Json.encodeToString(cardData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val vaultEntry = VaultEntry(
                type = VaultEntryType.CREDIT_CARD,
                title = title,
                category = category,
                tags = tags,
                notes = notes,
                sensitiveData = encryptedData
            )

            vaultDao?.insertVaultEntry(vaultEntry) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Identity Card Entry Operations
    suspend fun insertIdentityCardEntry(
        title: String,
        fullName: String,
        idNumber: String,
        documentType: String,
        issuingAuthority: String,
        issueDate: Long,
        expirationDate: Long,
        dateOfBirth: Long? = null,
        nationality: String = "",
        address: String = "",
        placeOfBirth: String = "",
        gender: String = "",
        height: String = "",
        eyeColor: String = "",
        bloodType: String = "",
        organDonor: Boolean = false,
        identifyingMarks: String = "",
        emergencyContact: String = "",
        additionalInfo: Map<String, String> = emptyMap(),
        category: String = "",
        tags: String = "",
        notes: String = ""
    ): Result<Long> {
        return errorRecovery.executeWithRecovery("insert_identity_card") {
            val identityData = IdentityCardData(
                fullName, idNumber, documentType, issuingAuthority, issueDate, expirationDate,
                dateOfBirth, nationality, address, placeOfBirth, gender, height, eyeColor,
                bloodType, organDonor, identifyingMarks, emergencyContact, additionalInfo
            )
            val sensitiveDataJson = Json.encodeToString(identityData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val vaultEntry = VaultEntry(
                type = VaultEntryType.IDENTITY_CARD,
                title = title,
                category = category,
                tags = tags,
                notes = notes,
                sensitiveData = encryptedData
            )

            vaultDao?.insertVaultEntry(vaultEntry) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Document Management for Identity Cards
    suspend fun addDocumentToEntry(entryId: Long, uri: Uri, fileName: String): Result<String> {
        return errorRecovery.executeWithRecovery("add_document") {
            // Save document securely
            val pathIdentifier = documentManager.saveDocument(uri, fileName).getOrThrow()

            // Update vault entry with document path
            val entry = vaultDao?.getAllVaultEntriesOnce()?.find { it.id == entryId }
                ?: throw IllegalArgumentException("Entry not found")

            val currentPaths = documentManager.parseDocumentPaths(entry.documentPaths)
            val updatedPaths = currentPaths + pathIdentifier
            val updatedPathString = documentManager.createDocumentPaths(updatedPaths)

            val updatedEntry = entry.copy(
                documentPaths = updatedPathString,
                updatedAt = System.currentTimeMillis()
            )

            vaultDao?.updateVaultEntry(updatedEntry)
            pathIdentifier
        }
    }

    suspend fun removeDocumentFromEntry(entryId: Long, pathIdentifier: String): Result<Unit> {
        return errorRecovery.executeWithRecovery("remove_document") {
            // Delete document from secure storage
            documentManager.deleteDocument(pathIdentifier).getOrThrow()

            // Update vault entry
            val entry = vaultDao?.getAllVaultEntriesOnce()?.find { it.id == entryId }
                ?: throw IllegalArgumentException("Entry not found")

            val currentPaths = documentManager.parseDocumentPaths(entry.documentPaths)
            val updatedPaths = currentPaths - pathIdentifier
            val updatedPathString = documentManager.createDocumentPaths(updatedPaths)

            val updatedEntry = entry.copy(
                documentPaths = updatedPathString,
                updatedAt = System.currentTimeMillis()
            )

            vaultDao?.updateVaultEntry(updatedEntry)
        }
    }

    suspend fun getDocument(pathIdentifier: String): Result<ByteArray> {
        return documentManager.getDocument(pathIdentifier)
    }

    suspend fun getDocumentInfo(pathIdentifier: String): Result<DocumentInfo> {
        return documentManager.getDocumentInfo(pathIdentifier)
    }

    // Generic operations
    suspend fun updateVaultEntry(entry: VaultEntry): Result<Unit> {
        return errorRecovery.executeWithRecovery("update_vault_entry") {
            vaultDao?.updateVaultEntry(entry) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Update password data operations
    suspend fun updatePasswordData(entryId: Long, service: String, username: String, password: String, url: String): Result<Unit> {
        return errorRecovery.executeWithRecovery("update_password_data") {
            val passwordData = PasswordData(service, username, password, url)
            val sensitiveDataJson = Json.encodeToString(passwordData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val entry = vaultDao?.getAllVaultEntriesOnce()?.find { it.id == entryId }
                ?: throw IllegalArgumentException("Entry not found")

            val updatedEntry = entry.copy(
                sensitiveData = encryptedData,
                updatedAt = System.currentTimeMillis()
            )

            vaultDao?.updateVaultEntry(updatedEntry)
        }
    }

    // Update bank account data operations
    suspend fun updateBankAccountData(
        entryId: Long,
        bankName: String,
        accountHolder: String,
        accountNumber: String,
        routingNumber: String,
        iban: String,
        accountType: String,
        swiftCode: String,
        branch: String,
        pin: String,
        phoneNumber: String,
        address: String
    ): Result<Unit> {
        return errorRecovery.executeWithRecovery("update_bank_account_data") {
            val bankData = BankAccountData(
                bankName, accountHolder, accountNumber, routingNumber, iban,
                accountType, swiftCode, branch, pin, phoneNumber, address
            )
            val sensitiveDataJson = Json.encodeToString(bankData)
            val encryptedData = encryptSensitiveData(sensitiveDataJson)

            val entry = vaultDao?.getAllVaultEntriesOnce()?.find { it.id == entryId }
                ?: throw IllegalArgumentException("Entry not found")

            val updatedEntry = entry.copy(
                sensitiveData = encryptedData,
                updatedAt = System.currentTimeMillis()
            )

            vaultDao?.updateVaultEntry(updatedEntry)
        }
    }

    suspend fun deleteVaultEntry(entry: VaultEntry): Result<Unit> {
        return errorRecovery.executeWithRecovery("delete_vault_entry") {
            vaultDao?.deleteVaultEntry(entry) ?: throw IllegalStateException("Database not initialized")
            Unit
        }
    }

    suspend fun archiveVaultEntry(id: Long, archived: Boolean): Result<Unit> {
        return errorRecovery.executeWithRecovery("archive_vault_entry") {
            vaultDao?.archiveVaultEntry(id, archived) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun softDeleteVaultEntry(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("soft_delete_vault_entry") {
            vaultDao?.softDeleteVaultEntry(id, System.currentTimeMillis()) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun restoreVaultEntry(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("restore_vault_entry") {
            vaultDao?.restoreVaultEntry(id) ?: throw IllegalStateException("Database not initialized")
        }
    }

    suspend fun permanentlyDeleteVaultEntry(id: Long): Result<Unit> {
        return errorRecovery.executeWithRecovery("permanently_delete_vault_entry") {
            // Clean up associated documents first
            val entry = vaultDao?.getAllVaultEntriesOnce()?.find { it.id == id }
            if (entry != null) {
                val documentPaths = documentManager.parseDocumentPaths(entry.documentPaths)
                documentPaths.forEach { path ->
                    documentManager.deleteDocument(path)
                }
            }

            vaultDao?.permanentlyDeleteVaultEntry(id) ?: throw IllegalStateException("Database not initialized")
        }
    }

    // Utility functions
    private fun encryptSensitiveData(plainJson: String): String {
        val (encrypted, iv) = cryptoManager.encrypt(plainJson.toByteArray())
        val ivStr = android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)
        val encStr = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        return "$ivStr:$encStr"
    }

    private fun decryptSensitiveData(entry: VaultEntry): String {
        return try {
            val parts = entry.sensitiveData.split(":")
            if (parts.size != 2) return "{}"
            val iv = android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT)
            val enc = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
            val plain = cryptoManager.decrypt(enc, iv)
            String(plain)
        } catch (e: Exception) {
            "{}" // Return empty JSON on decryption failure
        }
    }

    // Comprehensive search function
    fun matchesSearchQuery(entry: VaultEntry, query: String): Boolean {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isBlank()) return true

        // Search basic vault entry fields
        if (entry.title.lowercase().contains(normalizedQuery) ||
            entry.category.lowercase().contains(normalizedQuery) ||
            entry.tags.lowercase().contains(normalizedQuery) ||
            entry.notes.lowercase().contains(normalizedQuery)) {
            return true
        }

        // Search type-specific data
        when (entry.type) {
            VaultEntryType.PASSWORD -> {
                val data = getPasswordData(entry)
                if (data != null) {
                    if (data.service.lowercase().contains(normalizedQuery) ||
                        data.username.lowercase().contains(normalizedQuery) ||
                        data.url.lowercase().contains(normalizedQuery)) {
                        return true
                    }
                }
            }
            VaultEntryType.BANK_ACCOUNT -> {
                val data = getBankAccountData(entry)
                if (data != null) {
                    if (data.bankName.lowercase().contains(normalizedQuery) ||
                        data.accountHolder.lowercase().contains(normalizedQuery) ||
                        data.accountNumber.lowercase().contains(normalizedQuery) ||
                        data.routingNumber.lowercase().contains(normalizedQuery) ||
                        data.iban.lowercase().contains(normalizedQuery) ||
                        data.accountType.lowercase().contains(normalizedQuery) ||
                        data.swiftCode.lowercase().contains(normalizedQuery) ||
                        data.branch.lowercase().contains(normalizedQuery) ||
                        data.phoneNumber.lowercase().contains(normalizedQuery) ||
                        data.address.lowercase().contains(normalizedQuery)) {
                        return true
                    }
                }
            }
            VaultEntryType.CREDIT_CARD -> {
                val data = getCreditCardData(entry)
                if (data != null) {
                    if (data.cardholderName.lowercase().contains(normalizedQuery) ||
                        data.cardNumber.lowercase().contains(normalizedQuery) ||
                        data.cardType.lowercase().contains(normalizedQuery) ||
                        data.issuingBank.lowercase().contains(normalizedQuery) ||
                        data.billingAddress.lowercase().contains(normalizedQuery) ||
                        data.phoneNumber.lowercase().contains(normalizedQuery) ||
                        data.notes.lowercase().contains(normalizedQuery)) {
                        return true
                    }
                }
            }
            VaultEntryType.IDENTITY_CARD -> {
                val data = getIdentityCardData(entry)
                if (data != null) {
                    if (data.fullName.lowercase().contains(normalizedQuery) ||
                        data.idNumber.lowercase().contains(normalizedQuery) ||
                        data.documentType.lowercase().contains(normalizedQuery) ||
                        data.issuingAuthority.lowercase().contains(normalizedQuery) ||
                        data.nationality.lowercase().contains(normalizedQuery) ||
                        data.address.lowercase().contains(normalizedQuery) ||
                        data.placeOfBirth.lowercase().contains(normalizedQuery) ||
                        data.gender.lowercase().contains(normalizedQuery) ||
                        data.height.lowercase().contains(normalizedQuery) ||
                        data.eyeColor.lowercase().contains(normalizedQuery) ||
                        data.bloodType.lowercase().contains(normalizedQuery) ||
                        data.identifyingMarks.lowercase().contains(normalizedQuery) ||
                        data.emergencyContact.lowercase().contains(normalizedQuery)) {
                        return true
                    }

                    // Search additional info map
                    if (data.additionalInfo.values.any { it.lowercase().contains(normalizedQuery) }) {
                        return true
                    }
                }

                // Search document content (file names and metadata)
                if (entry.documentPaths.isNotBlank()) {
                    val documentPaths = documentManager.parseDocumentPaths(entry.documentPaths)
                    for (pathIdentifier in documentPaths) {
                        try {
                            // Note: This is a synchronous search, document info is not fetched in real-time
                            // For full document search, this would need to be made async
                            // For now, just search in document paths string
                            if (entry.documentPaths.lowercase().contains(normalizedQuery)) {
                                return true
                            }
                        } catch (e: Exception) {
                            // Continue searching other documents
                        }
                    }
                }
            }
        }

        return false
    }

    // Type-specific getters
    fun getPasswordData(entry: VaultEntry): PasswordData? {
        if (entry.type != VaultEntryType.PASSWORD) return null
        return try {
            Json.decodeFromString<PasswordData>(entry.sensitiveData)
        } catch (e: Exception) {
            null
        }
    }

    fun getBankAccountData(entry: VaultEntry): BankAccountData? {
        if (entry.type != VaultEntryType.BANK_ACCOUNT) return null
        return try {
            Json.decodeFromString<BankAccountData>(entry.sensitiveData)
        } catch (e: Exception) {
            null
        }
    }

    fun getCreditCardData(entry: VaultEntry): CreditCardData? {
        if (entry.type != VaultEntryType.CREDIT_CARD) return null
        return try {
            Json.decodeFromString<CreditCardData>(entry.sensitiveData)
        } catch (e: Exception) {
            null
        }
    }

    fun getIdentityCardData(entry: VaultEntry): IdentityCardData? {
        if (entry.type != VaultEntryType.IDENTITY_CARD) return null
        return try {
            Json.decodeFromString<IdentityCardData>(entry.sensitiveData)
        } catch (e: Exception) {
            null
        }
    }
}
