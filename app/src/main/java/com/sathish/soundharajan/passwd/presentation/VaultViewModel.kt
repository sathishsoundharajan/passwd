package com.sathish.soundharajan.passwd.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sathish.soundharajan.passwd.data.*
import com.sathish.soundharajan.passwd.data.models.*
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val vaultManager: VaultManager,
    private val exportImportManager: ExportImportManager
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
    }

    // Vault entries (all types)
    private val _vaultEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val vaultEntries: StateFlow<List<VaultEntry>> = _vaultEntries.asStateFlow()

    // Type-specific entries
    private val _passwordEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val passwordEntries: StateFlow<List<VaultEntry>> = _passwordEntries.asStateFlow()

    private val _bankAccountEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val bankAccountEntries: StateFlow<List<VaultEntry>> = _bankAccountEntries.asStateFlow()

    private val _creditCardEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val creditCardEntries: StateFlow<List<VaultEntry>> = _creditCardEntries.asStateFlow()

    private val _identityCardEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val identityCardEntries: StateFlow<List<VaultEntry>> = _identityCardEntries.asStateFlow()

    private val _archivedEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val archivedEntries: StateFlow<List<VaultEntry>> = _archivedEntries.asStateFlow()

    private val _recentlyDeletedEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val recentlyDeletedEntries: StateFlow<List<VaultEntry>> = _recentlyDeletedEntries.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilterType = MutableStateFlow<VaultEntryType?>(null)
    val selectedFilterType: StateFlow<VaultEntryType?> = _selectedFilterType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedEntries = MutableStateFlow<Set<Long>>(emptySet())
    val selectedEntries: StateFlow<Set<Long>> = _selectedEntries.asStateFlow()

    // Error handling
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private var currentImportJob: kotlinx.coroutines.Job? = null
    private var currentExportJob: kotlinx.coroutines.Job? = null

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _hasMoreEntries = MutableStateFlow(true)
    val hasMoreEntries: StateFlow<Boolean> = _hasMoreEntries.asStateFlow()

    private val _hasMoreSearchResults = MutableStateFlow(true)
    val hasMoreSearchResults: StateFlow<Boolean> = _hasMoreSearchResults.asStateFlow()

    private val _totalEntryCount = MutableStateFlow(0)
    val totalEntryCount: StateFlow<Int> = _totalEntryCount.asStateFlow()

    private val _totalArchivedCount = MutableStateFlow(0)
    val totalArchivedCount: StateFlow<Int> = _totalArchivedCount.asStateFlow()

    private val _totalRecentlyDeletedCount = MutableStateFlow(0)
    val totalRecentlyDeletedCount: StateFlow<Int> = _totalRecentlyDeletedCount.asStateFlow()

    private val _totalSearchResultCount = MutableStateFlow(0)
    val totalSearchResultCount: StateFlow<Int> = _totalSearchResultCount.asStateFlow()

    private var currentPage = 0
    private var currentSearchPage = 0

    init {
        // Observe search query with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun initializeData() {
        // Check if vault is open before trying to load data
        if (vaultManager.getDatabase() == null) {
            // Vault not open yet, this will be called again after authentication
            return
        }

        // Only load essential data immediately for fast UI response
        loadVaultEntriesPaged()
        loadCounts()

        // Load other data in background to avoid blocking UI
        viewModelScope.launch {
            loadTypeSpecificEntries()
            loadArchivedEntries()
            loadRecentlyDeletedEntries()
            loadAvailableCategories()
        }
    }

    private fun loadCounts() {
        viewModelScope.launch {
            vaultRepository.getActiveVaultEntryCount()?.collect { count ->
                _totalEntryCount.value = count
            }
        }
        viewModelScope.launch {
            vaultRepository.getArchivedVaultEntryCount()?.collect { count ->
                _totalArchivedCount.value = count
            }
        }
        viewModelScope.launch {
            vaultRepository.getRecentlyDeletedCount()?.collect { count ->
                _totalRecentlyDeletedCount.value = count
            }
        }
    }

    private fun loadTypeSpecificEntries() {
        viewModelScope.launch {
            vaultRepository.getVaultEntriesByType(VaultEntryType.PASSWORD)?.collect { entries ->
                _passwordEntries.value = entries
            }
        }
        viewModelScope.launch {
            vaultRepository.getVaultEntriesByType(VaultEntryType.BANK_ACCOUNT)?.collect { entries ->
                _bankAccountEntries.value = entries
            }
        }
        viewModelScope.launch {
            vaultRepository.getVaultEntriesByType(VaultEntryType.CREDIT_CARD)?.collect { entries ->
                _creditCardEntries.value = entries
            }
        }
        viewModelScope.launch {
            vaultRepository.getVaultEntriesByType(VaultEntryType.IDENTITY_CARD)?.collect { entries ->
                _identityCardEntries.value = entries
            }
        }
    }

    private fun loadArchivedEntries() {
        viewModelScope.launch {
            vaultRepository.getArchivedVaultEntries()?.collect { entries ->
                _archivedEntries.value = entries
            }
        }
    }

    private fun loadRecentlyDeletedEntries() {
        viewModelScope.launch {
            vaultRepository.getRecentlyDeletedVaultEntries()?.collect { entries ->
                _recentlyDeletedEntries.value = entries
            }
        }
    }

    private fun loadVaultEntriesPaged() {
        if (_isLoading.value) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val offset = currentPage * PAGE_SIZE
                val allEntries = if (_selectedFilterType.value != null) {
                    vaultRepository.getVaultEntriesByTypePaged(_selectedFilterType.value!!, PAGE_SIZE, offset)
                } else {
                    vaultRepository.getVaultEntriesPaged(PAGE_SIZE, offset)
                }

                if (allEntries != null) {
                    if (currentPage == 0) {
                        _vaultEntries.value = allEntries
                    } else {
                        _vaultEntries.value = _vaultEntries.value + allEntries
                    }
                    _hasMoreEntries.value = allEntries.size == PAGE_SIZE
                } else {
                    _hasMoreEntries.value = false
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load entries: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun searchEntries(query: String) {
        _searchQuery.value = query
    }

    fun filterByType(type: VaultEntryType?) {
        _selectedFilterType.value = type
        refreshData()
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        refreshData()
    }

    private fun loadAvailableCategories() {
        viewModelScope.launch {
            try {
                val allEntries = vaultRepository.getAllVaultEntriesOnce()
                val categories = allEntries
                    ?.filter { !it.isDeleted && !it.isArchived }
                    ?.mapNotNull { it.category.takeIf { it.isNotBlank() } }
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()
                _availableCategories.value = categories
            } catch (e: Exception) {
                // Ignore category loading errors
            }
        }
    }

    private fun performSearch(query: String) {
        if (_isSearching.value) return

        _isSearching.value = true
        _loadingMessage.value = "Searching vault..."
        currentSearchPage = 0

        viewModelScope.launch {
            try {
                // For comprehensive search, we need to get all entries and filter them
                // This is because our search now includes encrypted data
                val allEntries = vaultRepository.getAllVaultEntriesOnce()
                val filteredByType = if (_selectedFilterType.value != null) {
                    allEntries?.filter { it.type == _selectedFilterType.value && it.isDeleted == false && it.isArchived == false }
                } else {
                    allEntries?.filter { it.isDeleted == false && it.isArchived == false }
                }
                val searchResults = filteredByType?.filter { entry ->
                    vaultRepository.matchesSearchQuery(entry, query)
                }
                _totalSearchResultCount.value = searchResults?.size ?: 0

                val offset = currentSearchPage * PAGE_SIZE
                val pagedResults = searchResults?.drop(offset)?.take(PAGE_SIZE) ?: emptyList()
                _vaultEntries.value = pagedResults
                _hasMoreSearchResults.value = (searchResults?.size ?: 0) > offset + pagedResults.size

                _isSearching.value = false
                _loadingMessage.value = null
            } catch (e: Exception) {
                _error.value = "Failed to search entries: ${e.localizedMessage}"
                _isSearching.value = false
                _loadingMessage.value = null
            }
        }
    }

    fun loadMoreEntries() {
        if (!_hasMoreEntries.value || _isLoading.value) return
        currentPage++
        loadVaultEntriesPaged()
    }

    fun loadMoreSearchResults() {
        if (!_hasMoreSearchResults.value || _isLoading.value || _searchQuery.value.isBlank()) return
        currentSearchPage++
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val offset = currentSearchPage * PAGE_SIZE
                val newResults = vaultRepository.searchVaultEntriesPaged(_searchQuery.value, PAGE_SIZE, offset)
                if (newResults != null) {
                    _vaultEntries.value = _vaultEntries.value + newResults
                    _hasMoreSearchResults.value = newResults.size == PAGE_SIZE
                } else {
                    _hasMoreSearchResults.value = false
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load more search results: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        currentPage = 0
        currentSearchPage = 0
        _hasMoreEntries.value = true
        _hasMoreSearchResults.value = true

        // Only refresh the main list that's currently visible
        if (_searchQuery.value.isBlank()) {
            loadVaultEntriesPaged()
        } else {
            performSearch(_searchQuery.value)
        }

        // Defer other operations to avoid blocking UI
        viewModelScope.launch {
            loadTypeSpecificEntries()
            loadArchivedEntries()
            loadCounts()
        }
    }

    // Password Entry Operations
    fun addPasswordEntry(
        title: String,
        service: String,
        username: String,
        password: String,
        url: String = "",
        category: String = "",
        tags: String = "",
        notes: String = ""
    ) {
        // Clear any previous errors to prevent blocking
        _error.value = null

        if (title.trim().isBlank()) {
            _error.value = "Title is required"
            return
        }
        if (password.isBlank()) {
            _error.value = "Password is required"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            vaultRepository.insertPasswordEntry(
                title, service, username, password, url, category, tags, notes
            ).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = "Failed to save password: ${exception.localizedMessage ?: "Unknown error"}"
                    _isLoading.value = false
                }
            )
        }
    }

    // Bank Account Entry Operations
    fun addBankAccountEntry(
        title: String,
        bankName: String,
        accountHolder: String,
        accountNumber: String,
        routingNumber: String = "",
        iban: String = "",
        accountType: String = "Checking",
        swiftCode: String = "",
        branch: String = "",
        pin: String = "",
        phoneNumber: String = "",
        address: String = "",
        category: String = "",
        tags: String = "",
        notes: String = ""
    ) {
        if (title.trim().isBlank()) {
            _error.value = "Title is required"
            return
        }
        if (accountNumber.isBlank()) {
            _error.value = "Account number is required"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            vaultRepository.insertBankAccountEntry(
                title, bankName, accountHolder, accountNumber, routingNumber, iban,
                accountType, swiftCode, branch, pin, phoneNumber, address, category, tags, notes
            ).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = "Failed to save bank account: ${exception.localizedMessage ?: "Unknown error"}"
                    _isLoading.value = false
                }
            )
        }
    }

    // Credit Card Entry Operations
    fun addCreditCardEntry(
        title: String,
        cardholderName: String,
        cardNumber: String,
        expirationMonth: Int,
        expirationYear: Int,
        cvv: String,
        cardType: String = "Unknown",
        issuingBank: String,
        pin: String = "",
        billingAddress: String = "",
        phoneNumber: String = "",
        notes: String = "",
        category: String = "",
        tags: String = ""
    ) {
        if (title.trim().isBlank()) {
            _error.value = "Title is required"
            return
        }
        if (cardNumber.isBlank()) {
            _error.value = "Card number is required"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            vaultRepository.insertCreditCardEntry(
                title, cardholderName, cardNumber, expirationMonth, expirationYear,
                cvv, cardType, issuingBank, pin, billingAddress, phoneNumber, notes, category, tags
            ).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = "Failed to save credit card: ${exception.localizedMessage ?: "Unknown error"}"
                    _isLoading.value = false
                }
            )
        }
    }

    // Identity Card Entry Operations
    fun addIdentityCardEntry(
        title: String,
        fullName: String,
        idNumber: String,
        documentType: String,
        issuingAuthority: String,
        issueDate: Long,
        expirationDate: Long,
        dateOfBirth: Long? = null,
        nationality: String = "",
        address: String = "",
        placeOfBirth: String = "",
        gender: String = "",
        height: String = "",
        eyeColor: String = "",
        bloodType: String = "",
        organDonor: Boolean = false,
        identifyingMarks: String = "",
        emergencyContact: String = "",
        additionalInfo: Map<String, String> = emptyMap(),
        category: String = "",
        tags: String = "",
        notes: String = ""
    ) {
        if (title.trim().isBlank()) {
            _error.value = "Title is required"
            return
        }
        if (fullName.trim().isBlank()) {
            _error.value = "Full name is required"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            vaultRepository.insertIdentityCardEntry(
                title, fullName, idNumber, documentType, issuingAuthority, issueDate, expirationDate,
                dateOfBirth, nationality, address, placeOfBirth, gender, height, eyeColor,
                bloodType, organDonor, identifyingMarks, emergencyContact, additionalInfo,
                category, tags, notes
            ).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = "Failed to save identity card: ${exception.localizedMessage ?: "Unknown error"}"
                    _isLoading.value = false
                }
            )
        }
    }

    // Document Management
    suspend fun addDocumentToEntry(entryId: Long, uri: Uri, fileName: String): Result<String> {
        return vaultRepository.addDocumentToEntry(entryId, uri, fileName)
    }

    suspend fun removeDocumentFromEntry(entryId: Long, pathIdentifier: String): Result<Unit> {
        return vaultRepository.removeDocumentFromEntry(entryId, pathIdentifier)
    }

    suspend fun getDocument(pathIdentifier: String): Result<ByteArray> {
        return vaultRepository.getDocument(pathIdentifier)
    }

    suspend fun getDocumentInfo(pathIdentifier: String): Result<DocumentInfo> {
        return vaultRepository.getDocumentInfo(pathIdentifier)
    }

    // Generic operations
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            vaultRepository.updateVaultEntry(entry).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to update entry: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun softDeleteEntry(entry: VaultEntry) {
        viewModelScope.launch {
            vaultRepository.softDeleteVaultEntry(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to delete entry: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun restoreEntry(entry: VaultEntry) {
        viewModelScope.launch {
            vaultRepository.restoreVaultEntry(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to restore entry: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun permanentlyDeleteEntry(entry: VaultEntry) {
        viewModelScope.launch {
            vaultRepository.permanentlyDeleteVaultEntry(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    loadRecentlyDeletedEntries()
                },
                onFailure = { exception ->
                    _error.value = "Failed to permanently delete entry: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun archiveEntry(entry: VaultEntry, archived: Boolean = true) {
        viewModelScope.launch {
            vaultRepository.archiveVaultEntry(entry.id, archived).fold(
                onSuccess = {
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to ${if (archived) "archive" else "unarchive"} entry: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    // Selection operations
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedEntries.value = emptySet()
        }
    }

    fun toggleEntrySelection(entryId: Long) {
        val currentSelection = _selectedEntries.value
        if (currentSelection.contains(entryId)) {
            _selectedEntries.value = currentSelection - entryId
        } else {
            _selectedEntries.value = currentSelection + entryId
        }
    }

    fun selectAllEntries() {
        val allIds = _vaultEntries.value.map { it.id }.toSet()
        _selectedEntries.value = allIds
    }

    fun clearSelection() {
        _selectedEntries.value = emptySet()
    }

    fun clearError() {
        _error.value = null
    }

    // Bulk operations
    fun bulkDeleteSelected() {
        viewModelScope.launch {
            val selectedIds = _selectedEntries.value
            val selectedEntries = _vaultEntries.value.filter { it.id in selectedIds }
            if (selectedEntries.isNotEmpty()) {
                selectedEntries.forEach { entry ->
                    vaultRepository.softDeleteVaultEntry(entry.id)
                }
                _selectedEntries.value = emptySet()
                _isSelectionMode.value = false
                refreshData()
            }
        }
    }

    fun bulkArchiveSelected(archive: Boolean = true) {
        viewModelScope.launch {
            val selectedIds = _selectedEntries.value.toList()
            if (selectedIds.isNotEmpty()) {
                selectedIds.forEach { id ->
                    vaultRepository.archiveVaultEntry(id, archive)
                }
                _selectedEntries.value = emptySet()
                _isSelectionMode.value = false
                refreshData()
            }
        }
    }

    // Type-specific data getters
    fun getPasswordData(entry: VaultEntry): PasswordData? {
        return vaultRepository.getPasswordData(entry)
    }

    fun getBankAccountData(entry: VaultEntry): BankAccountData? {
        return vaultRepository.getBankAccountData(entry)
    }

    fun getCreditCardData(entry: VaultEntry): CreditCardData? {
        return vaultRepository.getCreditCardData(entry)
    }

    fun getIdentityCardData(entry: VaultEntry): IdentityCardData? {
        return vaultRepository.getIdentityCardData(entry)
    }

    // Get entry by ID
    suspend fun getVaultEntryById(entryId: Long): VaultEntry? {
        return vaultRepository.getVaultEntryById(entryId)
    }

    // Update password data
    fun updatePasswordEntry(
        entryId: Long,
        title: String,
        username: String,
        password: String,
        tags: String,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                // Update VaultEntry fields
                val entry = vaultRepository.getVaultEntryById(entryId)
                if (entry != null) {
                    val updatedEntry = entry.copy(
                        title = title,
                        tags = tags,
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    vaultRepository.updateVaultEntry(updatedEntry).fold(
                        onSuccess = {
                            // Update password data
                            vaultRepository.updatePasswordData(entryId, "", username, password, "").fold(
                                onSuccess = {
                                    _error.value = null
                                    refreshData()
                                },
                                onFailure = { exception ->
                                    _error.value = "Failed to update password data: ${exception.localizedMessage ?: "Unknown error"}"
                                }
                            )
                        },
                        onFailure = { exception ->
                            _error.value = "Failed to update entry: ${exception.localizedMessage ?: "Unknown error"}"
                        }
                    )
                } else {
                    _error.value = "Entry not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update password entry: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // Update bank account data
    fun updateBankAccountEntry(
        entryId: Long,
        title: String,
        bankName: String,
        accountNumber: String,
        routingNumber: String
    ) {
        viewModelScope.launch {
            try {
                // Update VaultEntry fields
                val entry = vaultRepository.getVaultEntryById(entryId)
                if (entry != null) {
                    val updatedEntry = entry.copy(
                        title = title,
                        updatedAt = System.currentTimeMillis()
                    )
                    vaultRepository.updateVaultEntry(updatedEntry).fold(
                        onSuccess = {
                            // Update bank account data
                            vaultRepository.updateBankAccountData(
                                entryId, bankName, "", accountNumber, routingNumber,
                                "", "Checking", "", "", "", "", ""
                            ).fold(
                                onSuccess = {
                                    _error.value = null
                                    refreshData()
                                },
                                onFailure = { exception ->
                                    _error.value = "Failed to update bank account data: ${exception.localizedMessage ?: "Unknown error"}"
                                }
                            )
                        },
                        onFailure = { exception ->
                            _error.value = "Failed to update entry: ${exception.localizedMessage ?: "Unknown error"}"
                        }
                    )
                } else {
                    _error.value = "Entry not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update bank account entry: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // Export/Import operations (placeholder for now)
    fun exportVaultEntries(context: Context, format: String = "json", masterPassword: String, destinationUri: Uri) {
        // TODO: Implement vault export with mixed data types
        _error.value = "Vault export not yet implemented"
    }

    fun importVaultEntries(context: Context, sourceUri: Uri, masterPassword: String) {
        // TODO: Implement vault import with mixed data types
        _error.value = "Vault import not yet implemented"
    }

    fun cancelExport() {
        currentExportJob?.cancel()
        currentExportJob = null
        _isExporting.value = false
        _loadingMessage.value = null
        _error.value = "Export cancelled"
    }

    fun cancelImport() {
        currentImportJob?.cancel()
        currentImportJob = null
        _isImporting.value = false
        _loadingMessage.value = null
        _error.value = "Import cancelled"
    }
}
