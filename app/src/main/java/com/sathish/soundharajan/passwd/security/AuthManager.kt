package com.sathish.soundharajan.passwd.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val cryptoManager: CryptoManager
) {

    companion object {
        private const val MASTER_PASSWORD_HASH_KEY = "master_password_hash"
        private const val MASTER_PASSWORD_SALT_KEY = "master_password_salt"
        private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
        private const val BIOMETRIC_PASSWORD_KEY = "biometric_password"
        private const val BIOMETRIC_IV_KEY = "biometric_iv"
        private const val BIOMETRIC_KEY_ALIAS = "biometric_key"
    }

    fun isMasterPasswordSet(): Boolean {
        return prefs.contains(MASTER_PASSWORD_HASH_KEY)
    }

    fun setMasterPassword(password: String) {
        // Generate new salt using CryptoManager (random 16 bytes)
        val salt = cryptoManager.generateSalt() 
        // Hash the password using the salt
        val hash = hashPassword(password.toCharArray(), salt)
        
        prefs.edit()
            .putString(MASTER_PASSWORD_HASH_KEY, hash)
            .putString(MASTER_PASSWORD_SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    fun verifyMasterPassword(password: String): Boolean {
        val storedHash = prefs.getString(MASTER_PASSWORD_HASH_KEY, null) ?: return false
        val storedSaltStr = prefs.getString(MASTER_PASSWORD_SALT_KEY, null) ?: return false
        val storedSalt = Base64.decode(storedSaltStr, Base64.NO_WRAP)
        
        val computedHash = hashPassword(password.toCharArray(), storedSalt)
        // Trim just in case legacy used Default
        return computedHash.trim() == storedHash.trim()
    }

    fun getDatabaseKey(password: String): ByteArray {
        // Note: For the database key, we use the specific logic from CryptoManager
        // which might use a separate, secure salt stored in EncryptedSharedPreferences
        // or the same salt. For "Project Ironclad", we use the improved CryptoManager.
        return cryptoManager.deriveDatabaseKey(password.toCharArray())
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return result == BiometricManager.BIOMETRIC_SUCCESS ||
               result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED // Available but not enrolled
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(BIOMETRIC_ENABLED_KEY, false)
    }

    fun setBiometricEnabled(enabled: Boolean, masterPassword: String? = null) {
        if (enabled && masterPassword != null) {
            storeBiometricPassword(masterPassword)
        } else if (!enabled) {
            clearBiometricPassword()
        }
        prefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, enabled).apply()
    }

    private fun storeBiometricPassword(password: String) {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(BIOMETRIC_KEY_ALIAS, null))

            val encryptedPassword = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            prefs.edit()
                .putString(BIOMETRIC_PASSWORD_KEY, Base64.encodeToString(encryptedPassword, Base64.DEFAULT))
                .putString(BIOMETRIC_IV_KEY, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            prefs.edit().putBoolean(BIOMETRIC_ENABLED_KEY, false).apply()
            throw RuntimeException("Failed to store biometric password", e)
        }
    }

    fun retrieveBiometricPassword(): String? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val encryptedPassword = prefs.getString(BIOMETRIC_PASSWORD_KEY, null)
            val ivString = prefs.getString(BIOMETRIC_IV_KEY, null)

            if (encryptedPassword == null || ivString == null) {
                return null
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            val gcmParameterSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, keyStore.getKey(BIOMETRIC_KEY_ALIAS, null), gcmParameterSpec)

            val decryptedPassword = cipher.doFinal(Base64.decode(encryptedPassword, Base64.DEFAULT))
            String(decryptedPassword, Charsets.UTF_8)
        } catch (e: Exception) {
            clearBiometricPassword()
            null
        }
    }

    private fun clearBiometricPassword() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
        } catch (e: Exception) {
            // Ignore
        }

        prefs.edit()
            .remove(BIOMETRIC_PASSWORD_KEY)
            .remove(BIOMETRIC_IV_KEY)
            .apply()
    }

    suspend fun authenticateWithBiometric(activity: FragmentActivity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate")
                .setSubtitle("Use your biometric credential")
                .setNegativeButtonText("Cancel")
                .build()

            val biometricPrompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(true)
                }

                override fun onAuthenticationFailed() {
                    continuation.resume(false)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(false)
                }
            })

            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): String {
        // Use PBKDF2 for password hashing for verification (separate from DB key derivation which uses 600k rounds)
        // We can keep 10k here for quick login checks or upgrade it. Let's upgrade to 100k for balance.
        val iterations = 100_000 
        val spec = PBEKeySpec(password, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}
