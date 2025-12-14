package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sathish.soundharajan.passwd.data.PasswordEntry
import com.sathish.soundharajan.passwd.security.PasswordGenerator
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import com.sathish.soundharajan.passwd.ui.components.GlassTextField

@Composable
fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var service by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // Validation state
    var serviceError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Add New Password",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                GlassTextField(
                    value = service,
                    onValueChange = {
                        service = it
                        serviceError = null
                    },
                    label = { Text("Service Name") },
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
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        modifier = Modifier.weight(1f),
                        isPassword = true,
                        isError = passwordError != null
                    )
                    IconButton(
                        onClick = { password = PasswordGenerator.generatePassword() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generate")
                    }
                }

                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                GlassTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Secure Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        onClick = {
                            // Validate required fields
                            serviceError = if (service.trim().isBlank()) "Service name is required" else null
                            passwordError = if (password.isBlank()) "Password is required" else null

                            // If validation passes, save the changes
                            if (serviceError == null && passwordError == null) {
                                onAdd(service.trim(), username.trim(), password, notes.trim(), tags.trim())
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun EditPasswordDialog(
    password: PasswordEntry,
    onDismiss: () -> Unit,
    onSave: (PasswordEntry) -> Unit
) {
    var service by remember { mutableStateOf(password.service) }
    var username by remember { mutableStateOf(password.username) }
    var passwordText by remember { mutableStateOf(password.password) }
    var notes by remember { mutableStateOf(password.notes) }
    var tags by remember { mutableStateOf(password.tags) }

    // Validation state
    var serviceError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Edit Password",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                GlassTextField(
                    value = service,
                    onValueChange = {
                        service = it
                        serviceError = null
                    },
                    label = { Text("Service Name") },
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
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(
                        value = passwordText,
                        onValueChange = {
                            passwordText = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        modifier = Modifier.weight(1f),
                        isPassword = true,
                        isError = passwordError != null
                    )
                    IconButton(
                        onClick = { passwordText = PasswordGenerator.generatePassword() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generate")
                    }
                }

                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                // Reorder: Tags first, then Notes (to match AddPasswordScreen layout)
                GlassTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Secure Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        onClick = {
                            // Validate required fields
                            serviceError = if (service.trim().isBlank()) "Service name is required" else null
                            passwordError = if (passwordText.isBlank()) "Password is required" else null

                            // If validation passes, save the changes
                            if (serviceError == null && passwordError == null) {
                                val updated = password.copy(
                                    service = service.trim(),
                                    username = username.trim(),
                                    password = passwordText,
                                    notes = notes.trim(),
                                    tags = tags.trim(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(updated)
                            }
                        }
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
