package com.athaya.printer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Regression test for the crash reported when opening NotaScreen: a
 * LazyColumn for the item list was nested inside a Column.verticalScroll()
 * (infinite height constraints from the parent scroll container) — same
 * root cause as the PrinterSettingsScreen crash. The fix replaced that
 * LazyColumn with a plain Column + forEach; this test just needs the
 * screen to compose successfully end to end (before any Preview/Print
 * interaction) to catch a regression back to a lazy list here.
 */
class NotaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersWithoutCrashingInsideAScrollableColumn() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    NotaScreen(onBack = {})
                }
            }
        }

        // "Tambah Item" is deliberately not asserted here: it appears twice
        // (a section label and the button text), which would fail
        // onNodeWithText's single-match requirement -- these labels are
        // each unique on first composition.
        composeTestRule.onNodeWithText("Cetak Nota").assertExists()
        composeTestRule.onNodeWithText("Nama Toko").assertExists()
        composeTestRule.onNodeWithText("Preview").assertExists()
        composeTestRule.onNodeWithText("Kembali").assertExists()
    }
}
