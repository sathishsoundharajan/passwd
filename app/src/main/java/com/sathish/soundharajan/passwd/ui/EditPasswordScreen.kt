package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.PasswordEntry
import com.sathish.soundharajan.passwd.presentation.PasswordViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.components.GlassTextField
import com.sathish.soundharajan.passwd.ui.components.PasswordStrengthIndicator
import com.sathish.soundharajan.passwd.ui.theme.AccentCyan
import com.sathish.soundharajan.passwd.ui.theme.PrimaryGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPasswordScreen(
    passwordId: Long,
    viewModel: PasswordViewModel,
    onBack: () -> Unit,
    errorMessage: String? = null
) {
    val passwords by viewModel.passwords.collectAsState()
    val archivedPasswords by viewModel.archivedPasswords.collectAsState()
    
    // Find the password in either active or archived list
    val originalPassword = remember(passwordId, passwords, archivedPasswords) {
        passwords.find { it.id == passwordId } 
            ?: archivedPasswords.find { it.id == passwordId }
    }

    // State for inputs
    var service by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // Validation state
    var serviceError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Initialize state when originalPassword is found
    LaunchedEffect(originalPassword) {
        originalPassword?.let {
            service = it.service
            username = it.username
            password = it.password
            notes = it.notes
            tags = it.tags
        }
    }

    GlassScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Password", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (originalPassword == null) {
             Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 CircularProgressIndicator()
             }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassTextField(
                            value = service,
                            onValueChange = {
                                service = it
                                serviceError = null
                            },
                            label = { Text("Service (e.g., Netflix)") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = serviceError != null
                        )

                        if (serviceError != null) {
                            Text(
                                text = serviceError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        GlassTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                usernameError = null
                            },
                            label = { Text("Username / Email") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = usernameError != null
                        )

                        if (usernameError != null) {
                            Text(
                                text = usernameError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        GlassTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                            },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            isPassword = true,
                            isError = passwordError != null
                        )

                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                        
                        PasswordStrengthIndicator(password)

                        GlassTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }

                if (errorMessage != null) {
                    GlassCard(backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)) {
                         Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                GlassButton(
                    onClick = {
                        // Validate required fields
                        serviceError = if (service.trim().isBlank()) "Service name is required" else null
                        usernameError = if (username.trim().isBlank()) "Username is required" else null
                        passwordError = if (password.trim().isBlank()) "Password is required" else null

                        // If validation passes, save the changes
                        if (serviceError == null && usernameError == null && passwordError == null) {
                            val updatedEntry = originalPassword!!.copy(
                                service = service.trim(),
                                username = username.trim(),
                                password = password,
                                notes = notes.trim(),
                                tags = tags.trim()
                            )
                            viewModel.updatePassword(updatedEntry)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Changes", color = Color.White)
                    }
                }
            }
        }
    }
}
