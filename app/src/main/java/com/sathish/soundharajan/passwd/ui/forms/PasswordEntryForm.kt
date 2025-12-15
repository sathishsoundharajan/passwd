package com.sathish.soundharajan.passwd.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.VaultEntry
import com.sathish.soundharajan.passwd.data.models.PasswordData
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton

@Composable
fun PasswordEntryForm(
    viewModel: VaultViewModel,
    entry: VaultEntry? = null,
    isEdit: Boolean = false
) {
    val passwordData = entry?.let { viewModel.getPasswordData(it) }

    var title by remember { mutableStateOf(entry?.title ?: "") }
    var username by remember { mutableStateOf(passwordData?.username ?: "") }
    var password by remember { mutableStateOf(passwordData?.password ?: "") }
    var tags by remember { mutableStateOf(entry?.tags ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isEdit) "Edit Password Entry" else "Add Password Entry",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password *") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        GlassButton(
            onClick = {
                if (isEdit && entry != null) {
                    // Update existing entry
                    viewModel.updatePasswordEntry(
                        entryId = entry.id,
                        title = title,
                        username = username,
                        password = password,
                        tags = tags,
                        notes = notes
                    )
                } else {
                    // Add new entry
                    viewModel.addPasswordEntry(
                        title = title,
                        service = "",
                        username = username,
                        password = password,
                        url = "",
                        category = "",
                        tags = tags,
                        notes = notes
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEdit) "Update Password" else "Save Password")
                }
            }
        }
    }
}
