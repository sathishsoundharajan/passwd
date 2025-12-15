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
import com.sathish.soundharajan.passwd.data.models.BankAccountData
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton

@Composable
fun BankAccountEntryForm(
    viewModel: VaultViewModel,
    entry: VaultEntry? = null,
    isEdit: Boolean = false
) {
    val bankData = entry?.let { viewModel.getBankAccountData(it) }

    var title by remember { mutableStateOf(entry?.title ?: "") }
    var bankName by remember { mutableStateOf(bankData?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(bankData?.accountNumber ?: "") }
    var routingNumber by remember { mutableStateOf(bankData?.routingNumber ?: "") }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isEdit) "Edit Bank Account Entry" else "Add Bank Account Entry",
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
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("Account Number *") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = routingNumber,
            onValueChange = { routingNumber = it },
            label = { Text("Routing Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        GlassButton(
            onClick = {
                if (isEdit && entry != null) {
                    // Update existing entry
                    viewModel.updateBankAccountEntry(
                        entryId = entry.id,
                        title = title,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        routingNumber = routingNumber
                    )
                } else {
                    // Add new entry
                    viewModel.addBankAccountEntry(
                        title = title,
                        bankName = bankName,
                        accountHolder = "",
                        accountNumber = accountNumber,
                        routingNumber = routingNumber,
                        iban = "",
                        accountType = "Checking",
                        swiftCode = "",
                        branch = "",
                        pin = "",
                        phoneNumber = "",
                        address = "",
                        category = "",
                        tags = "",
                        notes = ""
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && bankName.isNotBlank() && accountNumber.isNotBlank() && !isLoading
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
                    Text(if (isEdit) "Update Bank Account" else "Save Bank Account")
                }
            }
        }
    }
}
