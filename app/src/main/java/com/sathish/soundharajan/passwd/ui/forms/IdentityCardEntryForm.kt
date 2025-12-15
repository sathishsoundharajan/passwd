package com.sathish.soundharajan.passwd.ui.forms

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sathish.soundharajan.passwd.presentation.VaultViewModel
import com.sathish.soundharajan.passwd.ui.components.GlassButton
import com.sathish.soundharajan.passwd.ui.components.GlassCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityCardEntryForm(viewModel: VaultViewModel) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var documentType by remember { mutableStateOf("Passport") }
    var issuingAuthority by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expirationDate by remember { mutableStateOf(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) }
    var dateOfBirth by remember { mutableStateOf<Long?>(null) }
    var nationality by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var placeOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var eyeColor by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var organDonor by remember { mutableStateOf(false) }
    var identifyingMarks by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    val documentTypes = listOf(
        "Passport", "Driver's License", "National ID", "Birth Certificate",
        "Social Security Card", "Medical Card", "Student ID", "Other"
    )
    val genders = listOf("Male", "Female", "Other", "Prefer not to say")
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")

    // Document management
    var documents by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDocumentOptions by remember { mutableStateOf(false) }
    var currentPhotoPath by remember { mutableStateOf<String?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoPath != null) {
            // Add the captured photo to documents
            // Note: In a real implementation, you'd move the file to secure storage
            // For now, we'll just add a placeholder
            documents = documents + "photo_${System.currentTimeMillis()}.jpg"
        }
        currentPhotoPath = null
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Handle file selection
            // Note: In a real implementation, you'd copy the file to secure storage
            // For now, we'll just add a placeholder
            val fileName = "document_${System.currentTimeMillis()}.pdf"
            documents = documents + fileName
        }
    }

    // Create camera image file
    fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = context.cacheDir
        return File(storageDir, "JPEG_${timeStamp}_.jpg")
    }

    fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                it
            )
            currentPhotoPath = it.absolutePath
            cameraLauncher.launch(photoURI)
        }
    }

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
                    placeholder = { Text("e.g., My Passport") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("Full name as on document") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = idNumber,
                    onValueChange = { idNumber = it },
                    label = { Text("ID Number") },
                    placeholder = { Text("Document number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Document Type Dropdown
                var docTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = docTypeExpanded,
                    onExpandedChange = { docTypeExpanded = !docTypeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = documentType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = docTypeExpanded,
                        onDismissRequest = { docTypeExpanded = false }
                    ) {
                        documentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    documentType = type
                                    docTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = issuingAuthority,
                    onValueChange = { issuingAuthority = it },
                    label = { Text("Issuing Authority") },
                    placeholder = { Text("Government agency, DMV, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Dates
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Important Dates",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Issue Date
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(issueDate)),
                    onValueChange = { },
                    label = { Text("Issue Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Expiration Date
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expirationDate)),
                    onValueChange = { },
                    label = { Text("Expiration Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date of Birth (optional)
                OutlinedTextField(
                    value = dateOfBirth?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                    onValueChange = { },
                    label = { Text("Date of Birth (Optional)") },
                    placeholder = { Text("Not all documents require this") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Personal Information
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = { Text("Nationality") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = placeOfBirth,
                    onValueChange = { placeOfBirth = it },
                    label = { Text("Place of Birth") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Gender Dropdown
                var genderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genders.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gender = g
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height") },
                        placeholder = { Text("5'10\"") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = eyeColor,
                        onValueChange = { eyeColor = it },
                        label = { Text("Eye Color") },
                        placeholder = { Text("Brown") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Blood Type Dropdown
                var bloodTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bloodTypeExpanded,
                    onExpandedChange = { bloodTypeExpanded = !bloodTypeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = bloodType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = bloodTypeExpanded,
                        onDismissRequest = { bloodTypeExpanded = false }
                    ) {
                        bloodTypes.forEach { bt ->
                            DropdownMenuItem(
                                text = { Text(bt) },
                                onClick = {
                                    bloodType = bt
                                    bloodTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = organDonor,
                        onCheckedChange = { organDonor = it }
                    )
                    Text("Organ Donor", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // Physical Characteristics & Contact
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Additional Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = identifyingMarks,
                    onValueChange = { identifyingMarks = it },
                    label = { Text("Identifying Marks") },
                    placeholder = { Text("Scars, tattoos, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact") },
                    placeholder = { Text("Name and phone number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        }

        // Document Attachments
        GlassCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Document Attachments (${documents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = { showDocumentOptions = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Document")
                    }
                }

                if (documents.isNotEmpty()) {
                    documents.forEach { doc ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(doc, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                documents = documents - doc
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                } else {
                    Text(
                        "No documents attached. Tap + to add photos or files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
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
                    placeholder = { Text("e.g., Travel, Personal") },
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
                viewModel.addIdentityCardEntry(
                    title = title,
                    fullName = fullName,
                    idNumber = idNumber,
                    documentType = documentType,
                    issuingAuthority = issuingAuthority,
                    issueDate = issueDate,
                    expirationDate = expirationDate,
                    dateOfBirth = dateOfBirth,
                    nationality = nationality,
                    address = address,
                    placeOfBirth = placeOfBirth,
                    gender = gender,
                    height = height,
                    eyeColor = eyeColor,
                    bloodType = bloodType,
                    organDonor = organDonor,
                    identifyingMarks = identifyingMarks,
                    emergencyContact = emergencyContact,
                    additionalInfo = emptyMap(), // TODO: Implement additional fields
                    category = category,
                    tags = tags,
                    notes = notes
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && fullName.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Identity Card")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Document Options Dialog
    if (showDocumentOptions) {
        AlertDialog(
            onDismissRequest = { showDocumentOptions = false },
            title = { Text("Add Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to add a document:")
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showDocumentOptions = false
                            dispatchTakePictureIntent()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo")
                    }

                    TextButton(
                        onClick = {
                            showDocumentOptions = false
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose File")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocumentOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
