package com.sathish.soundharajan.passwd.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PasswordData(
    val service: String,
    val username: String,
    val password: String,
    val url: String = ""
)
