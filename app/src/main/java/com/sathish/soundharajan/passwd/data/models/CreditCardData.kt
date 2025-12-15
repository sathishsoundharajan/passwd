package com.sathish.soundharajan.passwd.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CreditCardData(
    val cardholderName: String,
    val cardNumber: String,          // Encrypted (16 digits)
    val expirationMonth: Int = 1,    // 1-12
    val expirationYear: Int = 2025,  // 4-digit year
    val cvv: String = "",            // Encrypted (3-4 digits)
    val cardType: String = "Unknown", // Visa, Mastercard, Amex, etc.
    val issuingBank: String,
    val pin: String = "",            // Encrypted
    val billingAddress: String = "",
    val phoneNumber: String = "",
    val notes: String = ""
)
