package com.athaya.printer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Regression test for the crash reported on a Samsung S10: opening
 * PrinterSettingsScreen used to crash because a LazyColumn was nested
 * inside a Column.verticalScroll() (infinite height constraints from the
 * parent scroll container). The fix replaced that LazyColumn with a plain
 * Column + forEach; this test just needs the screen to compose
 * successfully end to end to catch a regression back to a lazy list here.
 */
class PrinterSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersWithoutCrashingInsideAScrollableColumn() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    PrinterSettingsScreen(onBack = {})
                }
            }
        }

        composeTestRule.onNodeWithText("Pengaturan Printer").assertExists()
        composeTestRule.onNodeWithText("Muat daftar printer (paired)").assertExists()
        composeTestRule.onNodeWithText("Kembali").assertExists()
    }
}
