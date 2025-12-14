package com.sathish.soundharajan.passwd.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.sathish.soundharajan.passwd.security.AuthManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.seconds
import kotlin.Result

/**
 * Comprehensive error recovery system for the password manager
 */
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorRecovery @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "ErrorRecovery"
        private const val PREFS_NAME = "error_recovery_prefs"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_BACKUP_COUNT = "backup_count"
        private const val KEY_RECOVERY_ATTEMPTS = "recovery_attempts"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BACKUP_INTERVAL_HOURS = 24
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Circuit breaker state
    private val _circuitBreakerState = MutableStateFlow<CircuitBreakerState>(CircuitBreakerState.Closed)
    val circuitBreakerState: StateFlow<CircuitBreakerState> = _circuitBreakerState.asStateFlow()

    // Recovery suggestions
    private val _recoverySuggestions = MutableStateFlow<List<RecoverySuggestion>>(emptyList())
    val recoverySuggestions: StateFlow<List<RecoverySuggestion>> = _recoverySuggestions.asStateFlow()

    enum class CircuitBreakerState {
        Closed,    // Normal operation
        Open,      // Failing, requests rejected
        HalfOpen   // Testing if service recovered
    }

    sealed class RecoverySuggestion {
        data class RetryOperation(val operation: String) : RecoverySuggestion()
        data class RestoreFromBackup(val backupTime: Long) : RecoverySuggestion()
        data class ReinitializeDatabase(val reason: String) : RecoverySuggestion()
        data class CheckPermissions(val permission: String) : RecoverySuggestion()
        data class ClearCache(val cacheType: String) : RecoverySuggestion()
        data class ContactSupport(val errorCode: String) : RecoverySuggestion()
    }

    /**
     * Execute operation with circuit breaker and retry logic
     */
    suspend fun <T> executeWithRecovery(
        operation: String,
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            // Check circuit breaker
            if (_circuitBreakerState.value == CircuitBreakerState.Open) {
                return@withContext Result.failure(Exception("Circuit breaker is open for $operation"))
            }

            // Execute with retry logic
            var lastException: Exception? = null
            for (attempt in 1..MAX_RETRY_ATTEMPTS) {
                try {
                    val result = block()
                    // Success - reset circuit breaker if it was half-open
                    if (_circuitBreakerState.value == CircuitBreakerState.HalfOpen) {
                        _circuitBreakerState.value = CircuitBreakerState.Closed
                    }
                    return@withContext Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Attempt $attempt failed for $operation: ${e.message}")

                    // Classify error and determine recovery strategy
                    val errorType = classifyError(e)
                    handleError(operation, errorType, attempt, e)

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        // Exponential backoff: 1s, 2s, 4s
                        delay((1L shl (attempt - 1)).seconds)
                    }
                }
            }

            // All retries failed
            _circuitBreakerState.value = CircuitBreakerState.Open
            Result.failure(lastException ?: Exception("Unknown error in $operation"))

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in $operation", e)
            Result.failure(e)
        }
    }

    /**
     * Classify errors to determine appropriate recovery strategies
     */
    private fun classifyError(exception: Exception): ErrorType {
        return when {
            exception.message?.contains("database", ignoreCase = true) == true ||
            exception.message?.contains("sqlite", ignoreCase = true) == true -> {
                ErrorType.DatabaseError
            }
            exception.message?.contains("permission", ignoreCase = true) == true ||
            exception.message?.contains("access", ignoreCase = true) == true -> {
                ErrorType.PermissionError
            }
            exception.message?.contains("network", ignoreCase = true) == true ||
            exception.message?.contains("connection", ignoreCase = true) == true -> {
                ErrorType.NetworkError
            }
            exception.message?.contains("encryption", ignoreCase = true) == true ||
            exception.message?.contains("crypto", ignoreCase = true) == true -> {
                ErrorType.EncryptionError
            }
            exception.message?.contains("memory", ignoreCase = true) == true ||
            exception.message?.contains("outofmemory", ignoreCase = true) == true -> {
                ErrorType.MemoryError
            }
            else -> ErrorType.UnknownError
        }
    }

    private enum class ErrorType {
        DatabaseError, PermissionError, NetworkError, EncryptionError, MemoryError, UnknownError
    }

    /**
     * Handle errors and generate recovery suggestions
     */
    private fun handleError(operation: String, errorType: ErrorType, attempt: Int, exception: Exception) {
        val suggestions = mutableListOf<RecoverySuggestion>()

        when (errorType) {
            ErrorType.DatabaseError -> {
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    suggestions.add(RecoverySuggestion.ReinitializeDatabase("Database corruption detected"))
                    suggestions.add(RecoverySuggestion.RestoreFromBackup(getLastBackupTime()))
                } else {
                    suggestions.add(RecoverySuggestion.RetryOperation(operation))
                }
            }
            ErrorType.PermissionError -> {
                suggestions.add(RecoverySuggestion.CheckPermissions("Storage access required"))
            }
            ErrorType.NetworkError -> {
                suggestions.add(RecoverySuggestion.RetryOperation("Network operation"))
            }
            ErrorType.EncryptionError -> {
                suggestions.add(RecoverySuggestion.RestoreFromBackup(getLastBackupTime()))
                suggestions.add(RecoverySuggestion.ContactSupport("ENCRYPTION_ERROR"))
            }
            ErrorType.MemoryError -> {
                suggestions.add(RecoverySuggestion.ClearCache("Application cache"))
            }
            ErrorType.UnknownError -> {
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    suggestions.add(RecoverySuggestion.ContactSupport("UNKNOWN_ERROR"))
                } else {
                    suggestions.add(RecoverySuggestion.RetryOperation(operation))
                }
            }
        }

        _recoverySuggestions.value = suggestions
    }

    /**
     * Create automatic backup of password data
     */
    suspend fun createBackup(): Result<Unit> = executeWithRecovery("backup") {
        try {
            // In a real implementation, this would create an encrypted backup
            // For now, we'll just update the backup timestamp
            val currentTime = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_BACKUP_TIME, currentTime)
                .putInt(KEY_BACKUP_COUNT, getBackupCount() + 1)
                .apply()

            Log.i(TAG, "Backup created successfully at $currentTime")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Backup creation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Restore from backup
     */
    suspend fun restoreFromBackup(): Result<Unit> = executeWithRecovery("restore") {
        // Implementation would restore from encrypted backup
        // This is a placeholder for the actual implementation
        Log.i(TAG, "Restore from backup initiated")
        Result.success(Unit)
    }

    /**
     * Check if backup is needed
     */
    fun shouldCreateBackup(): Boolean {
        val lastBackup = getLastBackupTime()
        val hoursSinceBackup = (System.currentTimeMillis() - lastBackup) / (1000 * 60 * 60)
        return hoursSinceBackup >= BACKUP_INTERVAL_HOURS
    }

    /**
     * Get last backup timestamp
     */
    fun getLastBackupTime(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    /**
     * Get backup count
     */
    fun getBackupCount(): Int {
        return prefs.getInt(KEY_BACKUP_COUNT, 0)
    }

    /**
     * Reset circuit breaker (admin function)
     */
    fun resetCircuitBreaker() {
        _circuitBreakerState.value = CircuitBreakerState.Closed
        _recoverySuggestions.value = emptyList()
    }

    /**
     * Get recovery attempts count
     */
    fun getRecoveryAttempts(): Int {
        return prefs.getInt(KEY_RECOVERY_ATTEMPTS, 0)
    }

    /**
     * Increment recovery attempts
     */
    private fun incrementRecoveryAttempts() {
        val current = getRecoveryAttempts()
        prefs.edit().putInt(KEY_RECOVERY_ATTEMPTS, current + 1).apply()
    }

    /**
     * Clear recovery suggestions
     */
    fun clearRecoverySuggestions() {
        _recoverySuggestions.value = emptyList()
    }
}
