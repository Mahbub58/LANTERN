package com.softvinax.lantern.presentation.report

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportStatus
import com.softvinax.lantern.domain.model.SyncState
import com.softvinax.lantern.domain.usecase.GetLastKnownLocationUseCase
import com.softvinax.lantern.domain.usecase.GetReportDraftUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLanguageUseCase
import com.softvinax.lantern.domain.usecase.ObserveMyReportsUseCase
import com.softvinax.lantern.domain.usecase.ObserveReportUseCase
import com.softvinax.lantern.domain.usecase.SaveReportDraftUseCase
import com.softvinax.lantern.domain.usecase.StructureReportUseCase
import com.softvinax.lantern.domain.usecase.SubmitReportUseCase
import com.softvinax.lantern.testutil.MainDispatcherRule
import com.softvinax.lantern.testutil.awaitUntil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val online = MutableStateFlow(false)
    private val language = MutableStateFlow(AppLanguage.EN)

    private fun draft(id: String = "draft-1") = ReportDraft(
        id = id,
        description = "Roof cracked, two families trapped",
        language = AppLanguage.EN,
        type = EmergencyType.EARTHQUAKE,
        urgency = 4,
        peopleCount = 6,
        needs = setOf(Need.RESCUE),
        createdAt = 1L,
    )

    private fun report(id: String = "report-1") = Report(
        id = id,
        description = "Roof cracked, two families trapped",
        language = AppLanguage.EN,
        type = EmergencyType.EARTHQUAKE,
        urgency = 5,
        peopleCount = 6,
        needs = setOf(Need.RESCUE),
        placeDescription = "Block B",
        location = null,
        syncState = SyncState.QUEUED,
        status = ReportStatus.NEW,
        createdAt = 1L,
        updatedAt = 2L,
    )

    // ------------------------------------------------------------------ step 1

    @Test
    fun `step one rejects a blank description`() = runTest {
        val structure = mockk<StructureReportUseCase>()
        val saveDraft = mockk<SaveReportDraftUseCase>()
        val location = mockk<GetLastKnownLocationUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        every { observeConnectivity() } returns online
        every { observeLanguage() } returns language

        val vm = ReportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("prefill" to "")),
            structureReport = structure,
            saveDraft = saveDraft,
            getLastKnownLocation = location,
            observeConnectivity = observeConnectivity,
            observeLanguage = observeLanguage,
        )
        vm.onDescriptionChange("   ")

        vm.continueToReview()

        assertTrue(vm.state.value.blankError)
        coVerify(exactly = 0) { saveDraft(any()) }
    }

    @Test
    fun `step one structures and saves a valid draft then opens review`() = runTest {
        val structure = mockk<StructureReportUseCase>()
        val saveDraft = mockk<SaveReportDraftUseCase>()
        val location = mockk<GetLastKnownLocationUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        every { observeConnectivity() } returns online
        every { observeLanguage() } returns language
        coEvery { structure(any(), any(), any(), any()) } returns draft()
        coEvery { saveDraft(any()) } returns draft()

        val vm = ReportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("prefill" to "")),
            structureReport = structure,
            saveDraft = saveDraft,
            getLastKnownLocation = location,
            observeConnectivity = observeConnectivity,
            observeLanguage = observeLanguage,
        )
        vm.onDescriptionChange("Roof cracked, two families trapped")

        vm.events.test {
            vm.continueToReview()
            assertEquals(ReportEvent.OpenReview("draft-1"), awaitItem())
        }
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `step one prefill is loaded from the route`() = runTest {
        val structure = mockk<StructureReportUseCase>()
        val saveDraft = mockk<SaveReportDraftUseCase>()
        val location = mockk<GetLastKnownLocationUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        val observeLanguage = mockk<ObserveLanguageUseCase>()
        every { observeConnectivity() } returns online
        every { observeLanguage() } returns language

        val vm = ReportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("prefill" to "flood water rising")),
            structureReport = structure,
            saveDraft = saveDraft,
            getLastKnownLocation = location,
            observeConnectivity = observeConnectivity,
            observeLanguage = observeLanguage,
        )

        assertEquals("flood water rising", vm.state.value.description)
    }

    // ------------------------------------------------------------------ step 2

    @Test
    fun `step two loads the draft and clamps edits`() = runTest {
        val getDraft = mockk<GetReportDraftUseCase>()
        val submit = mockk<SubmitReportUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        coEvery { getDraft("draft-1") } returns draft()
        every { observeConnectivity() } returns online

        val vm = ReportReviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf("draftId" to "draft-1")),
            getDraft = getDraft,
            submitReport = submit,
            observeConnectivity = observeConnectivity,
        )

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals("draft-1", loaded.draft?.id)
        }

        vm.setUrgency(99)
        vm.setPeopleCount(0)
        vm.toggleNeed(Need.WATER)
        vm.setType(EmergencyType.FLOOD)

        val edited = vm.state.value.draft!!
        assertEquals(5, edited.urgency)
        assertEquals(1, edited.peopleCount)
        assertTrue(Need.WATER in edited.needs)
        assertEquals(EmergencyType.FLOOD, edited.type)
    }

    @Test
    fun `step two missing draft is reported`() = runTest {
        val getDraft = mockk<GetReportDraftUseCase>()
        val submit = mockk<SubmitReportUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        coEvery { getDraft("gone") } returns null
        every { observeConnectivity() } returns online

        val vm = ReportReviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf("draftId" to "gone")),
            getDraft = getDraft,
            submitReport = submit,
            observeConnectivity = observeConnectivity,
        )

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertTrue(loaded.notFound)
        }
    }

    @Test
    fun `step two send emits the new report id`() = runTest {
        val getDraft = mockk<GetReportDraftUseCase>()
        val submit = mockk<SubmitReportUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        coEvery { getDraft("draft-1") } returns draft()
        coEvery { submit(any()) } returns report()
        every { observeConnectivity() } returns online

        val vm = ReportReviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf("draftId" to "draft-1")),
            getDraft = getDraft,
            submitReport = submit,
            observeConnectivity = observeConnectivity,
        )
        vm.state.test { awaitUntil { it.draft != null } }

        vm.events.test {
            vm.send()
            assertEquals(ReportReviewEvent.OpenConfirmation("report-1"), awaitItem())
        }
    }

    // ------------------------------------------------------------------ step 3

    @Test
    fun `confirmation observes the submitted report`() = runTest {
        val observeReport = mockk<ObserveReportUseCase>()
        val observeMyReports = mockk<ObserveMyReportsUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        every { observeReport("report-1") } returns flowOf(report())
        every { observeMyReports() } returns flowOf(listOf(report()))
        every { observeConnectivity() } returns online

        val vm = ReportConfirmationViewModel(
            savedStateHandle = SavedStateHandle(mapOf("reportId" to "report-1")),
            observeReport = observeReport,
            observeMyReports = observeMyReports,
            observeConnectivity = observeConnectivity,
        )

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals("report-1", loaded.report?.id)
            assertEquals(1, loaded.reports.size)
            assertFalse(loaded.notFound)
        }
    }

    // ------------------------------------------------------------------ my reports

    @Test
    fun `my reports exposes the list and offline flag`() = runTest {
        val observeMyReports = mockk<ObserveMyReportsUseCase>()
        val observeConnectivity = mockk<ObserveConnectivityUseCase>()
        every { observeMyReports() } returns flowOf(listOf(report()))
        every { observeConnectivity() } returns online

        val vm = MyReportsViewModel(
            observeMyReports = observeMyReports,
            observeConnectivity = observeConnectivity,
        )

        vm.state.test {
            val loaded = awaitUntil { !it.isLoading }
            assertEquals(1, loaded.reports.size)
            assertFalse(loaded.isOnline)
            assertFalse(loaded.hasError)
        }
    }
}
