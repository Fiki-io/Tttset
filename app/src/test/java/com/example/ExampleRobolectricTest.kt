package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screen.StudioScreen
import com.example.ui.theme.TDStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches TD Studio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TD Studio", appName)
    }

    @Test
    fun `studio screen adds textdraw and exports pawn code`() {
        composeTestRule.setContent {
            TDStudioTheme {
                StudioScreen()
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Project Baru").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("add_text_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("top_export_pawn_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("export_copy_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("a_samp Klasik").assertIsDisplayed()
    }
}
