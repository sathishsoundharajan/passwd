package com.sathish.soundharajan.passwd

import com.sathish.soundharajan.passwd.security.AuthManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AuthManagerTest {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        AuthManager.init(context)
    }

    @Test
    fun `master password is not set initially`() {
        // Given - fresh state
        // When
        val isSet = AuthManager.isMasterPasswordSet()

        // Then
        assertFalse("Master password should not be set initially", isSet)
    }

    @Test
    fun `set master password makes it set`() {
        // Given
        val password = "testPassword123!"

        // When
        AuthManager.setMasterPassword(password)
        val isSet = AuthManager.isMasterPasswordSet()

        // Then
        assertTrue("Master password should be set after setting", isSet)
    }

    @Test
    fun `verify master password returns true for correct password`() {
        // Given
        val password = "correctPassword123!"
        AuthManager.setMasterPassword(password)

        // When
        val isValid = AuthManager.verifyMasterPassword(password)

        // Then
        assertTrue("Should verify correct password", isValid)
    }

    @Test
    fun `verify master password returns false for incorrect password`() {
        // Given
        val correctPassword = "correctPassword123!"
        val wrongPassword = "wrongPassword123!"
        AuthManager.setMasterPassword(correctPassword)

        // When
        val isValid = AuthManager.verifyMasterPassword(wrongPassword)

        // Then
        assertFalse("Should reject incorrect password", isValid)
    }

    @Test
    fun `biometric is not enabled initially`() {
        // Given - fresh state
        // When
        val isEnabled = AuthManager.isBiometricEnabled()

        // Then
        assertFalse("Biometric should not be enabled initially", isEnabled)
    }

    @Test
    fun `set biometric enabled makes it enabled`() {
        // Given
        // When
        AuthManager.setBiometricEnabled(true)
        val isEnabled = AuthManager.isBiometricEnabled()

        // Then
        assertTrue("Biometric should be enabled after setting", isEnabled)
    }

    @Test
    fun `get database key returns non-empty byte array`() {
        // Given
        val password = "testPassword123!"

        // When
        val key = AuthManager.getDatabaseKey(password)

        // Then
        assertNotNull("Database key should not be null", key)
        assertTrue("Database key should not be empty", key.isNotEmpty())
    }

    @Test
    fun `get database key is deterministic for same password`() {
        // Given
        val password = "testPassword123!"

        // When
        val key1 = AuthManager.getDatabaseKey(password)
        val key2 = AuthManager.getDatabaseKey(password)

        // Then
        assertArrayEquals("Same password should produce same key", key1, key2)
    }
}
