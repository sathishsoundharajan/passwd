package com.sathish.soundharajan.passwd.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.ui.theme.*

@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(BackgroundDarkStart, BackgroundDarkEnd))
    } else {
        Brush.verticalGradient(listOf(BackgroundLightStart, BackgroundLightEnd))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Ambient background blobs for "depth"
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Primary500.copy(alpha = 0.15f), Color.Transparent),
                            radius = 1200f
                        )
                    )
            )
        }

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent, // Transparent to show gradient
            topBar = topBar,
            floatingActionButton = floatingActionButton,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassColor = backgroundColor ?: (if (isDark) GlassDark else GlassLight)
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight

    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, borderColor, shape),
        color = glassColor,
        shape = shape
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
            contentColor = Color.White, // Force white text for better contrast
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(PrimaryGradient)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .then(modifier), // allow external sizing
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    minLines: Int = 1
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0x30000000) else Color(0x30FFFFFF)
    val textColor = if (isDark) TextWhite else TextBlack

    var passwordVisible by remember { mutableStateOf(false) }

    val actualVisualTransformation = when {
        isPassword && !passwordVisible && visualTransformation == androidx.compose.ui.text.input.VisualTransformation.None ->
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        else -> visualTransformation
    }

    val actualTrailingIcon = if (isPassword && trailingIcon == null) {
        {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        trailingIcon
    }

    val actualSingleLine = if (minLines > 1) false else singleLine

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = actualTrailingIcon,
        isError = isError,
        visualTransformation = actualVisualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = actualSingleLine,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedBorderColor = Primary500,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = ErrorRed,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
        )
    )
}
