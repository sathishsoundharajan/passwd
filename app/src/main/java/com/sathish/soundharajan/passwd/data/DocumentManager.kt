package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.sathish.soundharajan.passwd.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {

    companion object {
        private const val DOCUMENTS_DIR = "documents"
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB limit
        private const val THUMBNAIL_SIZE = 200 // pixels
    }

    private val documentsDir: File
        get() = File(context.filesDir, DOCUMENTS_DIR).apply { mkdirs() }

    /**
     * Saves a document from URI to secure storage
     * @return encrypted file path identifier
     */
    suspend fun saveDocument(uri: Uri, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open document"))

            val tempFile = File.createTempFile("temp_doc", null, context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Check file size
            if (tempFile.length() > MAX_FILE_SIZE) {
                tempFile.delete()
                return@withContext Result.failure(Exception("File too large (max 10MB)"))
            }

            // Generate unique filename
            val extension = getFileExtension(fileName)
            val uniqueId = UUID.randomUUID().toString()
            val secureFileName = "${uniqueId}_encrypted.$extension"
            val secureFile = File(documentsDir, secureFileName)

            // Read and encrypt file content
            val fileBytes = tempFile.readBytes()
            val (encryptedData, iv) = cryptoManager.encrypt(fileBytes)

            // Save encrypted data
            secureFile.writeBytes(encryptedData + iv)

            // Clean up temp file
            tempFile.delete()

            // Return encrypted path identifier (not actual file path for security)
            val pathIdentifier = Base64.encodeToString(uniqueId.toByteArray(), Base64.NO_WRAP)
            Result.success(pathIdentifier)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a document from secure storage
     * @return decrypted file bytes
     */
    suspend fun getDocument(pathIdentifier: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val uniqueId = String(Base64.decode(pathIdentifier, Base64.NO_WRAP))
            val files = documentsDir.listFiles { _, name ->
                name.startsWith("${uniqueId}_encrypted.")
            }

            if (files.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Document not found"))
            }

            val encryptedFile = files.first()
            val encryptedDataWithIv = encryptedFile.readBytes()

            // Split encrypted data and IV (IV is last 16 bytes for GCM)
            val encryptedData = encryptedDataWithIv.copyOfRange(0, encryptedDataWithIv.size - 16)
            val iv = encryptedDataWithIv.copyOfRange(encryptedDataWithIv.size - 16, encryptedDataWithIv.size)

            val decryptedData = cryptoManager.decrypt(encryptedData, iv)
            Result.success(decryptedData)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a document from secure storage
     */
    suspend fun deleteDocument(pathIdentifier: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uniqueId = String(Base64.decode(pathIdentifier, Base64.NO_WRAP))
            val files = documentsDir.listFiles { _, name ->
                name.startsWith("${uniqueId}_encrypted.")
            }

            files?.forEach { it.delete() }
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets document metadata without decrypting
     */
    suspend fun getDocumentInfo(pathIdentifier: String): Result<DocumentInfo> = withContext(Dispatchers.IO) {
        try {
            val uniqueId = String(Base64.decode(pathIdentifier, Base64.NO_WRAP))
            val files = documentsDir.listFiles { _, name ->
                name.startsWith("${uniqueId}_encrypted.")
            }

            if (files.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Document not found"))
            }

            val file = files.first()
            val extension = file.name.substringAfterLast(".")
            val size = file.length()

            Result.success(DocumentInfo(
                pathIdentifier = pathIdentifier,
                fileName = "document.$extension",
                fileSize = size,
                fileType = getMimeTypeFromExtension(extension)
            ))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all documents for a vault entry
     */
    fun parseDocumentPaths(documentPaths: String): List<String> {
        return if (documentPaths.isBlank()) emptyList()
        else documentPaths.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * Creates document paths string from list
     */
    fun createDocumentPaths(paths: List<String>): String {
        return paths.joinToString(",")
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "bin")
    }

    private fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    /**
     * Cleans up orphaned documents (documents not referenced by any vault entry)
     */
    suspend fun cleanupOrphanedDocuments(activePathIdentifiers: Set<String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val files = documentsDir.listFiles() ?: emptyArray()
            var deletedCount = 0

            for (file in files) {
                val uniqueId = file.name.substringBefore("_encrypted.")
                val pathIdentifier = Base64.encodeToString(uniqueId.toByteArray(), Base64.NO_WRAP)

                if (pathIdentifier !in activePathIdentifiers) {
                    file.delete()
                    deletedCount++
                }
            }

            Result.success(deletedCount)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class DocumentInfo(
    val pathIdentifier: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String
)
