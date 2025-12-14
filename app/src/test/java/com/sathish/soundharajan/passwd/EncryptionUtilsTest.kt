package com.sathish.soundharajan.passwd

import com.sathish.soundharajan.passwd.security.EncryptionUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EncryptionUtilsTest {

    @Test
    fun `deriveDatabaseKey returns consistent results for same password`() {
        // Given
        val password = "testPassword123!"

        // When
        val key1 = EncryptionUtils.deriveDatabaseKey(password)
        val key2 = EncryptionUtils.deriveDatabaseKey(password)

        // Then
        assertNotNull("Database key should not be null", key1)
        assertNotNull("Database key should not be null", key2)
        assertArrayEquals("Same password should produce same key", key1, key2)
    }

    @Test
    fun `deriveDatabaseKey returns different results for different passwords`() {
        // Given
        val password1 = "password1"
        val password2 = "password2"

        // When
        val key1 = EncryptionUtils.deriveDatabaseKey(password1)
        val key2 = EncryptionUtils.deriveDatabaseKey(password2)

        // Then
        assertFalse("Different passwords should produce different keys",
            key1.contentEquals(key2))
    }

    @Test
    fun `deriveDatabaseKey returns 32 byte key`() {
        // Given
        val password = "testPassword"

        // When
        val key = EncryptionUtils.deriveDatabaseKey(password)

        // Then
        assertEquals("Database key should be 32 bytes (256 bits)", 32, key.size)
    }

    @Test
    fun `encodeToBase64 and decodeFromBase64 are reversible`() {
        // Given
        val originalData = "Hello, World!".toByteArray()

        // When
        val encoded = EncryptionUtils.encodeToBase64(originalData)
        val decoded = EncryptionUtils.decodeFromBase64(encoded)

        // Then
        assertArrayEquals("Base64 encode/decode should be reversible", originalData, decoded)
    }

    @Test
    fun `encodeToBase64 produces valid base64 string`() {
        // Given
        val data = byteArrayOf(1, 2, 3, 4, 5)

        // When
        val encoded = EncryptionUtils.encodeToBase64(data)

        // Then
        assertNotNull("Encoded string should not be null", encoded)
        assertTrue("Encoded string should not be empty", encoded.isNotEmpty())

        // Verify it's valid base64 by trying to decode it
        val decoded = EncryptionUtils.decodeFromBase64(encoded)
        assertArrayEquals("Should be able to decode the encoded string", data, decoded)
    }

    @Test
    fun `decodeFromBase64 handles empty string`() {
        // Given
        val emptyString = ""

        // When
        val decoded = EncryptionUtils.decodeFromBase64(emptyString)

        // Then
        assertNotNull("Decoded data should not be null", decoded)
        assertEquals("Empty string should decode to empty byte array", 0, decoded.size)
    }

    // Note: encrypt/decrypt tests are skipped because they require Android KeyStore
    // which is not fully supported in Robolectric. In a real testing environment,
    // these would be tested with instrumentation tests or mocked KeyStore.
}
