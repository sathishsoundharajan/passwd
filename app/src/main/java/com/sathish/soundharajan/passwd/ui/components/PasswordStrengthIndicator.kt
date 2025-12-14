package com.sathish.soundharajan.passwd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.ui.theme.ErrorRed
import com.sathish.soundharajan.passwd.ui.theme.SuccessGreen
import com.sathish.soundharajan.passwd.ui.theme.WarningAmber

@Composable
fun PasswordStrengthIndicator(password: String) {
    val strength =
            when {
                password.length < 8 -> 0.2f
                password.length < 12 -> 0.4f
                password.contains(Regex("[A-Z]")) &&
                        password.contains(Regex("[a-z]")) &&
                        password.contains(Regex("[0-9]")) &&
                        password.contains(Regex("[^A-Za-z0-9]")) -> 1.0f
                password.contains(Regex("[A-Z]")) &&
                        password.contains(Regex("[a-z]")) &&
                        password.contains(Regex("[0-9]")) -> 0.8f
                password.contains(Regex("[A-Z]")) && password.contains(Regex("[a-z]")) -> 0.6f
                else -> 0.4f
            }

    val color =
            when {
                strength >= 0.8f -> SuccessGreen
                strength >= 0.6f -> WarningAmber
                else -> ErrorRed
            }

    Column {
        LinearProgressIndicator(
                progress = strength,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
                text =
                        when {
                            strength >= 0.8f -> "Strong"
                            strength >= 0.6f -> "Good"
                            strength >= 0.4f -> "Fair"
                            else -> "Weak"
                        },
                style = MaterialTheme.typography.bodySmall,
                color = color,
                modifier = Modifier.padding(top = 4.dp)
        )
    }
}
