package ua.kpi.doctrack

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType
import ua.kpi.doctrack.ui.DocTrackApp
import ua.kpi.doctrack.ui.screens.RecordFormScreen
import ua.kpi.doctrack.ui.theme.DocTrackTheme

/** Checks user actions across Compose fields, callbacks, navigation and saved state. */
@RunWith(AndroidJUnit4::class)
class DocTrackUiTest {
    @get:Rule val compose = createComposeRule()

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun field(id: Int) = compose.onNode(hasSetTextAction() and hasText(text(id)))

    private fun click(id: Int) {
        compose.onNodeWithText(text(id)).performScrollTo().performClick()
    }

    @Test fun invalidFormShowsErrorsAndDoesNotSave() {
        var saved: DocumentRecord? = null
        compose.setContent {
            DocTrackTheme { RecordFormScreen(null, { saved = it }, {}) }
        }
        click(R.string.form_save)
        compose.onAllNodesWithText(text(R.string.form_error_required)).assertCountEquals(2)
        compose.runOnIdle { assertNull(saved) }
        field(R.string.form_title).performScrollTo().performTextInput("Test document")
        field(R.string.form_valid_until).performScrollTo().performTextInput("03.11.2026")
        click(R.string.form_save)
        compose.runOnIdle {
            assertEquals("Test document", saved?.title)
            assertEquals(RecordType.DOCUMENT, saved?.type)
        }
    }

    @Test fun billControlsProduceExactAmountCurrencyAndPaymentState() {
        var saved: DocumentRecord? = null
        compose.setContent {
            DocTrackTheme { RecordFormScreen(null, { saved = it }, {}) }
        }
        click(R.string.record_type_bill)
        field(R.string.form_title).performScrollTo().performTextInput("Test bill")
        field(R.string.form_pay_until).performScrollTo().performTextInput("03.11.2026")
        field(R.string.form_amount).performScrollTo().performTextInput("35,50")
        compose.onNodeWithText("UAH").performScrollTo().performClick()
        click(R.string.record_paid)
        click(R.string.form_save)
        compose.runOnIdle {
            assertEquals(3550L, saved?.amountMinor)
            assertEquals(RecordCurrency.UAH, saved?.currency)
            assertEquals(RecordType.BILL, saved?.type)
            assertTrue(saved?.isPaid == true)
        }
    }

    @Test fun discardConfirmationKeepsInputUntilConfirmed() {
        var cancelled = false
        compose.setContent {
            DocTrackTheme { RecordFormScreen(null, {}, { cancelled = true }) }
        }
        field(R.string.form_title).performScrollTo().performTextInput("Unsaved title")
        click(R.string.form_cancel)
        compose.onNodeWithText(text(R.string.form_discard_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.form_keep_editing)).performClick()
        field(R.string.form_title).assertTextContains("Unsaved title")
        compose.runOnIdle { assertFalse(cancelled) }
        click(R.string.form_cancel)
        compose.onNodeWithText(text(R.string.form_discard)).performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }

    @Test fun unsavedInputSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        var saved: DocumentRecord? = null
        restoration.setContent {
            DocTrackTheme { RecordFormScreen(null, { saved = it }, {}) }
        }
        field(R.string.form_title).performScrollTo().performTextInput("Restored draft")
        field(R.string.form_valid_until).performScrollTo().performTextInput("03.11.2026")
        restoration.emulateSavedInstanceStateRestore()
        field(R.string.form_title).performScrollTo().assertTextContains("Restored draft")
        click(R.string.form_save)
        compose.runOnIdle { assertEquals("Restored draft", saved?.title) }
    }

    @Test fun newRecordSurvivesNavigationAndStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { DocTrackTheme { DocTrackApp() } }
        compose.onNodeWithText(text(R.string.nav_server)).performClick()
        compose.onNodeWithText(text(R.string.server_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_records)).performClick()
        click(R.string.form_add_action)
        field(R.string.form_title).performScrollTo().performTextInput("Saved UI record")
        field(R.string.form_valid_until).performScrollTo().performTextInput("03.11.2026")
        click(R.string.form_save)
        // Opening the saved record verifies the shared list and the details route together.
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("Saved UI record"))
        compose.onNodeWithText("Saved UI record").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Saved UI record").assertIsDisplayed()
        click(R.string.form_edit_action)
        field(R.string.form_title).assertTextContains("Saved UI record")
    }
}
