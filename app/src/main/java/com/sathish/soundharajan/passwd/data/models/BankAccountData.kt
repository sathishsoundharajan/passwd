package com.sathish.soundharajan.passwd.data.models

import kotlinx.serialization.Serializable

@Serializable
data class BankAccountData(
    val bankName: String,
    val accountHolder: String,
    val accountNumber: String,        // Encrypted
    val routingNumber: String = "",   // Encrypted (US)
    val iban: String = "",            // Encrypted (International)
    val accountType: String = "Checking", // Checking, Savings, etc.
    val swiftCode: String = "",
    val branch: String = "",
    val pin: String = "",             // Encrypted
    val phoneNumber: String = "",
    val address: String = ""
)
