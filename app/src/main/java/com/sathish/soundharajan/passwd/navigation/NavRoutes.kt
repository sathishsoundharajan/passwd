package com.sathish.soundharajan.passwd.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object PasswordList : Screen("password_list")
    object AddPassword : Screen("add_password")
    object Archive : Screen("archive")
    object Settings : Screen("settings")
    object RecentlyDeleted : Screen("recently_deleted")
    object EditPassword : Screen("edit_password/{passwordId}") {
        fun createRoute(passwordId: Long) = "edit_password/$passwordId"
    }
}
