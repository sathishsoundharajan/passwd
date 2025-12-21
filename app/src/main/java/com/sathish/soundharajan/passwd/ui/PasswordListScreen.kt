package com.sathish.soundharajan.passwd.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.PasswordEntry
import com.sathish.soundharajan.passwd.presentation.PasswordViewModel
import com.sathish.soundharajan.passwd.ui.components.*

import com.sathish.soundharajan.passwd.ui.theme.ErrorRed
import com.sathish.soundharajan.passwd.ui.theme.Primary500
import com.sathish.soundharajan.passwd.ui.theme.PrimaryGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
        viewModel: PasswordViewModel,
        onLogout: () -> Unit = {},
        onNavigateToArchive: () -> Unit = {},
        onNavigateToAddPassword: () -> Unit = {},
        onNavigateToSettings: () -> Unit = {},
        onNavigateToEdit: (Long) -> Unit = {}
) {
    val passwords by viewModel.passwords.collectAsState()
    val groupedPasswords by viewModel.groupedPasswords.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val expandedTags by viewModel.expandedTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedPasswords by viewModel.selectedPasswords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMorePasswords by viewModel.hasMorePasswords.collectAsState()
    val hasMoreSearchResults by viewModel.hasMoreSearchResults.collectAsState()

    // showAddDialog removed, editingPassword removed
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<PasswordEntry?>(null) }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Pagination logic
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading) {
            if (searchQuery.isBlank() && hasMorePasswords) {
                viewModel.loadMorePasswords()
            } else if (searchQuery.isNotBlank() && hasMoreSearchResults) {
                viewModel.loadMoreSearchResults()
            }
        }
    }

    GlassScaffold(
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(
                            onClick = onNavigateToAddPassword,
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Box(
                                modifier = Modifier
                                        .background(PrimaryGradient)
                                        .size(56.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }
                }
            }
    ) { padding ->
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                                "${selectedPasswords.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        if (selectedPasswords.isNotEmpty()) {
                            IconButton(onClick = { viewModel.bulkArchiveSelected() }) {
                                Icon(Icons.Default.Archive, null, tint = Primary500)
                            }
                            IconButton(onClick = { showBulkDeleteConfirmation = true }) {
                                Icon(Icons.Default.Delete, null, tint = ErrorRed)
                            }
                        }
                    }
                } else {
                    Text(
                            "My Vault",
                            style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                    ),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                    if (viewMode == PasswordViewModel.ViewMode.FLAT) Icons.Default.List else Icons.Default.GridView,
                                    "Toggle View",
                                    tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                    Icons.Default.Settings,
                                    "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onNavigateToArchive) {
                            Icon(
                                    Icons.Default.Archive,
                                    "Archive",
                                    tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                        Icons.Default.MoreVert,
                                        "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier =
                                            Modifier.background(
                                                    MaterialTheme.colorScheme.surface.copy(
                                                            alpha = 0.9f
                                                    )
                                            )
                            ) {
                                DropdownMenuItem(
                                        text = { Text("Select Items") },
                                        onClick = {
                                            viewModel.toggleSelectionMode()
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                                )
                                DropdownMenuItem(
                                        text = { Text("Log Out") },
                                        onClick = {
                                            onLogout()
                                            showMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Lock, null) }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchPasswords(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Passwords") },
                        leadingIcon = {
                            Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        trailingIcon =
                                if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { viewModel.searchPasswords("") }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List
            if (passwords.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                if (searchQuery.isEmpty()) "No passwords yet.\nTap + to add one."
                                else "No matches found.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "💡 Archived passwords can be found in the Archive section",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    if (viewMode == PasswordViewModel.ViewMode.FLAT) {
                        // Flat view - show all passwords in a single list
                        items(passwords, key = { it.id }) { password ->
                            PasswordItemGlass(
                                    password = password,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedPasswords.contains(password.id),
                                    onClick = {
                                        if (isSelectionMode)
                                                viewModel.togglePasswordSelection(password.id)
                                        else onNavigateToEdit(password.id)
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.toggleSelectionMode()
                                            viewModel.togglePasswordSelection(password.id)
                                        }
                                    },
                                    onDelete = { showDeleteConfirmation = password },
                                    onArchive = { viewModel.archivePassword(password) }
                            )
                        }
                    } else {
                        // Grouped view - show passwords grouped by tags with expandable sections
                        groupedPasswords.forEach { (tag, tagPasswords) ->
                            item(key = "tag_header_$tag") {
                                val isExpanded = expandedTags[tag] ?: false
                                val tagPasswordIds = tagPasswords.map { it.id }.toSet()
                                val selectedCount = tagPasswordIds.count { selectedPasswords.contains(it) }
                                val isFullySelected = selectedCount == tagPasswordIds.size && tagPasswordIds.isNotEmpty()
                                val isPartiallySelected = selectedCount > 0 && selectedCount < tagPasswordIds.size

                                // Expandable Tag header
                                Surface(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.toggleTagExpansion(tag) },
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Expand/collapse icon
                                        Icon(
                                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Icon(
                                                Icons.Default.Label,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                                text = tag,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                        )

                                        // Bulk selection checkbox (only shown in selection mode)
                                        if (isSelectionMode) {
                                            TriStateCheckbox(
                                                    state = when {
                                                        isFullySelected -> ToggleableState.On
                                                        isPartiallySelected -> ToggleableState.Indeterminate
                                                        else -> ToggleableState.Off
                                                    },
                                                    onClick = {
                                                        viewModel.selectAllInTag(tag, !isFullySelected)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                            checkedColor = Primary500,
                                                            checkmarkColor = Color.White,
                                                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                            )
                                        } else {
                                            Text(
                                                    text = "${tagPasswords.size}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Passwords in this tag (only show if expanded)
                            if (expandedTags[tag] == true) {
                                items(tagPasswords, key = { it.id }) { password ->
                                    PasswordItemGlass(
                                            password = password,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = selectedPasswords.contains(password.id),
                                            onClick = {
                                                if (isSelectionMode)
                                                        viewModel.togglePasswordSelection(password.id)
                                                else onNavigateToEdit(password.id)
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    viewModel.toggleSelectionMode()
                                                    viewModel.togglePasswordSelection(password.id)
                                                }
                                            },
                                            onDelete = { showDeleteConfirmation = password },
                                            onArchive = { viewModel.archivePassword(password) }
                                    )
                                }

                                // Add spacing between groups
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(color = Primary500) }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialogs
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

    if (showBulkDeleteConfirmation) {
        BulkDeleteConfirmationDialog(
            count = selectedPasswords.size,
            onConfirm = {
                viewModel.bulkDeleteSelected()
                showBulkDeleteConfirmation = false
            },
            onDismiss = { showBulkDeleteConfirmation = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordItemGlass(
        password: PasswordEntry,
        isSelectionMode: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onDelete: () -> Unit,
        onArchive: () -> Unit,
        archiveActionText: String = "Archive",
        useDirectEdit: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }

    GlassCard(
            modifier =
                    Modifier.fillMaxWidth()
                            .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) onClick()
                                        else if (useDirectEdit) onClick()
                                        else showActions = !showActions
                                    },
                                    onLongClick = onLongClick
                            )
                            .let {
                                if (isSelected) it.background(Primary500.copy(alpha = 0.1f)) else it
                            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                ) {
                    // Avatar / Icon
                    Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                    text = password.service.firstOrNull()?.toString()?.uppercase()
                                                    ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                                text = password.service,
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        if (password.username.isNotBlank()) {
                            Text(
                                    text = password.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = CheckboxDefaults.colors(checkedColor = Primary500)
                    )
                } else {
                    IconButton(onClick = onClick) {
                        Icon(
                                Icons.Default.Edit,
                                "Edit",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Expandable actions
            AnimatedVisibility(visible = showActions && !isSelectionMode) {
                Column {
                    Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(
                                icon = Icons.Default.ContentCopy,
                                text = "User",
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(password.username))
                                    Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT)
                                            .show()
                                }
                        )
                        ActionButton(
                                icon = Icons.Default.VpnKey,
                                text = "Pass",
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(password.password))
                                    Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT)
                                            .show()
                                }
                        )
                        ActionButton(
                                icon = Icons.Default.Archive,
                                text = archiveActionText,
                                color = Primary500,
                                onClick = onArchive
                        )
                        ActionButton(
                                icon = Icons.Default.Delete,
                                text = "Delete",
                                color = ErrorRed,
                                onClick = onDelete
                        )
                    }

                    // Timestamp information
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Created: ${formatTimestamp(password.createdAt)} | Modified: ${formatTimestamp(password.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        color: Color = MaterialTheme.colorScheme.primary,
        onClick: () -> Unit
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// Helper function to format timestamps
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}
