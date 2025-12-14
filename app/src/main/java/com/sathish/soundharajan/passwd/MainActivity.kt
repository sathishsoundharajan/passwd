package com.sathish.soundharajan.passwd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sathish.soundharajan.passwd.navigation.Screen
import com.sathish.soundharajan.passwd.presentation.AuthViewModel
import com.sathish.soundharajan.passwd.presentation.PasswordViewModel
import com.sathish.soundharajan.passwd.security.AuthManager
import com.sathish.soundharajan.passwd.ui.AddPasswordScreen
import com.sathish.soundharajan.passwd.ui.ArchiveScreen
import com.sathish.soundharajan.passwd.ui.AuthScreen
import com.sathish.soundharajan.passwd.ui.EditPasswordScreen
import com.sathish.soundharajan.passwd.ui.PasswordListScreen
import com.sathish.soundharajan.passwd.ui.RecentlyDeletedScreen
import com.sathish.soundharajan.passwd.ui.SettingsScreen
import com.sathish.soundharajan.passwd.ui.theme.PasswdTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manual initializations removed - handled by Hilt
        enableEdgeToEdge()
        setContent {
            PasswdTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Using hiltViewModel() to get Hilt-injected ViewModels
    // Note: These will be scoped to the NavGraph if inside a navigation graph, 
    // or activity if retrieved at activity level. 
    // For simple app structure, instantiating here is okay but better to do it inside composable destinations.
    // However, since we share it across screens (Login -> List), lifting it up is fine,
    // OR we should retrieve the SAME instance.
    // By calling hiltViewModel() at this level, it's scoped to the activity/navhost parent.
    
    // Better practice: Scope ViewModels to the Navigation Graph.
    // Since we need to share data between Auth and List (well, actually we don't, 
    // AuthViewModel handles auth, PasswordViewModel handles data).
    // But Auth updates VaultManager state which PasswordViewModel depends on.
    
    // We can instantiate here.
    val authViewModel: AuthViewModel = hiltViewModel()
    val passwordViewModel: PasswordViewModel = hiltViewModel()

    val errorMessage by passwordViewModel.error.collectAsState()

    // Get AuthManager from AuthViewModel (it has it injected)
    // We'll need to expose it from AuthViewModel or create a separate injection
    // For now, let's create a simple approach - we'll pass authViewModel to SettingsScreen
    // and access authManager through it

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    // Initialize ViewModel data after authentication
                    passwordViewModel.initializeData()
                    navController.navigate(Screen.PasswordList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PasswordList.route) {
            PasswordListScreen(
                viewModel = passwordViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.PasswordList.route) { inclusive = true }
                    }
                },
                onNavigateToArchive = {
                    navController.navigate(Screen.Archive.route)
                },
                onNavigateToAddPassword = {
                    navController.navigate(Screen.AddPassword.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToEdit = { passwordId ->
                    navController.navigate(Screen.EditPassword.createRoute(passwordId))
                }
            )
        }

        composable(Screen.Archive.route) {
            ArchiveScreen(
                viewModel = passwordViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { passwordId ->
                    navController.navigate(Screen.EditPassword.createRoute(passwordId))
                }
            )
        }

        composable(Screen.AddPassword.route) {
            val passwordCount by passwordViewModel.passwords.collectAsState()
            val initialCount = remember { passwordCount.size }
            val saveAttempted = remember { mutableStateOf(false) }
            
            // Navigate back when a new password is successfully added
            LaunchedEffect(passwordCount.size) {
                if (saveAttempted.value && passwordCount.size > initialCount) {
                    navController.popBackStack()
                }
            }
            
            AddPasswordScreen(
                onBack = {
                    passwordViewModel.clearError()
                    navController.popBackStack()
                },
                onSave = { service, username, password, notes, tags ->
                    saveAttempted.value = true
                    passwordViewModel.addPassword(service, username, password, notes, tags)
                },
                errorMessage = errorMessage,
                onClearError = { passwordViewModel.clearError() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = passwordViewModel,
                authManager = authViewModel.getAuthManager(),
                onBack = { navController.popBackStack() },
                onNavigateToRecentlyDeleted = { navController.navigate(Screen.RecentlyDeleted.route) }
            )
        }

        composable(Screen.RecentlyDeleted.route) {
            RecentlyDeletedScreen(
                viewModel = passwordViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditPassword.route) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getString("passwordId")?.toLongOrNull()
            if (passwordId != null) {
                EditPasswordScreen(
                    passwordId = passwordId,
                    viewModel = passwordViewModel,
                    onBack = { navController.popBackStack() },
                    errorMessage = errorMessage
                )
            }
        }
    }
}
