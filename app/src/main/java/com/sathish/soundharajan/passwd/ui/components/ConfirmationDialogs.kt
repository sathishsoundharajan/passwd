package com.sathish.soundharajan.passwd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Comprehensive confirmation dialogs for destructive actions
 */

enum class ConfirmationLevel {
    LOW,      // Simple actions (archive single item)
    MEDIUM,   // Moderate impact (delete single item)
    HIGH,     // High impact (bulk operations, data loss)
    CRITICAL  // Irreversible actions (database reset)
}

@Composable
fun DeletePasswordConfirmationDialog(
    passwordService: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete Password",
        message = "Are you sure you want to permanently delete the password for \"$passwordService\"? This action cannot be undone.",
        confirmText = "Delete",
        confirmButtonColor = MaterialTheme.colorScheme.error,
        level = ConfirmationLevel.MEDIUM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun BulkDeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Delete $count Passwords",
        message = "Are you sure you want to permanently delete $count passwords? This action cannot be undone and will remove all selected passwords.",
        confirmText = "Delete All",
        confirmButtonColor = MaterialTheme.colorScheme.error,
        level = ConfirmationLevel.HIGH,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ArchivePasswordConfirmationDialog(
    passwordService: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Archive Password",
        message = "Archive the password for \"$passwordService\"? You can restore it later from the Archived Passwords section.",
        confirmText = "Archive",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.LOW,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun BulkArchiveConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Archive $count Passwords",
        message = "Archive $count passwords? They will be moved to the Archived Passwords section and can be restored later.",
        confirmText = "Archive All",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.LOW,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ClearUndoHistoryConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Clear Undo History",
        message = "This will permanently remove all undo history. You won't be able to undo any recent actions. Continue?",
        confirmText = "Clear History",
        confirmButtonColor = MaterialTheme.colorScheme.error,
        level = ConfirmationLevel.MEDIUM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ExportOverwriteConfirmationDialog(
    filename: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Overwrite File",
        message = "A file named \"$filename\" already exists. Do you want to overwrite it?",
        confirmText = "Overwrite",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.MEDIUM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ImportConflictConfirmationDialog(
    importedCount: Int,
    skippedCount: Int,
    conflictsCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Import Conflicts Detected",
        message = buildString {
            append("Import Summary:\n")
            append("• $importedCount passwords will be imported\n")
            if (skippedCount > 0) append("• $skippedCount passwords will be skipped\n")
            if (conflictsCount > 0) append("• $conflictsCount conflicts need resolution\n\n")
            append("Existing passwords with the same service name will be overwritten. Continue?")
        },
        confirmText = "Import",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.HIGH,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun MasterPasswordChangeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Change Master Password",
        message = "Changing your master password will require re-encrypting all your data. This process cannot be undone. Make sure you remember your new password!",
        confirmText = "Change Password",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.HIGH,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun DatabaseResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Reset Database",
        message = "This will permanently delete ALL passwords and cannot be undone. This action is irreversible and will require you to set up the app again from scratch.",
        confirmText = "Reset Everything",
        confirmButtonColor = MaterialTheme.colorScheme.error,
        level = ConfirmationLevel.CRITICAL,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Lock Vault",
        message = "This will lock your password vault and require biometric authentication or master password to access it again.",
        confirmText = "Lock Vault",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        level = ConfirmationLevel.LOW,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmButtonColor: Color,
    level: ConfirmationLevel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = level != ConfirmationLevel.CRITICAL,
            dismissOnClickOutside = level != ConfirmationLevel.CRITICAL
        ),
        icon = {
            Icon(
                imageVector = getConfirmationIcon(level),
                contentDescription = null,
                tint = getConfirmationColor(level)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Additional warnings for high-risk actions
                when (level) {
                    ConfirmationLevel.HIGH, ConfirmationLevel.CRITICAL -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        WarningCard(level)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmButtonColor
                )
            ) {
                Icon(
                    imageVector = getConfirmationIcon(level),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun WarningCard(level: ConfirmationLevel) {
    val (warningText, warningColor) = when (level) {
        ConfirmationLevel.HIGH -> "This action has significant consequences" to MaterialTheme.colorScheme.error
        ConfirmationLevel.CRITICAL -> "This action is irreversible and will cause permanent data loss" to MaterialTheme.colorScheme.error
        else -> "" to MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = warningColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = warningColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = warningText,
                style = MaterialTheme.typography.bodySmall,
                color = warningColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getConfirmationIcon(level: ConfirmationLevel): androidx.compose.ui.graphics.vector.ImageVector {
    return when (level) {
        ConfirmationLevel.LOW -> Icons.Default.CheckCircle
        ConfirmationLevel.MEDIUM -> Icons.Default.Warning
        ConfirmationLevel.HIGH -> Icons.Default.Warning
        ConfirmationLevel.CRITICAL -> Icons.Default.Delete
    }
}

private fun getConfirmationColor(level: ConfirmationLevel): Color {
    return when (level) {
        ConfirmationLevel.LOW -> Color(0xFF4CAF50) // Green
        ConfirmationLevel.MEDIUM -> Color(0xFFFF9800) // Orange
        ConfirmationLevel.HIGH -> Color(0xFFF44336) // Red
        ConfirmationLevel.CRITICAL -> Color(0xFF7B1FA2) // Purple
    }
}

// Specialized confirmation dialogs for specific scenarios

@Composable
fun PasswordStrengthWarningDialog(
    onProceedAnyway: () -> Unit,
    onImprovePassword: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
        },
        title = {
            Text("Weak Password Detected")
        },
        text = {
            Text(
                "This password is considered weak and may be vulnerable to attacks. Consider using a stronger password with uppercase letters, numbers, and special characters."
            )
        },
        confirmButton = {
            Button(onClick = onProceedAnyway) {
                Text("Use Anyway")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onImprovePassword) {
                    Text("Improve Password")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun DuplicateServiceWarningDialog(
    serviceName: String,
    onOverwrite: () -> Unit,
    onKeepBoth: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Duplicate Service Name")
        },
        text = {
            Text(
                "You already have a password saved for \"$serviceName\". What would you like to do?"
            )
        },
        confirmButton = {
            Button(onClick = onOverwrite) {
                Text("Overwrite")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onKeepBoth) {
                    Text("Keep Both")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun BiometricSetupDialog(
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Enable Biometric Authentication")
        },
        text = {
            Text(
                "Would you like to enable biometric authentication (fingerprint/face) for quicker access to your password vault?"
            )
        },
        confirmButton = {
            Button(onClick = onEnable) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    )
}
