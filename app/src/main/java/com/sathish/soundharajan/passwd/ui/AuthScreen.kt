package com.sathish.soundharajan.passwd.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sathish.soundharajan.passwd.presentation.AuthState
import com.sathish.soundharajan.passwd.presentation.AuthViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.components.GlassTextField

import com.sathish.soundharajan.passwd.ui.theme.Primary500
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()

    GlassScaffold {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (authState) {
                is AuthState.Checking -> {
                    CircularProgressIndicator(color = Primary500)
                }
                is AuthState.SetupRequired -> {
                    SetupPasswordScreen(
                        onSetupComplete = { password ->
                            viewModel.setupMasterPassword(password)
                            onAuthenticated()
                        }
                    )
                }
                is AuthState.LoginRequired -> {
                    LoginScreen(
                        onLogin = { password ->
                            val success = viewModel.login(password)
                            if (success) onAuthenticated()
                            success
                        },
                        onBiometricLogin = { activity ->
                            viewModel.biometricLogin(activity)
                        },
                        isBiometricAvailable = viewModel.isBiometricAvailable(),
                        isBiometricEnabled = viewModel.isBiometricEnabled()
                    )
                }
                is AuthState.Authenticated -> {
                    LaunchedEffect(Unit) { onAuthenticated() }
                }
            }
        }
    }
}

@Composable
fun SetupPasswordScreen(onSetupComplete: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    GlassCard(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection()
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure Setup",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Create a master password to encrypt your vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            GlassButton(
                onClick = {
                    when {
                        password.length < 8 -> error = "Password must be at least 8 characters"
                        password != confirmPassword -> error = "Passwords do not match"
                        else -> onSetupComplete(password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CREATE VAULT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String) -> Boolean,
    onBiometricLogin: suspend (FragmentActivity) -> Boolean,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    
    // Auto-trigger biometric only once
    LaunchedEffect(Unit) {
        if (isBiometricAvailable && isBiometricEnabled) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                val success = onBiometricLogin(activity)
                if (!success) {
                  // silent fail, let user type password
                }
            }
        }
    }

    GlassCard(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Enter your master password to unlock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                onClick = {
                    if (password.isBlank()) {
                        error = "Password required"
                        return@GlassButton
                    }
                    isAuthenticating = true
                    if (!onLogin(password)) {
                        error = "Invalid password"
                        isAuthenticating = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("UNLOCK", fontWeight = FontWeight.Bold)
                }
            }
            
            if (isBiometricAvailable && isBiometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = {
                         coroutineScope.launch {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                if (!onBiometricLogin(activity)) {
                                    error = "Biometric failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Fingerprint, 
                        contentDescription = "Biometric", 
                        tint = Primary500,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogoSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp)
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .alpha(0.3f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Primary500, Color.Transparent)
                    )
                )
        )
        // Icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
