package com.sathish.soundharajan.passwd.ui.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardEntryForm(viewModel: VaultViewModel) {
    var title by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expirationMonth by remember { mutableStateOf(1) }
    var expirationYear by remember { mutableStateOf(2025) }
    var cvv by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("Unknown") }
    var issuingBank by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var billingAddress by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    val cardTypes = listOf("Visa", "Mastercard", "American Express", "Discover", "Other")
    val months = (1..12).toList()
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (currentYear..(currentYear + 20)).toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Information
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Basic Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g., Main Credit Card") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Cardholder Name *") },
                    placeholder = { Text("Name on card") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = issuingBank,
                    onValueChange = { issuingBank = it },
                    label = { Text("Issuing Bank *") },
                    placeholder = { Text("e.g., Chase, Capital One") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Card Details
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Card Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number *") },
                    placeholder = { Text("16-digit card number") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Expiration Date
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Month Dropdown
                    var monthExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = !monthExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = expirationMonth.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", month)) },
                                    onClick = {
                                        expirationMonth = month
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Year Dropdown
                    var yearExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = !yearExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = expirationYear.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        expirationYear = year
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { cvv = it },
                    label = { Text("CVV *") },
                    placeholder = { Text("3-4 digit security code") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Card Type Dropdown
                var cardTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = cardTypeExpanded,
                    onExpandedChange = { cardTypeExpanded = !cardTypeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = cardType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Card Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = cardTypeExpanded,
                        onDismissRequest = { cardTypeExpanded = false }
                    ) {
                        cardTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    cardType = type
                                    cardTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Security Information
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Security Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    placeholder = { Text("Card PIN if applicable") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Billing Information
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Billing Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = billingAddress,
                    onValueChange = { billingAddress = it },
                    label = { Text("Billing Address") },
                    placeholder = { Text("Card billing address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("Card issuer contact") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Organization
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Organization",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g., Personal, Business") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    placeholder = { Text("comma, separated, tags") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Additional Notes
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Additional Notes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Additional information...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        }

        // Save Button
        GlassButton(
            onClick = {
                viewModel.addCreditCardEntry(
                    title = title,
                    cardholderName = cardholderName,
                    cardNumber = cardNumber,
                    expirationMonth = expirationMonth,
                    expirationYear = expirationYear,
                    cvv = cvv,
                    cardType = cardType,
                    issuingBank = issuingBank,
                    pin = pin,
                    billingAddress = billingAddress,
                    phoneNumber = phoneNumber,
                    notes = notes,
                    category = category,
                    tags = tags
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && cardholderName.isNotBlank() &&
                     cardNumber.isNotBlank() && issuingBank.isNotBlank() &&
                     cvv.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Credit Card")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
