package com.sathish.soundharajan.passwd.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.*
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.*
import com.sathish.soundharajan.passwd.ui.theme.AccentCyan
import com.sathish.soundharajan.passwd.ui.theme.ErrorRed
import com.sathish.soundharajan.passwd.ui.theme.Primary500
import com.sathish.soundharajan.passwd.ui.theme.PrimaryGradient

// Using ActionButton from PasswordListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    viewModel: VaultViewModel,
    onLogout: () -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    onNavigateToAddEntry: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEdit: (VaultEntry) -> Unit = {}
) {
    val vaultEntries by viewModel.vaultEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilterType by viewModel.selectedFilterType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMoreEntries by viewModel.hasMoreEntries.collectAsState()
    val hasMoreSearchResults by viewModel.hasMoreSearchResults.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<VaultEntry?>(null) }
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
            if (searchQuery.isBlank() && hasMoreEntries) {
                viewModel.loadMoreEntries()
            } else if (searchQuery.isNotBlank() && hasMoreSearchResults) {
                viewModel.loadMoreSearchResults()
            }
        }
    }

    GlassScaffold(
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddEntry,
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
                            "${selectedEntries.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        if (selectedEntries.isNotEmpty()) {
                            IconButton(onClick = { viewModel.bulkArchiveSelected() }) {
                                Icon(Icons.Default.Archive, null, tint = AccentCyan)
                            }
                            IconButton(onClick = { showBulkDeleteConfirmation = true }) {
                                Icon(Icons.Default.Delete, null, tint = ErrorRed)
                            }
                        }
                    }
                } else {
                    Text(
                        "My Vault",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
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
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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

            // Filter Tabs
            if (!isSelectionMode) {
                ScrollableTabRow(
                    selectedTabIndex = when (selectedFilterType) {
                        null -> 0
                        VaultEntryType.PASSWORD -> 1
                        VaultEntryType.BANK_ACCOUNT -> 2
                        VaultEntryType.CREDIT_CARD -> 3
                        VaultEntryType.IDENTITY_CARD -> 4
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedFilterType == null,
                        onClick = { viewModel.filterByType(null) },
                        text = { Text("All") }
                    )
                    Tab(
                        selected = selectedFilterType == VaultEntryType.PASSWORD,
                        onClick = { viewModel.filterByType(VaultEntryType.PASSWORD) },
                        text = { Text("Passwords") }
                    )
                    Tab(
                        selected = selectedFilterType == VaultEntryType.BANK_ACCOUNT,
                        onClick = { viewModel.filterByType(VaultEntryType.BANK_ACCOUNT) },
                        text = { Text("Bank") }
                    )
                    Tab(
                        selected = selectedFilterType == VaultEntryType.CREDIT_CARD,
                        onClick = { viewModel.filterByType(VaultEntryType.CREDIT_CARD) },
                        text = { Text("Cards") }
                    )
                    Tab(
                        selected = selectedFilterType == VaultEntryType.IDENTITY_CARD,
                        onClick = { viewModel.filterByType(VaultEntryType.IDENTITY_CARD) },
                        text = { Text("Identity") }
                    )
                }
            }

            // Category Filter Chips
            if (!isSelectionMode && availableCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("All Categories") },
                        leadingIcon = if (selectedCategory == null) {
                            { Icon(Icons.Default.Check, null) }
                        } else null
                    )
                    availableCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.filterByCategory(category) },
                            label = { Text(category) },
                            leadingIcon = if (selectedCategory == category) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchEntries(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search Vault") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.searchEntries("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    } else null
                )
            }

            // List
            if (vaultEntries.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (searchQuery.isEmpty()) "No entries yet.\nTap + to add one."
                            else "No matches found.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "💡 Archived entries can be found in the Archive section",
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
                    items(vaultEntries, key = { it.id }) { entry ->
                        VaultEntryItemGlass(
                            entry = entry,
                            viewModel = viewModel,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedEntries.contains(entry.id),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleEntrySelection(entry.id)
                                } else {
                                    onNavigateToEdit(entry)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleEntrySelection(entry.id)
                                }
                            },
                            onDelete = { showDeleteConfirmation = entry },
                            onArchive = { viewModel.archiveEntry(entry) }
                        )
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
    showDeleteConfirmation?.let { entry ->
        DeleteVaultEntryConfirmationDialog(
            entryTitle = entry.title,
            entryType = entry.type,
            onConfirm = {
                viewModel.softDeleteEntry(entry)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }

    if (showBulkDeleteConfirmation) {
        BulkDeleteConfirmationDialog(
            count = selectedEntries.size,
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
fun VaultEntryItemGlass(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    archiveActionText: String = "Archive"
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }

    // Get type-specific data and display info
    val (icon, title, subtitle, copyActions) = getEntryDisplayInfo(entry, viewModel)

    GlassCard(
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onClick()
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
                    // Type-specific icon
                    Surface(
                        shape = CircleShape,
                        color = getEntryTypeColor(entry.type).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = getEntryTypeColor(entry.type),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Type indicator badge
                            Surface(
                                shape = CircleShape,
                                color = getEntryTypeColor(entry.type).copy(alpha = 0.1f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = getEntryTypeShortName(entry.type),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getEntryTypeColor(entry.type),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Show document count for identity cards
                        if (entry.type == VaultEntryType.IDENTITY_CARD && entry.documentPaths.isNotBlank()) {
                            val docCount = entry.documentPaths.split(",").size
                            Text(
                                text = "$docCount document${if (docCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentCyan,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
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
                        copyActions.forEach { (label, text) ->
                            ActionButton(
                                icon = Icons.Default.ContentCopy,
                                text = label,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        ActionButton(
                            icon = Icons.Default.Archive,
                            text = archiveActionText,
                            onClick = onArchive
                        )
                        ActionButton(
                            icon = Icons.Default.Delete,
                            text = "Delete",
                            color = ErrorRed,
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getEntryDisplayInfo(
    entry: VaultEntry,
    viewModel: VaultViewModel
): Quadruple<androidx.compose.ui.graphics.vector.ImageVector, String, String, List<Pair<String, String>>> {
    return when (entry.type) {
        VaultEntryType.PASSWORD -> {
            val data = viewModel.getPasswordData(entry)
            val icon = Icons.Default.Lock
            val title = entry.title
            val subtitle = data?.service ?: ""
            val copyActions = listOf(
                "User" to (data?.username ?: ""),
                "Pass" to (data?.password ?: "")
            )
            Quadruple(icon, title, subtitle, copyActions)
        }
        VaultEntryType.BANK_ACCOUNT -> {
            val data = viewModel.getBankAccountData(entry)
            val icon = Icons.Default.AccountBalance
            val title = entry.title
            val subtitle = data?.bankName ?: ""
            val copyActions = listOf(
                "Account" to (data?.accountNumber ?: ""),
                "Routing" to (data?.routingNumber ?: "")
            )
            Quadruple(icon, title, subtitle, copyActions)
        }
        VaultEntryType.CREDIT_CARD -> {
            val data = viewModel.getCreditCardData(entry)
            val icon = Icons.Default.CreditCard
            val title = entry.title
            val subtitle = data?.issuingBank ?: ""
            val copyActions = listOf(
                "Number" to (data?.cardNumber ?: ""),
                "CVV" to (data?.cvv ?: "")
            )
            Quadruple(icon, title, subtitle, copyActions)
        }
        VaultEntryType.IDENTITY_CARD -> {
            val data = viewModel.getIdentityCardData(entry)
            val icon = Icons.Default.Badge
            val title = entry.title
            val subtitle = data?.documentType ?: ""
            val copyActions = listOf(
                "ID" to (data?.idNumber ?: ""),
                "Name" to (data?.fullName ?: "")
            )
            Quadruple(icon, title, subtitle, copyActions)
        }
    }
}

private fun getEntryTypeColor(type: VaultEntryType): Color {
    return when (type) {
        VaultEntryType.PASSWORD -> AccentCyan
        VaultEntryType.BANK_ACCOUNT -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        VaultEntryType.CREDIT_CARD -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        VaultEntryType.IDENTITY_CARD -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
    }
}

private fun getEntryTypeShortName(type: VaultEntryType): String {
    return when (type) {
        VaultEntryType.PASSWORD -> "PWD"
        VaultEntryType.BANK_ACCOUNT -> "BANK"
        VaultEntryType.CREDIT_CARD -> "CARD"
        VaultEntryType.IDENTITY_CARD -> "ID"
    }
}

@Composable
fun DeleteVaultEntryConfirmationDialog(
    entryTitle: String,
    entryType: VaultEntryType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Entry") },
        text = {
            Text(
                "Are you sure you want to delete \"$entryTitle\"? " +
                "This will move it to Recently Deleted where you can restore it within 30 days."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BulkDeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Selected Entries") },
        text = {
            Text(
                "Are you sure you want to delete $count selected entries? " +
                "This will move them to Recently Deleted where you can restore them within 30 days."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper data class for multiple return values
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
