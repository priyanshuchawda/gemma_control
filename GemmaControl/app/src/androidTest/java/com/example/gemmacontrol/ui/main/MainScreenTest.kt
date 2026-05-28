package com.example.gemmacontrol.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.gemmacontrol.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { 
      MainScreen(onItemClick = {}) 
    }
  }

  @Test
  fun testTitle_exists() {
    composeTestRule.onNodeWithText("GemmaControl").assertExists()
  }
}
