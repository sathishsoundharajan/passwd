package com.sathish.soundharajan.passwd.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aegis Logo Component
 *
 * Displays the Aegis secure vault logo with optional text
 *
 * Symbolism:
 * - Lock: Security and access control
 * - Protection and encryption
 * - Primary blue theme: Trust, security, and professionalism
 */
@Composable
fun AegisLogo(
    size: Dp = 48.dp, // Increased size to match Font C
    showText: Boolean = true,
    text: String = "Aegis",
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold
    ),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Aegis Logo",
            tint = Color(0xFF2D68FF), // Primary Blue
            modifier = Modifier.size(size)
        )

        if (showText) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = textStyle,
                color = Color(0xFF2D68FF) // Primary Blue to match logo
            )
        }
    }
}

/**
 * Compact version for smaller spaces
 */
@Composable
fun AegisLogoCompact(
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    AegisLogo(
        size = size,
        showText = false,
        modifier = modifier
    )
}

/**
 * Inline version for use in text
 */
@Composable
fun AegisLogoInline(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    AegisLogo(
        size = size,
        showText = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier
    )
}
