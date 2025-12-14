package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.PasswordEntry
import com.sathish.soundharajan.passwd.presentation.PasswordViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.components.DeletePasswordConfirmationDialog
import com.sathish.soundharajan.passwd.ui.theme.Primary500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: PasswordViewModel,
    onBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val archivedPasswords by viewModel.archivedPasswords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf<PasswordEntry?>(null) }
    val listState = rememberLazyListState()

    // Ensure archived passwords are loaded when screen opens
    LaunchedEffect(Unit) {
        // Reset archived page counter to ensure we load from the beginning
        viewModel.loadArchivedPasswordsPaged(resetPage = true)
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Archived Passwords",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (archivedPasswords.isEmpty() && !isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No archived passwords.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archivedPasswords) { password ->
                        PasswordItemGlass(
                            password = password,
                            isSelectionMode = false,
                            isSelected = false,
                            onClick = { onNavigateToEdit(password.id) },
                            onLongClick = { }, // No action on archive
                            onDelete = { showDeleteConfirmation = password },
                            onArchive = {
                                viewModel.archivePassword(password, archived = false) // Unarchive
                            },
                            archiveActionText = "Unarchive"
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog
    showDeleteConfirmation?.let { password ->
        DeletePasswordConfirmationDialog(
            passwordService = password.service,
            onConfirm = {
                viewModel.softDeletePassword(password)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }
}
