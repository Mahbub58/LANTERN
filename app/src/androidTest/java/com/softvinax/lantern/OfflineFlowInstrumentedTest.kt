package com.softvinax.lantern

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.softvinax.lantern.data.ai.FirebaseCloudInference
import com.softvinax.lantern.data.ai.FirebaseOnDeviceInference
import com.softvinax.lantern.data.remote.AndroidConnectivityRepository
import com.softvinax.lantern.data.remote.FirebaseAuthRepository
import com.softvinax.lantern.data.remote.FirebaseConfigRepository
import com.softvinax.lantern.data.remote.FirebaseReportRepository
import com.softvinax.lantern.data.remote.FirebaseSafetyCardRepository
import com.softvinax.lantern.data.remote.LocalReportQueue
import com.softvinax.lantern.data.remote.ReportDraftStore
import com.softvinax.lantern.domain.ai.AiRouter
import com.softvinax.lantern.domain.ai.GroundedAnswerValidator
import com.softvinax.lantern.domain.ai.LexicalSafetyCardRetriever
import com.softvinax.lantern.domain.ai.PiiScrubber
import com.softvinax.lantern.domain.ai.SafetyGuard
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.SyncState
import com.softvinax.lantern.domain.usecase.AskLanternUseCase
import com.softvinax.lantern.domain.usecase.StructureReportUseCase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Firebase wiring smoke test. Always runs.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseWiringTest {

    @Test
    fun firebaseIsInitializedFromGoogleServicesJson() {
        assertNotNull(FirebaseApp.getInstance())
        assertNotNull(FirebaseFirestore.getInstance())
    }
}

/**
 * End-to-end offline flow against the real Firebase project.
 *
 * This test is opt-in because it needs:
 *   - the App Check debug token registered in the Firebase console,
 *   - Anonymous Auth enabled,
 *   - Firestore rules deployed,
 *   - an online first launch (anonymous sign-in),
 *   - a device/emulator where airplane mode can be toggled.
 *
 * Run it with:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.lanternLive=true
 *
 * On a physical device, use `adb shell cmd connectivity airplane-mode` or the
 * quick-settings tile; the test uses the same shell command.
 */
@RunWith(AndroidJUnit4::class)
class OfflineFlowInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requireLiveOptIn() {
        assumeTrue(
            "Set -Pandroid.testInstrumentationRunnerArguments.lanternLive=true to run",
            InstrumentationRegistry.getArguments().getString("lanternLive") == "true",
        )
    }

    @After
    fun restoreNetwork() {
        runCatching { setAirplaneMode(false) }
    }

    @Test
    fun askAndReportOfflineThenSyncToFirestore() = runBlocking {
        val auth = FirebaseAuthRepository(context)
        val connectivity = AndroidConnectivityRepository(context)
        val cards = FirebaseSafetyCardRepository(context, auth)
        val config = FirebaseConfigRepository(context, cards, connectivity)
        val validator = GroundedAnswerValidator(SafetyGuard())
        val router = AiRouter(
            retriever = LexicalSafetyCardRetriever(),
            onDeviceInference = FirebaseOnDeviceInference(),
            cloudInference = FirebaseCloudInference(validator),
            piiScrubber = PiiScrubber(),
            answerValidator = validator,
            safetyGuard = SafetyGuard(),
        )
        val ask = AskLanternUseCase(cards, config, connectivity, router)
        val reports = FirebaseReportRepository(
            authRepository = auth,
            connectivityRepository = connectivity,
            draftStore = ReportDraftStore(context),
            localQueue = LocalReportQueue(context),
        )

        // Anonymous auth needs one online moment. The app does this on launch;
        // the test does it explicitly so it can assert the uid.
        val uid = auth.ensureAnonymousUid()

        setAirplaneMode(true)
        withTimeout(30_000) { connectivity.isOnline.first { online -> !online } }

        val answer = ask("flood water rising", AppLanguage.EN)
        assertTrue("Offline answer must not be blank", answer.text.isNotBlank())

        val draft = StructureReportUseCase()(
            description = "Instrumented test: wall collapsed, two people trapped near the market",
            language = AppLanguage.EN,
        )
        val submitted = reports.submit(draft)
        assertEquals(SyncState.QUEUED, submitted.syncState)

        setAirplaneMode(false)
        val synced = withTimeout(90_000) {
            reports.observeReports().first { list ->
                list.any { it.id == submitted.id && it.syncState == SyncState.SENT }
            }
        }
        assertTrue(synced.any { it.id == submitted.id && it.syncState == SyncState.SENT })

        val document = Tasks.await(
            FirebaseFirestore.getInstance()
                .collection("reports")
                .document(submitted.id)
                .get(Source.SERVER),
            60,
            TimeUnit.SECONDS,
        )
        assertTrue("Report must exist on the server", document.exists())
        assertEquals(uid, document.getString("uid"))
    }

    private fun setAirplaneMode(enabled: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val state = if (enabled) "enable" else "disable"
        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand("cmd connectivity airplane-mode $state")
                .use { ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        }
        // Fallback for devices without the connectivity shell command.
        runCatching {
            instrumentation.uiAutomation
                .executeShellCommand(
                    "settings put global airplane_mode_on ${if (enabled) 1 else 0}",
                )
                .use { ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        }
        Thread.sleep(2_000)
    }
}
