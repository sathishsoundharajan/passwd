package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.VaultEntry
import com.sathish.soundharajan.passwd.data.VaultEntryType
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.forms.BankAccountEntryForm
import com.sathish.soundharajan.passwd.ui.forms.PasswordEntryForm
import com.sathish.soundharajan.passwd.ui.theme.PasswdTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVaultEntryScreen(
    entryId: Long,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onEntryUpdated: () -> Unit = {}
) {
    var entry by remember { mutableStateOf<VaultEntry?>(null) }
    val error by viewModel.error.collectAsState()

    // Load entry when composable is first created
    LaunchedEffect(entryId) {
        entry = viewModel.getVaultEntryById(entryId)
    }

    // Handle successful entry update
    LaunchedEffect(error) {
        val currentError = error
        if (currentError != null && currentError.contains("successfully")) {
            onEntryUpdated()
        }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Entry",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Error display
            val currentError = error
            if (currentError != null && !currentError.contains("successfully")) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Show edit form based on entry type
            entry?.let { vaultEntry ->
                when (vaultEntry.type) {
                    VaultEntryType.PASSWORD -> {
                        PasswordEntryForm(viewModel = viewModel, entry = vaultEntry, isEdit = true)
                    }
                    VaultEntryType.BANK_ACCOUNT -> {
                        BankAccountEntryForm(viewModel = viewModel, entry = vaultEntry, isEdit = true)
                    }
                    else -> {
                        // For other types, show a message for now
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Edit functionality for ${vaultEntry.type} is not yet implemented",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            } ?: run {
                // Entry not found
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Entry not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
