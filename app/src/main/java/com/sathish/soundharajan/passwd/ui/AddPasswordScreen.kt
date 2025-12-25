package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.security.PasswordGenerator
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.components.GlassTextField
import com.sathish.soundharajan.passwd.ui.components.PasswordStrengthIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(
    onBack: () -> Unit = {},
    onSave: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    var service by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState()
    
    // Clear error when screen is first displayed
    LaunchedEffect(Unit) {
        onClearError()
    }

    GlassScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "Add Entry",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = {
                        if (service.trim().isNotBlank() && username.trim().isNotBlank() && password.trim().isNotBlank() && !isSaving) {
                            isSaving = true
                            onSave(service.trim(), username.trim(), password.trim(), notes.trim(), tags.trim())
                        }
                    },
                    enabled = service.trim().isNotBlank() && username.trim().isNotBlank() && password.trim().isNotBlank() && !isSaving
                ) {
                    Text(if (isSaving) "SAVING..." else "SAVE", fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (errorMessage != null) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassTextField(
                            value = service,
                            onValueChange = { service = it },
                            label = { Text("Service Name *") },
                            leadingIcon = { Icon(Icons.Default.Language, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username / Email *") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            GlassTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password *") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                                modifier = Modifier.weight(1f)
                            )
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Password generator") } },
                                state = tooltipState
                            ) {
                                IconButton(onClick = { password = PasswordGenerator.generatePassword() }) {
                                    Icon(Icons.Default.Refresh, "Password generator")
                                }
                            }
                        }
                        
                        if (password.isNotEmpty()) {
                            PasswordStrengthIndicator(password = password)
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags (comma separated)") },
                            leadingIcon = { Icon(Icons.Default.Label, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Secure Notes") },
                            leadingIcon = { Icon(Icons.Default.Note, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
