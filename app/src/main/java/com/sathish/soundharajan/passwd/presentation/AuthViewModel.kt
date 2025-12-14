package com.sathish.soundharajan.passwd.presentation

import androidx.lifecycle.ViewModel
import com.sathish.soundharajan.passwd.data.VaultManager
import com.sathish.soundharajan.passwd.security.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val vaultManager: VaultManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _authState.value = if (authManager.isMasterPasswordSet()) {
            AuthState.LoginRequired
        } else {
            AuthState.SetupRequired
        }
    }

    fun setupMasterPassword(password: String) {
        authManager.setMasterPassword(password)
        vaultManager.openVault(password)
        _authState.value = AuthState.Authenticated
    }

    fun login(password: String): Boolean {
        return if (authManager.verifyMasterPassword(password)) {
            vaultManager.openVault(password)
            _authState.value = AuthState.Authenticated
            true
        } else {
            false
        }
    }

    suspend fun biometricLogin(activity: FragmentActivity): Boolean {
        if (!authManager.isBiometricEnabled()) {
            return false
        }

        // First authenticate with biometrics
        val biometricSuccess = authManager.authenticateWithBiometric(activity)
        if (!biometricSuccess) {
            return false
        }

        // If biometric authentication succeeded, retrieve the stored password
        val storedPassword = authManager.retrieveBiometricPassword()
        if (storedPassword != null && authManager.verifyMasterPassword(storedPassword)) {
            vaultManager.openVault(storedPassword)
            _authState.value = AuthState.Authenticated
            return true
        }

        return false
    }

    fun enableBiometric(password: String) {
        try {
            authManager.setBiometricEnabled(true, password)
        } catch (e: Exception) {
            // Handle biometric setup failure
            throw RuntimeException("Failed to enable biometric authentication", e)
        }
    }

    fun disableBiometric() {
        authManager.setBiometricEnabled(false)
    }

    fun isBiometricAvailable(): Boolean {
        return authManager.isBiometricAvailable()
    }

    fun isBiometricEnabled(): Boolean {
        return authManager.isBiometricEnabled()
    }

    fun logout() {
        vaultManager.closeVault()
        _authState.value = AuthState.LoginRequired
    }

    // Expose AuthManager for use in other screens (like Settings)
    fun getAuthManager(): AuthManager = authManager
}

sealed class AuthState {
    object Checking : AuthState()
    object SetupRequired : AuthState()
    object LoginRequired : AuthState()
    object Authenticated : AuthState()
}
