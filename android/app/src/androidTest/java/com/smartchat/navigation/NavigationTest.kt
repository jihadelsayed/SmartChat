package com.smartchat.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartchat.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginNavigatesToRegistrationAndBack() {
        composeRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Create an account").performClick()
        composeRule.onNodeWithTag("register_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Back to login").performClick()
        composeRule.onNodeWithTag("login_screen").assertIsDisplayed()
    }
}
