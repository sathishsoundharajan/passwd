package com.sathish.soundharajan.passwd.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.data.VaultEntryType
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import com.sathish.soundharajan.passwd.ui.components.GlassScaffold
import com.sathish.soundharajan.passwd.ui.forms.*
import com.sathish.soundharajan.passwd.ui.theme.AccentCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaultEntryScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onEntryAdded: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf<VaultEntryType?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()

    // Reset error when type changes
    LaunchedEffect(selectedType) {
        if (selectedType != null) {
            viewModel.clearError()
        }
    }

    // Handle successful entry addition
    LaunchedEffect(error) {
        val currentError = error
        if (currentError != null && currentError.contains("successfully")) {
            onEntryAdded()
        }
    }

    GlassScaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (showForm) {
                        showForm = false
                        selectedType = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showForm) "Add ${selectedType?.name?.replace("_", " ")?.lowercase()?.capitalize()}" else "Add Entry",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Error display
            val currentError = error
            if (currentError != null && !currentError.contains("successfully")) {
                GlassCard(
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
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

            if (!showForm) {
                // Entry Type Selection
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Choose Entry Type",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    EntryTypeCard(
                        icon = Icons.Default.Lock,
                        title = "Password",
                        description = "Website login, app credentials, etc.",
                        onClick = {
                            selectedType = VaultEntryType.PASSWORD
                            showForm = true
                        }
                    )

                    EntryTypeCard(
                        icon = Icons.Default.AccountBalance,
                        title = "Bank Account",
                        description = "Checking, savings, and other bank accounts",
                        onClick = {
                            selectedType = VaultEntryType.BANK_ACCOUNT
                            showForm = true
                        }
                    )

                    EntryTypeCard(
                        icon = Icons.Default.CreditCard,
                        title = "Credit Card",
                        description = "Payment cards and financial information",
                        onClick = {
                            selectedType = VaultEntryType.CREDIT_CARD
                            showForm = true
                        }
                    )

                    EntryTypeCard(
                        icon = Icons.Default.Badge,
                        title = "Identity Card",
                        description = "Passports, driver's licenses, and documents",
                        onClick = {
                            selectedType = VaultEntryType.IDENTITY_CARD
                            showForm = true
                        }
                    )
                }
            } else {
                // Show form based on selected type
                when (selectedType) {
                    VaultEntryType.PASSWORD -> PasswordEntryForm(viewModel)
                    VaultEntryType.BANK_ACCOUNT -> BankAccountEntryForm(viewModel)
                    VaultEntryType.CREDIT_CARD -> CreditCardEntryForm(viewModel)
                    VaultEntryType.IDENTITY_CARD -> IdentityCardEntryForm(viewModel)
                    null -> {}
                }
            }
        }
    }
}

@Composable
fun EntryTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
