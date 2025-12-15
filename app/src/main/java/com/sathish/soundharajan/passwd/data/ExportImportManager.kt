package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.sathish.soundharajan.passwd.data.models.*
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
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
    private val vaultRepository: VaultRepository,
    private val documentManager: DocumentManager
) {

    companion object {
        private const val EXPORT_VERSION = "2.0"
        private const val EXPORT_FORMAT_JSON = "json"
    }

    @Serializable
    data class ExportData(
        val version: String,
        val timestamp: Long,
        val totalEntries: Int,
        val passwords: List<ExportedPassword>,
        val bankAccounts: List<ExportedBankAccount>,
        val creditCards: List<ExportedCreditCard>,
        val identityCards: List<ExportedIdentityCard>,
        val documents: List<ExportedDocument>
    )

    @Serializable
    data class ExportedPassword(
        val id: Long,
        val title: String,
        val service: String,
        val username: String,
        val password: String,
        val url: String,
        val category: String,
        val tags: String,
        val notes: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isArchived: Boolean
    )

    @Serializable
    data class ExportedBankAccount(
        val id: Long,
        val title: String,
        val bankName: String,
        val accountHolder: String,
        val accountNumber: String,
        val routingNumber: String,
        val iban: String,
        val accountType: String,
        val swiftCode: String,
        val branch: String,
        val pin: String,
        val phoneNumber: String,
        val address: String,
        val category: String,
        val tags: String,
        val notes: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isArchived: Boolean
    )

    @Serializable
    data class ExportedCreditCard(
        val id: Long,
        val title: String,
        val cardholderName: String,
        val cardNumber: String,
        val expirationMonth: Int,
        val expirationYear: Int,
        val cvv: String,
        val cardType: String,
        val issuingBank: String,
        val pin: String,
        val billingAddress: String,
        val phoneNumber: String,
        val notes: String,
        val category: String,
        val tags: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isArchived: Boolean
    )

    @Serializable
    data class ExportedIdentityCard(
        val id: Long,
        val title: String,
        val fullName: String,
        val idNumber: String,
        val documentType: String,
        val issuingAuthority: String,
        val issueDate: Long,
        val expirationDate: Long,
        val dateOfBirth: Long?,
        val nationality: String,
        val address: String,
        val placeOfBirth: String,
        val gender: String,
        val height: String,
        val eyeColor: String,
        val bloodType: String,
        val organDonor: Boolean,
        val identifyingMarks: String,
        val emergencyContact: String,
        val additionalInfo: Map<String, String>,
        val category: String,
        val tags: String,
        val notes: String,
        val documentPaths: String,
        val createdAt: Long,
        val updatedAt: Long,
        val isArchived: Boolean
    )

    @Serializable
    data class ExportedDocument(
        val pathIdentifier: String,
        val fileName: String,
        val mimeType: String,
        val size: Long,
        val checksum: String
    )

    suspend fun exportVaultEntries(
        vaultEntries: List<VaultEntry>,
        format: String = EXPORT_FORMAT_JSON,
        masterPassword: String,
        destinationUri: Uri
    ): Result<VaultExportResult> = withContext(Dispatchers.IO) {
        try {
            val exportData = createVaultExportData(vaultEntries)
            val serializedData = Json.encodeToString(exportData)

            // Create ZIP file containing data and documents
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Add main data file
                    zipOut.putNextEntry(ZipEntry("vault_data.json"))
                    zipOut.write(serializedData.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()

                    // Add documents
                    for (document in exportData.documents) {
                        try {
                            val documentData = documentManager.getDocument(document.pathIdentifier).getOrThrow()
                            zipOut.putNextEntry(ZipEntry("documents/${document.fileName}"))
                            zipOut.write(documentData)
                            zipOut.closeEntry()
                        } catch (e: Exception) {
                            // Skip documents that can't be read
                        }
                    }
                }
            } ?: throw Exception("Failed to open output stream")

            // Encrypt the entire ZIP file
            val zipData = context.contentResolver.openInputStream(destinationUri)?.use { it.readBytes() }
                ?: throw Exception("Failed to read created ZIP file")

            val encryptedData = encryptExportData(String(zipData, Charsets.ISO_8859_1), masterPassword)

            // Write encrypted data back
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(encryptedData)
            } ?: throw Exception("Failed to write encrypted data")

            Result.success(VaultExportResult(
                totalEntries = vaultEntries.size,
                passwordCount = exportData.passwords.size,
                bankAccountCount = exportData.bankAccounts.size,
                creditCardCount = exportData.creditCards.size,
                identityCardCount = exportData.identityCards.size,
                documentCount = exportData.documents.size,
                fileSize = encryptedData.size,
                format = format,
                timestamp = System.currentTimeMillis()
            ))

        } catch (e: Exception) {
            Result.failure(Exception("Export failed: ${e.localizedMessage}", e))
        }
    }

    suspend fun importVaultEntries(
        sourceUri: Uri,
        masterPassword: String,
        conflictStrategy: ConflictStrategy = ConflictStrategy.SKIP
    ): Result<VaultImportResult> = withContext(Dispatchers.IO) {
        try {
            val encryptedData = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw Exception("Failed to read file")

            val decryptedData = decryptImportData(encryptedData, masterPassword)

            // Extract ZIP contents
            val zipData = decryptedData.toByteArray(Charsets.ISO_8859_1)
            val extractedData = extractZipContents(zipData)

            val exportData = Json.decodeFromString<ExportData>(extractedData["vault_data.json"] ?: throw Exception("Missing vault data"))
            val importedEntries = processVaultImportData(exportData, extractedData, conflictStrategy)

            Result.success(VaultImportResult(
                totalEntries = exportData.totalEntries,
                importedEntries = importedEntries.size,
                skippedEntries = exportData.totalEntries - importedEntries.size,
                conflictsResolved = importedEntries.count { it.isUpdate },
                importedDocuments = importedEntries.sumOf { it.documentCount }
            ))

        } catch (e: Exception) {
            Result.failure(Exception("Import failed: ${e.localizedMessage}", e))
        }
    }

    private suspend fun createVaultExportData(vaultEntries: List<VaultEntry>): ExportData {
        val passwords = mutableListOf<ExportedPassword>()
        val bankAccounts = mutableListOf<ExportedBankAccount>()
        val creditCards = mutableListOf<ExportedCreditCard>()
        val identityCards = mutableListOf<ExportedIdentityCard>()
        val documents = mutableListOf<ExportedDocument>()

        vaultEntries.forEach { entry ->
            when (entry.type) {
                VaultEntryType.PASSWORD -> {
                    vaultRepository.getPasswordData(entry)?.let { data ->
                        passwords.add(ExportedPassword(
                            id = entry.id,
                            title = entry.title,
                            service = data.service,
                            username = data.username,
                            password = data.password,
                            url = data.url,
                            category = entry.category,
                            tags = entry.tags,
                            notes = entry.notes,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            isArchived = entry.isArchived
                        ))
                    }
                }
                VaultEntryType.BANK_ACCOUNT -> {
                    vaultRepository.getBankAccountData(entry)?.let { data ->
                        bankAccounts.add(ExportedBankAccount(
                            id = entry.id,
                            title = entry.title,
                            bankName = data.bankName,
                            accountHolder = data.accountHolder,
                            accountNumber = data.accountNumber,
                            routingNumber = data.routingNumber,
                            iban = data.iban,
                            accountType = data.accountType,
                            swiftCode = data.swiftCode,
                            branch = data.branch,
                            pin = data.pin,
                            phoneNumber = data.phoneNumber,
                            address = data.address,
                            category = entry.category,
                            tags = entry.tags,
                            notes = entry.notes,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            isArchived = entry.isArchived
                        ))
                    }
                }
                VaultEntryType.CREDIT_CARD -> {
                    vaultRepository.getCreditCardData(entry)?.let { data ->
                        creditCards.add(ExportedCreditCard(
                            id = entry.id,
                            title = entry.title,
                            cardholderName = data.cardholderName,
                            cardNumber = data.cardNumber,
                            expirationMonth = data.expirationMonth,
                            expirationYear = data.expirationYear,
                            cvv = data.cvv,
                            cardType = data.cardType,
                            issuingBank = data.issuingBank,
                            pin = data.pin,
                            billingAddress = data.billingAddress,
                            phoneNumber = data.phoneNumber,
                            notes = data.notes,
                            category = entry.category,
                            tags = entry.tags,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            isArchived = entry.isArchived
                        ))
                    }
                }
                VaultEntryType.IDENTITY_CARD -> {
                    vaultRepository.getIdentityCardData(entry)?.let { data ->
                        identityCards.add(ExportedIdentityCard(
                            id = entry.id,
                            title = entry.title,
                            fullName = data.fullName,
                            idNumber = data.idNumber,
                            documentType = data.documentType,
                            issuingAuthority = data.issuingAuthority,
                            issueDate = data.issueDate,
                            expirationDate = data.expirationDate,
                            dateOfBirth = data.dateOfBirth,
                            nationality = data.nationality,
                            address = data.address,
                            placeOfBirth = data.placeOfBirth,
                            gender = data.gender,
                            height = data.height,
                            eyeColor = data.eyeColor,
                            bloodType = data.bloodType,
                            organDonor = data.organDonor,
                            identifyingMarks = data.identifyingMarks,
                            emergencyContact = data.emergencyContact,
                            additionalInfo = data.additionalInfo,
                            category = entry.category,
                            tags = entry.tags,
                            notes = entry.notes,
                            documentPaths = entry.documentPaths,
                            createdAt = entry.createdAt,
                            updatedAt = entry.updatedAt,
                            isArchived = entry.isArchived
                        ))

                        // Collect document metadata
                        val documentPaths = documentManager.parseDocumentPaths(entry.documentPaths)
                        documentPaths.forEach { pathIdentifier ->
                            try {
                                val docInfo = documentManager.getDocumentInfo(pathIdentifier).getOrNull()
                                val documentData = documentManager.getDocument(pathIdentifier).getOrNull()
                                if (docInfo != null && documentData != null) {
                                    val checksum = calculateChecksum(documentData)
                                    documents.add(ExportedDocument(
                                        pathIdentifier = pathIdentifier,
                                        fileName = docInfo.fileName,
                                        mimeType = docInfo.fileType,
                                        size = documentData.size.toLong(),
                                        checksum = checksum
                                    ))
                                }
                            } catch (e: Exception) {
                                // Skip documents that can't be read
                            }
                        }
                    }
                }
            }
        }

        return ExportData(
            version = EXPORT_VERSION,
            timestamp = System.currentTimeMillis(),
            totalEntries = vaultEntries.size,
            passwords = passwords,
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            identityCards = identityCards,
            documents = documents
        )
    }

    private fun extractZipContents(zipData: ByteArray): Map<String, String> {
        val extractedFiles = mutableMapOf<String, String>()

        ZipInputStream(zipData.inputStream()).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val content = zipIn.readBytes().toString(Charsets.UTF_8)
                    extractedFiles[entry.name] = content
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        return extractedFiles
    }

    private suspend fun processVaultImportData(
        exportData: ExportData,
        extractedFiles: Map<String, String>,
        conflictStrategy: ConflictStrategy
    ): List<VaultImportEntry> {
        val importedEntries = mutableListOf<VaultImportEntry>()

        // Import passwords
        for (exportedPassword in exportData.passwords) {
            try {
                vaultRepository.insertPasswordEntry(
                    title = exportedPassword.title,
                    service = exportedPassword.service,
                    username = exportedPassword.username,
                    password = exportedPassword.password,
                    url = exportedPassword.url,
                    category = exportedPassword.category,
                    tags = exportedPassword.tags,
                    notes = exportedPassword.notes
                ).fold(
                    onSuccess = { importedEntries.add(VaultImportEntry(VaultEntryType.PASSWORD, false, 0)) },
                    onFailure = { /* Skip on failure */ }
                )
            } catch (e: Exception) {
                // Skip failed imports
            }
        }

        // Import bank accounts
        for (exportedBank in exportData.bankAccounts) {
            try {
                vaultRepository.insertBankAccountEntry(
                    title = exportedBank.title,
                    bankName = exportedBank.bankName,
                    accountHolder = exportedBank.accountHolder,
                    accountNumber = exportedBank.accountNumber,
                    routingNumber = exportedBank.routingNumber,
                    iban = exportedBank.iban,
                    accountType = exportedBank.accountType,
                    swiftCode = exportedBank.swiftCode,
                    branch = exportedBank.branch,
                    pin = exportedBank.pin,
                    phoneNumber = exportedBank.phoneNumber,
                    address = exportedBank.address,
                    category = exportedBank.category,
                    tags = exportedBank.tags,
                    notes = exportedBank.notes
                ).fold(
                    onSuccess = { importedEntries.add(VaultImportEntry(VaultEntryType.BANK_ACCOUNT, false, 0)) },
                    onFailure = { /* Skip on failure */ }
                )
            } catch (e: Exception) {
                // Skip failed imports
            }
        }

        // Import credit cards
        for (exportedCard in exportData.creditCards) {
            try {
                vaultRepository.insertCreditCardEntry(
                    title = exportedCard.title,
                    cardholderName = exportedCard.cardholderName,
                    cardNumber = exportedCard.cardNumber,
                    expirationMonth = exportedCard.expirationMonth,
                    expirationYear = exportedCard.expirationYear,
                    cvv = exportedCard.cvv,
                    cardType = exportedCard.cardType,
                    issuingBank = exportedCard.issuingBank,
                    pin = exportedCard.pin,
                    billingAddress = exportedCard.billingAddress,
                    phoneNumber = exportedCard.phoneNumber,
                    notes = exportedCard.notes,
                    category = exportedCard.category,
                    tags = exportedCard.tags
                ).fold(
                    onSuccess = { importedEntries.add(VaultImportEntry(VaultEntryType.CREDIT_CARD, false, 0)) },
                    onFailure = { /* Skip on failure */ }
                )
            } catch (e: Exception) {
                // Skip failed imports
            }
        }

        // Import identity cards with documents
        for (exportedId in exportData.identityCards) {
            try {
                vaultRepository.insertIdentityCardEntry(
                    title = exportedId.title,
                    fullName = exportedId.fullName,
                    idNumber = exportedId.idNumber,
                    documentType = exportedId.documentType,
                    issuingAuthority = exportedId.issuingAuthority,
                    issueDate = exportedId.issueDate,
                    expirationDate = exportedId.expirationDate,
                    dateOfBirth = exportedId.dateOfBirth,
                    nationality = exportedId.nationality,
                    address = exportedId.address,
                    placeOfBirth = exportedId.placeOfBirth,
                    gender = exportedId.gender,
                    height = exportedId.height,
                    eyeColor = exportedId.eyeColor,
                    bloodType = exportedId.bloodType,
                    organDonor = exportedId.organDonor,
                    identifyingMarks = exportedId.identifyingMarks,
                    emergencyContact = exportedId.emergencyContact,
                    additionalInfo = exportedId.additionalInfo,
                    category = exportedId.category,
                    tags = exportedId.tags,
                    notes = exportedId.notes
                ).fold(
                    onSuccess = { entryId ->
                        // Import associated documents
                        var documentCount = 0
                        val documentPaths = documentManager.parseDocumentPaths(exportedId.documentPaths)
                        documentPaths.forEach { pathIdentifier ->
                            try {
                                val documentFileName = "documents/${documentManager.getDocumentInfo(pathIdentifier).getOrNull()?.fileName}"
                                val documentData = extractedFiles[documentFileName]?.toByteArray(Charsets.ISO_8859_1)
                                if (documentData != null) {
                                    // Save document and link to entry
                                    val newPathIdentifier = documentManager.saveDocument(
                                        Uri.parse("content://temp"), // This would need proper URI handling
                                        documentManager.getDocumentInfo(pathIdentifier).getOrNull()?.fileName ?: "document"
                                    ).getOrThrow()

                                    vaultRepository.addDocumentToEntry(entryId, Uri.parse("content://temp"), "temp").getOrThrow()
                                    documentCount++
                                }
                            } catch (e: Exception) {
                                // Skip failed document imports
                            }
                        }
                        importedEntries.add(VaultImportEntry(VaultEntryType.IDENTITY_CARD, false, documentCount))
                    },
                    onFailure = { /* Skip on failure */ }
                )
            } catch (e: Exception) {
                // Skip failed imports
            }
        }

        return importedEntries
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun encryptExportData(data: String, masterPassword: String): ByteArray {
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
        val salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT)

        val keyBytes = cryptoManager.deriveDatabaseKey(masterPassword.toCharArray(), salt)
        val key = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    fun generateExportFilename(format: String = EXPORT_FORMAT_JSON): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "vault_export_$timestamp.$format"
    }

    enum class ConflictStrategy { SKIP, OVERWRITE }

    data class VaultExportResult(
        val totalEntries: Int,
        val passwordCount: Int,
        val bankAccountCount: Int,
        val creditCardCount: Int,
        val identityCardCount: Int,
        val documentCount: Int,
        val fileSize: Int,
        val format: String,
        val timestamp: Long
    )

    data class VaultImportResult(
        val totalEntries: Int,
        val importedEntries: Int,
        val skippedEntries: Int,
        val conflictsResolved: Int,
        val importedDocuments: Int
    )

    data class VaultImportEntry(
        val type: VaultEntryType,
        val isUpdate: Boolean,
        val documentCount: Int
    )
}
