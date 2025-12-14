package com.sathish.soundharajan.passwd.security

import kotlin.random.Random

object PasswordGenerator {

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val charPool = buildString {
            append(LOWERCASE)
            if (includeUppercase) append(UPPERCASE)
            if (includeDigits) append(DIGITS)
            if (includeSymbols) append(SYMBOLS)
        }

        return (1..length)
            .map { charPool[Random.nextInt(charPool.length)] }
            .joinToString("")
    }
}
