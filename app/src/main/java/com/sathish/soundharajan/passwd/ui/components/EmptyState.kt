package com.sathish.soundharajan.passwd.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Comprehensive empty state components for different scenarios
 */

@Composable
fun EmptyPasswordListState(
    onAddPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No passwords yet",
        description = "Start building your secure password collection. Add your first password to get organized and stay safe online.",
        primaryAction = {
            Button(onClick = onAddPassword) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Your First Password")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptySearchResultsState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Search,
        title = "No results found",
        description = "We couldn't find any passwords matching \"$searchQuery\". Try adjusting your search terms or check your spelling.",
        primaryAction = {
            OutlinedButton(onClick = onClearSearch) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Search")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyArchivedPasswordsState(
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No archived passwords",
        description = "Archived passwords will appear here. Archive passwords you no longer use but want to keep for reference.",
        primaryAction = {
            OutlinedButton(onClick = onBackToMain) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Passwords")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyExportState(
    onAddPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "Nothing to export",
        description = "Add some passwords first before you can export them. Your password data will be securely encrypted during export.",
        primaryAction = {
            Button(onClick = onAddPassword) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Password")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyImportState(
    onBrowseFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "Import passwords",
        description = "Import your password collection from a previously exported file. Files are encrypted and require your master password.",
        primaryAction = {
            Button(onClick = onBrowseFiles) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Files")
            }
        },
        secondaryAction = {
            Text(
                text = "Supported formats: JSON, CSV",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
    )
}

@Composable
fun EmptyUndoHistoryState(
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No actions to undo",
        description = "Your recent actions will appear here. Use undo to quickly reverse changes like deleted or modified passwords.",
        modifier = modifier
    )
}

@Composable
fun EmptyStateTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    primaryAction: @Composable (() -> Unit)? = null,
    secondaryAction: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(24.dp)
                    .size(72.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Primary Action
        primaryAction?.invoke()

        Spacer(modifier = Modifier.height(16.dp))

        // Secondary Action
        secondaryAction?.invoke()

        // Tips section for some empty states
        when (icon) {
            Icons.Default.Lock -> {
                Spacer(modifier = Modifier.height(48.dp))
                SecurityTips()
            }
            Icons.Default.Search -> {
                Spacer(modifier = Modifier.height(48.dp))
                SearchTips()
            }
        }
    }
}

@Composable
private fun SecurityTips() {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text(
            text = "Security Tips:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TipItem("Use unique passwords for each service")
        TipItem("Enable two-factor authentication when available")
        TipItem("Change passwords regularly for important accounts")
        TipItem("Use a password manager like this one!")
    }
}

@Composable
private fun SearchTips() {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text(
            text = "Search Tips:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TipItem("Search by service name (e.g., 'Google', 'GitHub')")
        TipItem("Search by username or email")
        TipItem("Search by tags or notes")
        TipItem("Use partial matches for broader results")
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Specialized empty states for specific scenarios

@Composable
fun EmptyPasswordSelectionState(
    onCancelSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No passwords selected",
        description = "Select passwords to perform bulk operations like archiving or deleting multiple items at once.",
        primaryAction = {
            OutlinedButton(onClick = onCancelSelection) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Selection")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyFilteredResultsState(
    filterType: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No $filterType passwords",
        description = "Try adjusting your filters or clear them to see all your passwords.",
        primaryAction = {
            OutlinedButton(onClick = onClearFilters) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Filters")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyRecentPasswordsState(
    onViewAllPasswords: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Lock,
        title = "No recent activity",
        description = "Your recently accessed or modified passwords will appear here for quick access.",
        primaryAction = {
            OutlinedButton(onClick = onViewAllPasswords) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View All Passwords")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EmptyFavoritesState(
    onBrowsePasswords: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        icon = Icons.Default.Star,
        title = "No favorite passwords",
        description = "Mark important passwords as favorites for quick access. Star passwords you use frequently.",
        primaryAction = {
            OutlinedButton(onClick = onBrowsePasswords) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Passwords")
            }
        },
        modifier = modifier
    )
}
