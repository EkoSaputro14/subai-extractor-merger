package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatchWorkspaceRoute
import com.example.model.QueueUiEvent
import com.example.model.QueueUiState
import com.example.ui.screens.SubAiMviLinearBatchWorkspace
import com.example.ui.theme.SubAiExtractorTheme
import com.example.viewmodel.SubtitleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MviQueueStateMachineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mviStateMachine_transitionsAcrossAllSixStatesAndModals() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SubtitleViewModel(app)
        viewModel.onQueueEvent(QueueUiEvent.OnResetQueue)

        // 1. State.Empty
        assertTrue(viewModel.queueUiState.value is QueueUiState.Empty)

        composeTestRule.setContent {
            SubAiExtractorTheme {
                SubAiMviLinearBatchWorkspace(
                    viewModel = viewModel,
                    onSelectMultipleFiles = {},
                    onChooseOutputFolder = {},
                    onSaveMergedFile = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_state_empty_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Antrean Subtitle Kosong").assertIsDisplayed()

        // 2. Event.OnLoadSampleFiles -> PreFlightCheckValidator -> State.ReadyToStart
        composeTestRule.onNodeWithTag("mvi_load_sample_files_button").performClick()
        composeTestRule.waitForIdle()

        val readyState = viewModel.queueUiState.value
        assertTrue("Expected ReadyToStart but got $readyState", readyState is QueueUiState.ReadyToStart)
        val ready = readyState as QueueUiState.ReadyToStart
        assertEquals(4, ready.files.size)
        assertEquals(1250, ready.totalCues)
        assertEquals(100, ready.preFlightPassedPercent)
        composeTestRule.onNodeWithTag("mvi_state_ready_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("01 Shingeki_EP01.ass").assertExists()
        composeTestRule.onNodeWithText("Pre-flight: Lolos 100% ✓").assertExists()
        composeTestRule.onNodeWithText("Mulai Terjemahkan Batch (4 File)").assertIsDisplayed()

        // 3. Navigate through Core Screens 02, 03, 04 and Modals 02, 03, 04
        composeTestRule.onNodeWithTag("mvi_tab_extract").performClick()
        composeTestRule.waitForIdle()
        assertEquals(BatchWorkspaceRoute.CORE_EXTRACT, viewModel.activeWorkspaceRoute.value)
        composeTestRule.onNodeWithTag("mvi_core_extract_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pilih Preset Ekstraksi").assertExists()

        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.CORE_TRANSLATE))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_core_translate_screen").assertIsDisplayed()

        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.CORE_MERGE))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_core_merge_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audit Validasi Kelengkapan (7 Tahap)").assertExists()

        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_STORAGE))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_modal_storage_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kebijakan Konflik Nama Berkas").assertExists()

        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_GLOSSARY))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_modal_glossary_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daftar Istilah Terkunci (Glosarium)").assertExists()

        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mvi_modal_settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pilih Provider AI Aktif").assertExists()

        // Return to Queue Hub
        viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute)
        composeTestRule.waitForIdle()

        // 4. Event.OnPauseBatch -> State.Paused
        viewModel.onQueueEvent(QueueUiEvent.OnPauseBatch)
        composeTestRule.waitForIdle()
        assertTrue(viewModel.queueUiState.value is QueueUiState.Paused)
        composeTestRule.onNodeWithTag("mvi_state_paused_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("ANTREAN DIJEDA PENGGUNA").assertExists()

        // 5. Event.OnQueueFailed -> End-of-Queue Retry on attempts 1..4, then State.NeedsAttention on attempt 5
        viewModel.onQueueEvent(QueueUiEvent.OnCancelBatch)
        for (attempt in 1..4) {
            viewModel.onQueueEvent(
                QueueUiEvent.OnQueueFailed(
                    file = "Shingeki_EP04.srt",
                    error = "missingCueIds: [42, 43]",
                    missingCueIds = listOf(42, 43)
                )
            )
            // After attempts 1..4, batch is NOT halted into NeedsAttention; file is moved to end of queue
            assertTrue(viewModel.queueUiState.value is QueueUiState.ReadyToStart)
        }

        // On 5th retry failure -> triggers State.NeedsAttention & opens Modal_01_Inspection
        viewModel.onQueueEvent(
            QueueUiEvent.OnQueueFailed(
                file = "Shingeki_EP04.srt",
                error = "missingCueIds: [42, 43]",
                missingCueIds = listOf(42, 43)
            )
        )
        composeTestRule.waitForIdle()
        assertTrue(viewModel.queueUiState.value is QueueUiState.NeedsAttention)
        assertEquals(BatchWorkspaceRoute.MODAL_INSPECTION, viewModel.activeWorkspaceRoute.value)
        composeTestRule.onNodeWithTag("mvi_modal_inspection_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Galat Validasi: Shingeki_EP04.srt").assertExists()
        composeTestRule.onNodeWithTag("mvi_targeted_repair_button").assertExists()
        composeTestRule.onNodeWithText("Perbaiki Cue 42 & 43 Saja").assertExists()
    }
}
