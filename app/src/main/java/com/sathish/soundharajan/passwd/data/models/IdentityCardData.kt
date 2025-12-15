package com.sathish.soundharajan.passwd.data.models

import kotlinx.serialization.Serializable

@Serializable
data class IdentityCardData(
    val fullName: String,
    val idNumber: String,            // Encrypted
    val documentType: String,        // Passport, Driver's License, etc.
    val issuingAuthority: String,
    val issueDate: Long = System.currentTimeMillis(),    // Timestamp
    val expirationDate: Long = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L), // 1 year from now
    val dateOfBirth: Long? = null,   // Optional for some documents
    val nationality: String = "",
    val address: String = "",
    val placeOfBirth: String = "",
    val gender: String = "",         // M, F, Other
    val height: String = "",         // For driver's licenses
    val eyeColor: String = "",       // For driver's licenses
    val bloodType: String = "",      // For medical IDs
    val organDonor: Boolean = false, // For driver's licenses
    val identifyingMarks: String = "",
    val emergencyContact: String = "",
    val additionalInfo: Map<String, String> = emptyMap() // Flexible additional fields
)
