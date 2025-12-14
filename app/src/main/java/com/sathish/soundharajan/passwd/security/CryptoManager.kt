package com.sathish.soundharajan.passwd.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

interface CryptoManager {
    fun getMasterKey(): SecretKey
    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray>
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray
    fun deriveDatabaseKey(masterPassword: CharArray, salt: ByteArray? = null): ByteArray
    fun generateSalt(): ByteArray
    fun getOrGenerateSalt(): ByteArray
}

@Singleton
class AndroidCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) : CryptoManager {

    companion object {
        private const val KEY_ALIAS = "password_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val PREFS_FILE = "secure_prefs"
        private const val SALT_KEY = "db_salt"
    }

    private val keyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    
    private fun generateMasterKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // For now, we handle auth separately
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private var cachedMasterKey: SecretKey? = null

    @Synchronized
    override fun getMasterKey(): SecretKey {
        if (cachedMasterKey != null) return cachedMasterKey!!

        cachedMasterKey = if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            generateMasterKey()
        }
        return cachedMasterKey!!
    }

    override fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val encryptedData = cipher.doFinal(data)
        val iv = cipher.iv
        return Pair(encryptedData, iv)
    }

    override fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher.doFinal(encryptedData)
    }

    override fun deriveDatabaseKey(masterPassword: CharArray, salt: ByteArray?): ByteArray {
        val actualSalt = salt ?: getOrGenerateSalt()
        val iterations = 600_000 // High iteration count for security
        
        val pbkdf2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(masterPassword, actualSalt, iterations, 256)
        val key = pbkdf2.generateSecret(spec)
        return key.encoded
    }

    override fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    override fun getOrGenerateSalt(): ByteArray {
        val storedSalt = securePrefs.getString(SALT_KEY, null)
        if (storedSalt != null) {
            return Base64.decode(storedSalt, Base64.DEFAULT)
        }
        
        val newSalt = generateSalt()
        securePrefs.edit()
            .putString(SALT_KEY, Base64.encodeToString(newSalt, Base64.DEFAULT))
            .apply()
        return newSalt
    }
}
