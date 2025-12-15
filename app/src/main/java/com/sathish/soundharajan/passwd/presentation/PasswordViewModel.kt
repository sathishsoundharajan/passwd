package com.sathish.soundharajan.passwd.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sathish.soundharajan.passwd.data.PasswordEntry
import com.sathish.soundharajan.passwd.data.PasswordRepository
import com.sathish.soundharajan.passwd.data.ExportImportManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val exportImportManager: ExportImportManager
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
    }

    private val _passwords = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val passwords: StateFlow<List<PasswordEntry>> = _passwords.asStateFlow()

    private val _archivedPasswords = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val archivedPasswords: StateFlow<List<PasswordEntry>> = _archivedPasswords.asStateFlow()

    private val _recentlyDeletedPasswords = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val recentlyDeletedPasswords: StateFlow<List<PasswordEntry>> = _recentlyDeletedPasswords.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedPasswords = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPasswords: StateFlow<Set<Long>> = _selectedPasswords.asStateFlow()


    // Error handling - using SharedFlow for one-time events (not persistent state)
    private val _errorEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()
    
    // Legacy error state for screens that haven't migrated yet
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

    private val _hasMorePasswords = MutableStateFlow(true)
    val hasMorePasswords: StateFlow<Boolean> = _hasMorePasswords.asStateFlow()

    private val _hasMoreArchivedPasswords = MutableStateFlow(true)
    val hasMoreArchivedPasswords: StateFlow<Boolean> = _hasMoreArchivedPasswords.asStateFlow()

    private val _hasMoreSearchResults = MutableStateFlow(true)
    val hasMoreSearchResults: StateFlow<Boolean> = _hasMoreSearchResults.asStateFlow()

    private val _totalPasswordCount = MutableStateFlow(0)
    val totalPasswordCount: StateFlow<Int> = _totalPasswordCount.asStateFlow()

    private val _totalArchivedCount = MutableStateFlow(0)
    val totalArchivedCount: StateFlow<Int> = _totalArchivedCount.asStateFlow()

    private val _totalSearchResultCount = MutableStateFlow(0)
    val totalSearchResultCount: StateFlow<Int> = _totalSearchResultCount.asStateFlow()

    private var currentPasswordPage = 0
    private var currentArchivedPage = 0
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
        loadPasswordsPaged()
        loadArchivedPasswordsPaged()
        loadRecentlyDeletedPasswords()
        loadCounts()
    }

    private fun loadCounts() {
        viewModelScope.launch {
            passwordRepository.getActivePasswordCount()?.collect { count ->
                _totalPasswordCount.value = count
            }
        }
        viewModelScope.launch {
            passwordRepository.getArchivedPasswordCount()?.collect { count ->
                _totalArchivedCount.value = count
            }
        }
        viewModelScope.launch {
            passwordRepository.getRecentlyDeletedCount()?.collect { count ->
                // We can add a state for recently deleted count if needed
            }
        }
    }

    private fun loadRecentlyDeletedPasswords() {
        viewModelScope.launch {
            passwordRepository.getRecentlyDeletedPasswords()?.collect { passwords ->
                _recentlyDeletedPasswords.value = passwords
            }
        }
    }

    private fun loadPasswordsPaged() {
        if (_isLoading.value) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val offset = currentPasswordPage * PAGE_SIZE
                val newPasswords = passwordRepository.getPasswordsPaged(PAGE_SIZE, offset)
                if (newPasswords != null) {
                    if (currentPasswordPage == 0) {
                        _passwords.value = newPasswords
                    } else {
                        _passwords.value = _passwords.value + newPasswords
                    }
                    _hasMorePasswords.value = newPasswords.size == PAGE_SIZE
                } else {
                    _hasMorePasswords.value = false
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load passwords: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    fun loadArchivedPasswordsPaged(resetPage: Boolean = false) {
        if (resetPage) {
            currentArchivedPage = 0
            _hasMoreArchivedPasswords.value = true
        }

        // Don't block on general loading state for archived passwords
        viewModelScope.launch {
            try {
                val offset = currentArchivedPage * PAGE_SIZE
                val newPasswords = passwordRepository.getArchivedPasswordsPaged(PAGE_SIZE, offset)
                if (newPasswords != null) {
                    if (currentArchivedPage == 0) {
                        _archivedPasswords.value = newPasswords
                    } else {
                        _archivedPasswords.value = _archivedPasswords.value + newPasswords
                    }
                    _hasMoreArchivedPasswords.value = newPasswords.size == PAGE_SIZE
                } else {
                    _hasMoreArchivedPasswords.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load archived passwords: ${e.localizedMessage}"
            }
        }
    }

    init {
        // Observe search query with debounce
        viewModelScope.launch {
            // Wait for 2 chars or immediate clear
            _searchQuery
                .debounce(300L) // 300ms delay to wait for typing to stop
                .collect { query ->
                    if (query.isBlank()) {
                        currentPasswordPage = 0
                        loadPasswordsPaged()
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun searchPasswords(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        if (_isSearching.value) return

        _isSearching.value = true
        _loadingMessage.value = "Searching passwords..."
        currentSearchPage = 0

        viewModelScope.launch {
            try {
                // Keep count flow? Actually simpler to just not count for search or do one-shot.
                // Keeping count flow since it hasn't changed.
                passwordRepository.getSearchResultCount(query)?.collect { count ->
                    _totalSearchResultCount.value = count
                }
            } catch (e: Exception) {
                // Ignore count errors or handle
            }
        }
        
        viewModelScope.launch {
            try {
                val offset = currentSearchPage * PAGE_SIZE
                val searchResults = passwordRepository.searchPasswordsPaged(query, PAGE_SIZE, offset)
                if (searchResults != null) {
                    _passwords.value = searchResults
                    _hasMoreSearchResults.value = searchResults.size == PAGE_SIZE
                } else {
                    _passwords.value = emptyList()
                    _hasMoreSearchResults.value = false
                }
                _isSearching.value = false
                _loadingMessage.value = null
            } catch (e: Exception) {
                _error.value = "Failed to search passwords: ${e.localizedMessage}"
                _isSearching.value = false
                _loadingMessage.value = null
            }
        }
    }

    fun loadMorePasswords() {
        if (!_hasMorePasswords.value || _isLoading.value) return
        currentPasswordPage++
        loadPasswordsPaged()
    }

    fun loadMoreArchivedPasswords() {
        if (!_hasMoreArchivedPasswords.value || _isLoading.value) return
        currentArchivedPage++
        loadArchivedPasswordsPaged()
    }

    fun loadMoreSearchResults() {
        if (!_hasMoreSearchResults.value || _isLoading.value || _searchQuery.value.isBlank()) return
        currentSearchPage++
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val offset = currentSearchPage * PAGE_SIZE
                val newResults = passwordRepository.searchPasswordsPaged(_searchQuery.value, PAGE_SIZE, offset)
                if (newResults != null) {
                    _passwords.value = _passwords.value + newResults
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
        currentPasswordPage = 0
        currentArchivedPage = 0
        currentSearchPage = 0
        _hasMorePasswords.value = true
        _hasMoreArchivedPasswords.value = true
        _hasMoreSearchResults.value = true

        if (_searchQuery.value.isBlank()) {
            loadPasswordsPaged()
        } else {
            performSearch(_searchQuery.value)
        }
        loadArchivedPasswordsPaged()
        loadCounts()
    }

    fun addPassword(service: String, username: String, password: String, notes: String = "", tags: String = "") {
        // Only validate required fields - password strength is just a hint
        if (service.trim().isBlank()) {
            _error.value = "Service name is required"
            return
        }
        if (password.isBlank()) {
            _error.value = "Password is required"
            return
        }

        viewModelScope.launch {
            val entry = PasswordEntry(
                service = service.trim(),
                username = username.trim(),
                password = password,
                notes = notes.trim(),
                tags = tags.trim()
            )

            val result = passwordRepository.insertPassword(entry)
            result.fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to save password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    private fun validatePasswordInput(service: String, username: String, password: String): String? {
        return when {
            service.trim().isBlank() -> "Service name is required"
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters long"
            else -> null
        }
    }

    fun updatePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.updatePassword(entry).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to update password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun softDeletePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.softDeletePassword(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to delete password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun deletePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.deletePassword(entry).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to delete password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun restorePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.restorePassword(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to restore password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun permanentlyDeletePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordRepository.permanentlyDeletePassword(entry.id).fold(
                onSuccess = {
                    _error.value = null
                    loadRecentlyDeletedPasswords() // Refresh recently deleted list
                },
                onFailure = { exception ->
                    _error.value = "Failed to permanently delete password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun archivePassword(entry: PasswordEntry, archived: Boolean = true) {
        viewModelScope.launch {
            passwordRepository.archivePassword(entry.id, archived).fold(
                onSuccess = {
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to ${if (archived) "archive" else "unarchive"} password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedPasswords.value = emptySet()
        }
    }

    fun togglePasswordSelection(passwordId: Long) {
        val currentSelection = _selectedPasswords.value
        if (currentSelection.contains(passwordId)) {
            _selectedPasswords.value = currentSelection - passwordId
        } else {
            _selectedPasswords.value = currentSelection + passwordId
        }
    }

    fun selectAllPasswords() {
        val allIds = _passwords.value.map { it.id }.toSet()
        _selectedPasswords.value = allIds
    }

    fun clearSelection() {
        _selectedPasswords.value = emptySet()
    }

    fun clearError() {
        _error.value = null
    }

    fun exportPasswords(context: Context, format: String = "json", masterPassword: String, destinationUri: Uri) {
        // Cancel any existing export job
        currentExportJob?.cancel()

        currentExportJob = viewModelScope.launch {
            _isExporting.value = true
            _loadingMessage.value = "Exporting data..."
            _error.value = null

            try {
                // Get all passwords (active + archived) as a one-time snapshot
                val activePasswords = passwordRepository.getAllPasswordsOnce() ?: emptyList()
                val archivedPasswords = passwordRepository.getAllArchivedPasswordsOnce() ?: emptyList()
                val allPasswords = activePasswords + archivedPasswords

                // Convert PasswordEntry to VaultEntry for export
                val vaultEntries = allPasswords.map { password ->
                    // This would need proper conversion - for now just skip export
                    // TODO: Implement proper conversion from PasswordEntry to VaultEntry
                    throw NotImplementedError("Export not yet implemented for new vault system")
                }

                exportImportManager.exportVaultEntries(vaultEntries, format, masterPassword, destinationUri).fold(
                    onSuccess = {
                        _error.value = "Successfully exported ${it.totalEntries} entries"
                        _loadingMessage.value = "Export complete!"
                    },
                    onFailure = {
                        _error.value = "Export failed: ${it.localizedMessage}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
                _loadingMessage.value = null
                currentExportJob = null
            }
        }
    }

    fun importPasswords(context: Context, sourceUri: Uri, masterPassword: String, conflictStrategy: ExportImportManager.ConflictStrategy = ExportImportManager.ConflictStrategy.OVERWRITE) {
        // Cancel any existing import job
        currentImportJob?.cancel()

        currentImportJob = viewModelScope.launch {
            _isImporting.value = true
            _loadingMessage.value = "Importing data..."
            exportImportManager.importVaultEntries(sourceUri, masterPassword, conflictStrategy).fold(
                onSuccess = {
                    _error.value = "Imported ${it.importedEntries}, Skipped ${it.totalEntries - it.importedEntries}"
                    refreshData()
                },
                onFailure = { _error.value = "Import failed: ${it.localizedMessage}" }
            )
            _isImporting.value = false
            _loadingMessage.value = null
            currentImportJob = null
        }
    }



    fun generateExportFilename(format: String = "json"): String {
        return exportImportManager.generateExportFilename(format)
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

    fun bulkDeleteSelected() {
        viewModelScope.launch {
            val selectedIds = _selectedPasswords.value
            val selectedPasswords = _passwords.value.filter { it.id in selectedIds }
            if (selectedPasswords.isNotEmpty()) {
                // Soft delete each password individually
                selectedPasswords.forEach { password ->
                    passwordRepository.softDeletePassword(password.id)
                }
                _selectedPasswords.value = emptySet()
                _isSelectionMode.value = false
                refreshData()
            }
        }
    }

    fun bulkArchiveSelected(archive: Boolean = true) {
        viewModelScope.launch {
            val selectedIds = _selectedPasswords.value.toList()
            if (selectedIds.isNotEmpty()) {
                selectedIds.forEach { id ->
                    passwordRepository.archivePassword(id, archive)
                }
                _selectedPasswords.value = emptySet()
                _isSelectionMode.value = false
                refreshData()
            }
        }
    }

    fun changeMasterPassword(
        oldPassword: String,
        newPassword: String,
        onProgress: ((current: Int, total: Int, step: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            passwordRepository.changeMasterPassword(oldPassword, newPassword) { current, total, step ->
                onProgress?.invoke(current, total, step)
            }.fold(
                onSuccess = {
                    _error.value = "Master password changed successfully"
                    refreshData()
                },
                onFailure = { exception ->
                    _error.value = "Failed to change master password: ${exception.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

}
