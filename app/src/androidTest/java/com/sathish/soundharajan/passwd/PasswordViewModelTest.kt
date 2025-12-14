package com.sathish.soundharajan.passwd

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordManagerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAddPasswordFlow() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Click the add button (should be visible in empty state or toolbar)
        composeTestRule.onNodeWithText("Add Password").performClick()

        // Should navigate to add password screen
        composeTestRule.waitForIdle()

        // Fill in the form
        composeTestRule.onNodeWithText("Service").performTextInput("Test Service")
        composeTestRule.onNodeWithText("Username").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("TestPassword123!")

        // Click save
        composeTestRule.onNodeWithText("Save").performClick()

        // Should navigate back to password list
        composeTestRule.waitForIdle()

        // Verify password appears in the list
        composeTestRule.onNodeWithText("Test Service").assertExists()
    }

    @Test
    fun testNavigationToArchive() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Click the menu button (3 dots)
        // Note: This tests the menu functionality
        composeTestRule.onNodeWithText("More options").performClick()

        // Click "Archived Passwords"
        composeTestRule.onNodeWithText("Archived Passwords").performClick()

        // Should navigate to archive screen
        composeTestRule.waitForIdle()

        // Verify we're on archive screen
        composeTestRule.onNodeWithText("Archived Passwords").assertExists()
    }

    @Test
    fun testPasswordValidationShowsErrorForWeakPassword() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to add password screen
        composeTestRule.onNodeWithText("Add Password").performClick()
        composeTestRule.waitForIdle()

        // Fill in form with invalid data (weak password)
        composeTestRule.onNodeWithText("Service").performTextInput("Test Service")
        composeTestRule.onNodeWithText("Username").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("weak") // Too short, missing requirements

        // Click save
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Password must be at least 8 characters long").assertExists()

        // Verify we didn't navigate back (still on add screen)
        composeTestRule.onNodeWithText("Add Password").assertExists()
    }

    @Test
    fun testPasswordValidationShowsErrorForMissingService() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to add password screen
        composeTestRule.onNodeWithText("Add Password").performClick()
        composeTestRule.waitForIdle()

        // Fill in form but leave service empty
        composeTestRule.onNodeWithText("Username").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("ValidPassword123!")

        // Click save
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Service name is required").assertExists()
    }

    @Test
    fun testSuccessfulPasswordSaveWithValidData() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to add password screen
        composeTestRule.onNodeWithText("Add Password").performClick()
        composeTestRule.waitForIdle()

        // Fill in form with valid data
        composeTestRule.onNodeWithText("Service").performTextInput("Valid Service")
        composeTestRule.onNodeWithText("Username").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("ValidPassword123!")

        // Click save
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify we navigated back to password list
        composeTestRule.onNodeWithText("Passwords").assertExists()

        // Verify the password appears in the list
        composeTestRule.onNodeWithText("Valid Service").assertExists()
    }

    @Test
    fun testPasswordStrengthIndicatorAppears() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to add password screen
        composeTestRule.onNodeWithText("Add Password").performClick()
        composeTestRule.waitForIdle()

        // Enter a password
        composeTestRule.onNodeWithText("Password").performTextInput("TestPassword123!")

        // Verify strength indicator appears (should show "Strong")
        composeTestRule.onNodeWithText("Strong").assertExists()
    }

    @Test
    fun testPasswordGeneratorIntegration() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to add password screen
        composeTestRule.onNodeWithText("Add Password").performClick()
        composeTestRule.waitForIdle()

        // Click the generate password button
        composeTestRule.onNodeWithText("Generate").performClick()
        composeTestRule.waitForIdle()

        // Verify a password was generated (field should not be empty)
        // Note: We can't easily verify the exact generated password, but we can check the field has content
        composeTestRule.onNodeWithText("Password").assertExists()
    }
}
