package com.sathish.soundharajan.passwd

import com.sathish.soundharajan.passwd.security.PasswordGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasswordGeneratorTest {

    @Test
    fun `generatePassword returns correct length`() {
        // Given
        val length = 12

        // When
        val password = PasswordGenerator.generatePassword(length = length)

        // Then
        assertEquals("Password should be correct length", length, password.length)
    }

    @Test
    fun `generatePassword includes lowercase by default`() {
        // When
        val password = PasswordGenerator.generatePassword(length = 100)

        // Then
        assertTrue("Password should contain lowercase letters",
            password.any { it.isLowerCase() })
    }

    @Test
    fun `generatePassword includes uppercase when enabled`() {
        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeUppercase = true
        )

        // Then
        assertTrue("Password should contain uppercase letters",
            password.any { it.isUpperCase() })
    }

    @Test
    fun `generatePassword excludes uppercase when disabled`() {
        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeUppercase = false
        )

        // Then
        assertFalse("Password should not contain uppercase letters",
            password.any { it.isUpperCase() })
    }

    @Test
    fun `generatePassword includes digits when enabled`() {
        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeDigits = true
        )

        // Then
        assertTrue("Password should contain digits",
            password.any { it.isDigit() })
    }

    @Test
    fun `generatePassword excludes digits when disabled`() {
        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeDigits = false
        )

        // Then
        assertFalse("Password should not contain digits",
            password.any { it.isDigit() })
    }

    @Test
    fun `generatePassword includes symbols when enabled`() {
        // Given
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeSymbols = true
        )

        // Then
        assertTrue("Password should contain symbols",
            password.any { it in symbols })
    }

    @Test
    fun `generatePassword excludes symbols when disabled`() {
        // Given
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        // When
        val password = PasswordGenerator.generatePassword(
            length = 100,
            includeSymbols = false
        )

        // Then
        assertFalse("Password should not contain symbols",
            password.any { it in symbols })
    }

    @Test
    fun `generatePassword with all options disabled only uses lowercase`() {
        // When
        val password = PasswordGenerator.generatePassword(
            length = 10,
            includeUppercase = false,
            includeDigits = false,
            includeSymbols = false
        )

        // Then
        assertTrue("Password should only contain lowercase letters",
            password.all { it.isLowerCase() })
    }

    @Test
    fun `generatePassword produces different results on multiple calls`() {
        // When
        val password1 = PasswordGenerator.generatePassword(length = 16)
        val password2 = PasswordGenerator.generatePassword(length = 16)

        // Then
        // Note: There's a small chance this could fail due to randomness,
        // but with length 16 and large character pool, it's extremely unlikely
        assertNotEquals("Passwords should be different", password1, password2)
    }

    @Test
    fun `generatePassword default parameters work correctly`() {
        // When
        val password = PasswordGenerator.generatePassword()

        // Then
        assertEquals("Default length should be 16", 16, password.length)
        assertTrue("Should contain uppercase", password.any { it.isUpperCase() })
        assertTrue("Should contain digits", password.any { it.isDigit() })
        assertTrue("Should contain symbols", password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) })
    }
}
