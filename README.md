# Lantern

**Offline-first flood and earthquake guidance for the people who need it most.**

Lantern is an Android citizen app built for the **#BuildHybridAI 2026** contest. It answers
with no signal, using six human-reviewable safety cards and a grounded on-device model, and
it becomes a hybrid app when a connection returns: Firebase AI Logic can ask Gemini Nano 4
on the phone or Gemini in the cloud, and damage reports queue locally and sync to Firestore.

> **Safety first.** Every answer screen starts with
> “Call your local emergency number first if you can.”
> Lantern never produces medical doses or diagnoses. Every bundled card is marked
> **TO VERIFY** until a human checks it against public guidance.

---

## What works in Phase 2

- **Firebase AI Logic, hybrid.** Gemini Nano 4 is requested on-device with
  `InferenceMode.ONLY_ON_DEVICE` and `OnDeviceModelOption.PREVIEW`. The router decides when
  to escalate to a cloud model with `InferenceMode.ONLY_IN_CLOUD` (`gemini-3.8-flash`).
  The response's `inferenceSource` is logged, and the UI badge is driven by the router's
  `InferenceSource`.
- **Anonymous Firebase Auth.** A uid is created on first launch; reports are scoped to it.
- **Firestore, local-first.** Unlimited offline persistence. Every report is written to a
  local queue first, then to Firestore; it is removed from the queue only after the server
  acknowledges the write. A 30-second retry recovers after Auth/App Check are fixed without
  restarting the app. The sync badge is derived from `snapshot.metadata.hasPendingWrites`
  (QUEUED → SENT). Citizens can create reports and read only their own.
- **Remote Config, live.** `confidence_threshold`, `cloud_fallback_enabled`, `alert_level`,
  `emergency_numbers_json`, `cards_version` with safe in-app defaults and real-time updates.
- **App Check.** Debug provider for the judges' build, Play Integrity for release. App Check
  is installed before any AI Logic or Firestore call.
- **Versioned card pack.** Bundled JSON is the fallback; a newer `config/safety_cards`
  document is validated, cached and used. A malformed pack is ignored.
- **Offline queue that survives auth gaps.** If the very first launch is offline, reports
  are kept in a local queue until anonymous sign-in is possible, then written to Firestore.
- **Privacy.** `PiiScrubber` runs before every cloud prompt. Coordinates never go to a
  model. Drafts never leave the phone; a report leaves only when the person presses Send.
- **Tests.** 129 unit tests, 22 emulator-based Firestore rules tests, an always-on Firebase
  wiring test, and an opt-in airplane-mode offline flow test.
- **Real accuracy numbers.** 40 questions (20 Roman Urdu, 10 Urdu, 10 English) and 20
  reports are measured by `AccuracyReportTest`; the numbers are published below.

The UI, domain and presentation layers are unchanged from Phase 1. Only `data/`, `di/`, the
Application wiring, build files and the new Firebase rules/config files changed.

---

## Architecture

Strict clean architecture. Dependencies point inward; presentation and data never leak into
domain.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        presentation (Compose)                       │
│  Screens ── StateFlow<UiState> ── ViewModels ── sealed Events        │
│  LanternTheme (tokens) · Navigation (type-safe routes) · Previews    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ depends only on use cases
┌───────────────────────────────▼─────────────────────────────────────┐
│                             domain                                  │
│  Use cases: AskLantern, StructureReport, SubmitReport, Observe…     │
│  AI: AiRouter · LexicalSafetyCardRetriever · GroundedAnswerValidator│
│      PiiScrubber · SafetyGuard · TextNormalizer                     │
│  Models: SafetyCard, Answer, Report, ReportDraft, Alert, Routing…   │
│  Repository + inference interfaces (no Android, no Firebase)        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ interfaces implemented by
┌───────────────────────────────▼─────────────────────────────────────┐
│                               data                                  │
│  data/ai      FirebaseOnDeviceInference · FirebaseCloudInference    │
│               CloudTriageService · prompts · defensive JSON         │
│  data/remote  Auth · Reports · Alerts · Config · Cards · Location   │
│               Connectivity · Firestore DTOs · local draft/queue     │
│  data/appcheck  debug provider (debug) · Play Integrity (release)   │
│  data/asset   bundled safety_cards.json · DTOs + validated mapper   │
│  di/          Hilt modules; the only place implementations are bound│
└─────────────────────────────────────────────────────────────────────┘
```

### Routing policy (`AiRouter`, pure Kotlin, unchanged)

1. **No match** — retrieval score below `MINIMUM_MATCH_SCORE` (0.18): NoMatch answer with
   local emergency numbers. No model is called.
2. **On-device** — score ≥ `confidenceThreshold` (default 0.55) and Nano is ready: ask the
   on-device model for strict JSON, then validate card id, safety and grounding.
3. **Cloud fallback** — score below threshold, or the on-device answer is missing/invalid,
   and the device is online with cloud fallback enabled: scrub PII, then ask the cloud.
4. **Card text** — offline, cloud disabled, or cloud answer rejected: return the vetted
   card text unchanged.
5. Every outcome returns `Answer(card, text, InferenceSource)`.

---

## Firebase AI Logic (the contest-critical part)

**AI Logic is called here at runtime:**

- [On-device: `FirebaseOnDeviceInference.kt`](app/src/main/java/com/mahbub/lantern/data/ai/FirebaseOnDeviceInference.kt)
- [Cloud: `FirebaseCloudInference.kt`](app/src/main/java/com/mahbub/lantern/data/ai/FirebaseCloudInference.kt)
- [Cloud-only desk tasks: `CloudTriageService.kt`](app/src/main/java/com/mahbub/lantern/data/ai/CloudTriageService.kt)

### On-device: Gemini Nano 4, explicitly

```kotlin
Firebase.ai(backend = GenerativeBackend.googleAI())
    .generativeModel(
        modelName = "gemini-3.8-flash",          // required by the builder, unused on-device
        onDeviceConfig = OnDeviceConfig(
            mode = InferenceMode.ONLY_ON_DEVICE,  // the router owns escalation
            modelOption = OnDeviceModelOption.PREVIEW, // Nano 4, not the STABLE Nano 3/2
            maxOutputTokens = 320,
            temperature = 0.1f,
            candidateCount = 1,
        ),
    )
```

- Single-turn text only: no chat history is created or sent.
- The answer uses AI Logic **structured output**: a `@Generable` data class plus the
  `firebase-ai-ksp-processor` generates the schema, so the `cardId`/`answer` contract is
  enforced by the SDK instead of trusting a "please return JSON" prompt. The defensive
  `ModelJson` extractor remains for the cloud-only desk tasks.
- One retry on a model or schema error, then the exception reaches `AiRouter`, which falls
  back to cloud (if allowed) or the vetted card text.
- `response.inferenceSource` is logged as `LanternOnDevice: inferenceSource=ON_DEVICE`.
- The status flow maps `OnDeviceModelStatus` to the existing `NanoStatus` enum, so the
  Settings screen shows **Ready / Downloading / Not downloaded / Unavailable**. A
  downloadable model is fetched in the background; an unsupported device falls back
  gracefully.

### Cloud: `ONLY_IN_CLOUD`

```kotlin
Firebase.ai(backend = GenerativeBackend.googleAI())
    .generativeModel(
        modelName = "gemini-3.8-flash",
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_IN_CLOUD),
    )
```

- The question has already passed through `PiiScrubber` in `AiRouter`.
- The prompt contains only the scrubbed question and the retrieved card. No coordinates,
  uid, device id or exact address.
- The cloud answer uses the same `@Generable` schema and goes through
  `GroundedAnswerValidator` before the router accepts it. A drifting answer is rejected and
  the card text is shown instead.
- `response.inferenceSource` is logged as `LanternCloud: inferenceSource=IN_CLOUD`.

### Cloud-only desk tasks (Phase 3)

`CloudTriageService` provides urgency re-check, duplicate grouping and a situation brief.
It is not wired to the citizen UI; it is the seam for the responder Desk. It uses
`ONLY_IN_CLOUD`, scrubs every input and never sends coordinates.

### Dependency versions (verified against current docs)

| Artifact | Version | Notes |
| --- | --- | --- |
| `firebase-bom` | 35.2.0 | Auth 25.0.0, Firestore 27.2.0, Config 24.0.1, App Check 20.1.0 |
| `firebase-ai` | 18.2.0 | Hybrid API, `onDeviceExtension`, `inferenceSource` |
| `firebase-ai-ondevice` | 16.0.0-beta08 | Prompt API via ML Kit; **not** in the BoM |
| `firebase-ai-ksp-processor` | 17.0.0 | Generates the `@Generable` schema used by both paths |
| `com.google.gms.google-services` | 4.6.0 | |
| Kotlin / KSP | 2.4.0 / 2.3.14 | The on-device SDK ships Kotlin 2.4 metadata; AGP 9's built-in KGP is upgraded in `build.gradle.kts` |

The hybrid API is experimental, so the AI files opt in with
`@OptIn(PublicPreviewAPI::class)`.

---

## Firebase products

| Product | What it does in Lantern |
| --- | --- |
| **AI Logic** | On-device Gemini Nano 4 (`ONLY_ON_DEVICE`) and cloud Gemini (`ONLY_IN_CLOUD`); `inferenceSource` is logged |
| **Authentication** | Anonymous sign-in; `uid` scopes every report |
| **Firestore** | `reports`, realtime `alerts` + `warnings` feeds, `config/safety_cards`; unlimited offline cache; `hasPendingWrites` drives the sync badge |
| **Remote Config** | Live threshold, cloud fallback, alert level, emergency numbers and card version |
| **App Check** | Debug provider in debug, Play Integrity in release; installed before AI/Firestore |

---

## Project layout

```
app/src/main/java/com/mahbub/lantern/
├── domain/            pure Kotlin: models, AI, repositories, use cases
├── data/ai/           Firebase AI Logic adapters, prompts, defensive JSON, desk tasks
├── data/remote/       Firebase Auth, Firestore repos, Remote Config, cards, connectivity
├── data/appcheck/     debug / release App Check installers (variant source sets)
├── data/fake/         in-memory implementations kept for unit tests
├── di/                Hilt modules; real bindings live here
└── presentation/      Compose UI (unchanged from Phase 1)
app/src/main/assets/safety_cards.json     6 cards (3 flood, 3 earthquake)
app/src/main/assets/test/questions.csv    40 retrieval questions
app/src/main/assets/test/reports.csv      20 report-field cases
app/src/main/res/values*/strings.xml      EN, UR, Roman Urdu
app/src/test/                             124 JVM tests
app/src/androidTest/                      Firebase wiring + opt-in offline flow
firestore.rules                           security rules
firestore.indexes.json                    no composite indexes required
firebase.json / .firebaserc               emulator + deploy config
rules-tests/                              Node emulator rules tests (22)
```

---

## Setup

### 0. One-command debug setup (after `firebase login`)

```bash
npx --yes firebase-tools@15.37.0 login
./scripts/firebase_setup.sh
```

The script reads the current App Check debug token from logcat, registers it, deploys
`firestore.rules`, and prints the remaining console links. It never writes the token to the
repository.

### 1. Firebase project file

Download `google-services.json` from the Firebase console for the Android app whose package
is **`com.mahbub.lantern`** and place it at `app/google-services.json`.

It is git-ignored. Commit only the placeholder
[`app/google-services.json.example`](app/google-services.json.example).

> The code package (`namespace`) and the Firebase-registered `applicationId` are both
> `com.mahbub.lantern`, so `google-services.json` must use the same package name.

### 2. App Check

1. Firebase console → **Security → App Check**.
2. The debug build installs `DebugAppCheckProviderFactory` and prints the debug token to
   logcat:
   ```bash
   adb logcat -s DebugAppCheckProvider
   ```
3. Register that token under **Apps → Manage debug tokens**. The token is a secret: do not
   commit it, screenshot it into the repo, or ship it in release.
4. Release builds use `PlayIntegrityAppCheckProviderFactory`. Register the release app in
   Play Console / App Check before shipping.
5. In **App Check → APIs**, enforce App Check for **Firebase AI Logic** and **Cloud
   Firestore**.

The app installs App Check in `LanternApplication.onCreate()` before any AI or Firestore
call and requests a token immediately so the debug secret appears in logcat.

### 3. Anonymous Authentication

Firebase console → **Authentication → Sign-in method → Anonymous → Enable**.

If this is not enabled, `signInAnonymously()` returns `CONFIGURATION_NOT_FOUND`. The app
still runs offline (cards and reports work); reports wait in the local queue until auth is
possible.

### 4. Firestore rules

```bash
firebase deploy --only firestore:rules
```

Or run the emulator tests without touching production:

```bash
cd rules-tests
npm install
npm test
```

### 5. Remote Config keys

Create these parameters in **Remote Config** with the same values as
[`RemoteConfigDefaults.kt`](app/src/main/java/com/mahbub/lantern/data/remote/RemoteConfigDefaults.kt):

| Key | Type | Default |
| --- | --- | --- |
| `confidence_threshold` | Number | `0.55` |
| `cloud_fallback_enabled` | Boolean | `true` |
| `alert_level` | String | `WARNING` |
| `emergency_numbers_json` | String | `[{"label":"Rescue 1122","number":"1122",…}]` |
| `cards_version` | String | `1.0.0` |

The app calls `setDefaultsAsync` first, fetches and activates, then listens with
`addOnConfigUpdateListener`. Publishing a new value updates the running app without a
restart. It also re-fetches when connectivity returns.

### 6. AI Logic and Gemini Nano 4

1. Firebase console → **AI Services → AI Logic → Get started**; choose the **Gemini
   Developer API** provider.
2. The hybrid on-device API is experimental. To get **Gemini Nano 4** rather than Nano 3/2,
   enroll the test device in the **AICore Developer Preview** and keep
   `OnDeviceModelOption.PREVIEW` in `FirebaseOnDeviceInference.kt`.
3. On a device without AICore/Nano, the app shows **Unavailable on this device** in Settings
   and falls back to the cloud or the card text.

### 7. Optional: publish a newer card pack

Create `config/safety_cards` in Firestore:

```json
{
  "version": "1.1.0",
  "json": "{\"version\":\"1.1.0\",\"cards\":[ … same shape as safety_cards.json … ]}"
}
```

The app validates the pack, caches it, and uses it only if its version is newer than the
bundled `1.0.0-demo`. A bad pack is ignored. Set `cards_version` to the same value so the
console and the app agree.

---

## Build and run

Requirements: Android Studio with AGP 9.2+, JDK 17+, Android SDK 37, a device/emulator with
API 26+.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The installed application id is `com.mahbub.lantern`; the activity is
`com.mahbub.lantern.MainActivity`:

```bash
adb shell am start -n com.mahbub.lantern/com.mahbub.lantern.MainActivity
```

---

## Demo script

1. First launch: pick **English** (or اردو / Roman Urdu). The choice is stored locally.
2. Turn on **airplane mode** (or use the debug panel's simulated signal). Ask “Flood water
   rising” → the Answer screen shows the vetted card and the **On this phone** or **Card
   text** badge. No network is used.
3. **Report** → choose the type and people → Next → choose needs and place → Review report → Check and send. The confirmation says **Queued**.
4. Turn the signal back on. The queued report flips to **Sent** when Firestore acknowledges
   the write (`hasPendingWrites` becomes false).
5. Tap the **Lantern** title **7 times** → demo panel. Switch **Simulated signal** online and
   ask a low-confidence question to see the **Cloud** badge. The panel also shows the last
   `inferenceSource`, Nano status and the card version.
6. Change `confidence_threshold` in the Remote Config console and publish. The running app
   picks it up live; the routing decision changes on the next question.
7. Ask a question with no vetted card (for example a medical question) → NoMatch with local
   emergency numbers.

---

## Firestore data model

```
reports/{reportId}
  uid: string                 // request.auth.uid; rules require equality
  description: string         // 1..500
  language: EN | UR | ROMAN_UR
  type: FLOOD | EARTHQUAKE | LANDSLIDE | OTHER
  urgency: int 1..5
  peopleCount: int 1..99
  needs: [RESCUE | SHELTER | WATER | FOOD | MEDICAL | CLOTHING | POWER | OTHER]
  placeDescription: string
  location: { latitude, longitude, accuracyMeters?, placeLabel? } | null
  status: NEW | ASSIGNED | RESOLVED
  createdAt / updatedAt / sentAt: epoch millis

alerts/{alertId}                 // general information and all-clear updates
warnings/{warningId}             // active hazard warnings
  level: INFO | WARNING | URGENT
  title: string                  // { en, ur, roman } maps are also accepted
  message: string                // { en, ur, roman } maps are also accepted
  region: string
  createdAt: timestamp           // Firestore Timestamp; epoch millis/seconds accepted

responders/{responderId}      // not readable by the citizen client
config/safety_cards           // read-only to signed-in citizens
  version: string
  json: string                // full SafetyCardBundleDto
```

`syncState` is not stored. It is derived from Firestore snapshot metadata, so a report can
never claim to be Sent before the server acknowledged it.

### Alert and warning feeds

Both feeds are read in realtime and merged newest first. The Alerts tab has
**All / Alerts / Warnings** filters, so each collection can also be viewed on its own.

Create documents from the Firebase console (**Firestore → Start collection**) or with the
Admin SDK. The console example below uses the date type for `createdAt`:

```
alerts/flood-update-1
  level:     "WARNING"
  title:     "Water rising near Malir River"
  message:   "Move to higher ground. Do not drive through water."
  region:    "Karachi • Malir"
  createdAt: 2026-11-27T10:30:00Z      // Timestamp

warnings/flood-warning-1
  level:     "URGENT"
  title:     "Flash flood warning"
  message:   "Leave low-lying streets now and go to the nearest shelter."
  region:    "Karachi • Malir"
  createdAt: 2026-11-27T10:35:00Z      // Timestamp
```

A document appears at the top of the feed as soon as it is added or edited. Writes are
denied to the app; only the console/Admin SDK can publish.

---

## Privacy

- `PiiScrubber` removes emails, phones, CNIC numbers, coordinates, URLs, house/street
  identifiers and labelled names before any cloud prompt.
- Cloud prompts contain only the scrubbed question and the retrieved card. Exact
  coordinates are never sent to a model.
- Reports go to Firestore only when the person presses **Send**. Drafts are local-only.
- Reports store the anonymous uid; citizens cannot read another citizen's report.
- The App Check debug token and `google-services.json` are git-ignored. The example file has
  placeholders only.

---

## Tests

```bash
# 124 JVM tests: routing, safety, PII, use cases, ViewModels, new data helpers
./gradlew :app:testDebugUnitTest

# 22 Firestore emulator rules tests (Node)
cd rules-tests && npm install && npm test

# Always-on Firebase wiring test + opt-in airplane-mode offline flow
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.lanternLive=true
```

The live instrumented test requires the console setup above (App Check token registered,
Anonymous Auth enabled, rules deployed) and a device/emulator where airplane mode can be
toggled. It asks offline, submits a report offline, reconnects, waits for SENT, and reads the
document back from Firestore with `Source.SERVER`.

### Accuracy report (real numbers)

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*AccuracyReportTest*" --rerun-tasks --info
```

The summary is written to `app/build/reports/accuracy/summary.txt`.

| Metric | Result |
| --- | --- |
| Card match, top-1 (all 40) | **38/40 = 95.0 %** |
| Roman Urdu (20) | **20/20 = 100 %** |
| Urdu (10) | **10/10 = 100 %** |
| English (10) | **8/10 = 80 %** |
| Report type exact (20) | **18/20 = 90 %** |
| Urgency exact | **16/20 = 80 %** |
| Urgency within ±1 | **17/20 = 85 %** |
| People exact | **18/20 = 90 %** |
| Needs exact set | **13/20 = 65 %** |
| Individual need items | **27/28 = 96.4 %** |

Known misses, kept visible rather than tuned away:

- “the river is overflowing near our street” → no match (no flood keyword).
- “can we drink the tap water after the flood” → matched the rising-water card instead of
  the drinking-water card.
- `StructureReportUseCase` treats “water” as a drinking-water need even when the text means
  floodwater, so some reports over-report WATER.
- Urdu word-boundary handling misses “2 افراد” / “4 لوگ” people counts and the oblique
  “زلزلے” earthquake form.

These are domain-level limitations to fix in Phase 3; they are not hidden by the test set.

---

## Troubleshooting (the failures seen on the emulator)

| Symptom | Log line | Cause | Fix |
| --- | --- | --- | --- |
| Report stays **Queued**, no `reports` doc | `LanternReports: submit ... uid=false` or `LanternAuth: ... CONFIGURATION_NOT_FOUND` | Anonymous Auth is disabled | Authentication → Sign-in method → **Anonymous → Enable** |
| Firestore/AI calls rejected | `FirebaseAppCheck: Debug token was rejected by server` | Debug token not registered | `./scripts/firebase_setup.sh`, or register the token from logcat |
| Cloud answer falls back to card text | `LanternCloud: ... The Gemini API free tier is not available for this project` | The project has no Gemini API free tier | Enable billing at <https://aistudio.google.com/billing>, or use a project with a free tier |
| On-device answer falls back to card text | `LanternOnDevice: OnDeviceModelStatus=UNAVAILABLE` | Emulator/unsupported device, or Nano not downloaded/enrolled | Use a supported physical device and enroll in the AICore Developer Preview |
| Firestore `PERMISSION_DENIED` | `Report write rejected` | Rules not deployed or App Check token rejected | Deploy `firestore.rules` and register the token |

The on-device SDK is wired correctly. On a supported device the log shows
`onDeviceExtension=true` and `OnDeviceModelStatus=AVAILABLE`; on the Android emulator it
shows `UNAVAILABLE` because emulators do not ship AICore/Gemini Nano. That is a hardware
limitation, not a code fallback.

Diagnostics:

```bash
adb logcat -s LanternOnDevice LanternCloud LanternReports LanternAuth LanternApp DebugAppCheckProvider
```

---

## Contest checklist

- [x] **AI Logic called at runtime.** `FirebaseOnDeviceInference` and
  `FirebaseCloudInference` call `generateObject`/`generateContent` on a `GenerativeModel`;
  the on-device pass is `ONLY_ON_DEVICE` with `OnDeviceModelOption.PREVIEW` (Nano 4).
- [x] **Hybrid routing stays in the app.** The router owns escalation; on-device exceptions
  become cloud calls only when online and `cloud_fallback_enabled`.
- [x] **App Check.** Debug provider in debug, Play Integrity in release, installed before AI
  Logic/Firestore. Enforcement is a console step (below).
- [x] **Anonymous auth.** `FirebaseAuthRepository` signs in anonymously and stores the uid
  with each report.
- [x] **Offline persistence.** Firestore unlimited cache; `hasPendingWrites` drives
  QUEUED → SENT; drafts are local.
- [x] **Rules.** `firestore.rules` plus 22 emulator tests covering every table row.
- [x] **Remote Config.** Five keys, safe defaults, real-time updates.
- [x] **Privacy.** PII scrubbed before every cloud prompt; no coordinates to models.
- [x] **License visible.** [LICENSE](LICENSE), Apache 2.0.
- [x] **No secrets in the repo.** `app/google-services.json` and the debug token are not
  committed; `app/google-services.json.example` has placeholders.
- [x] **Test set + runner.** 40 questions, 20 reports, real accuracy printed.

### Still manual (console steps)

1. Register the App Check debug token printed by `adb logcat -s DebugAppCheckProvider`
   (or run `./scripts/firebase_setup.sh` after `firebase login`).
2. Enable **Anonymous** sign-in:
   <https://console.firebase.google.com/project/hackathon-258dd/authentication/providers>
3. Enable **AI Logic** with the Gemini Developer API provider.
4. Enforce App Check on **AI Logic** and **Cloud Firestore**:
   <https://console.firebase.google.com/project/hackathon-258dd/appcheck/apis>
5. Deploy `firestore.rules` (`./scripts/firebase_setup.sh` does this).
6. Create the five Remote Config keys (optional for the demo, required for live control).
7. Enroll the test device in the **AICore Developer Preview** for Gemini Nano 4:
   <https://developers.google.com/ml-kit/genai/aicore-dev-preview>
8. Configure **Play Integrity** for the release build.
9. Optional: publish `config/safety_cards` for a newer card pack.
10. If cloud answers are required, enable Gemini API billing for the project:
    <https://aistudio.google.com/billing>. The on-device path does not need billing.

The live instrumented test cannot pass until steps 1–3 are done; it currently fails at
anonymous sign-in with `CONFIGURATION_NOT_FOUND` on a project where Auth is not enabled.
The emulator cannot run Gemini Nano; test the on-device path on a supported physical
device.

---

## Status: done, still manual, Phase 3

### Done in Phase 2

- Firebase AI Logic on-device + cloud adapters with `@Generable` structured output,
  prompts, defensive JSON and one retry.
- Anonymous Auth, Firestore reports and realtime alerts/warnings, always-local-first queue with 30s retry,
  local draft/queue stores.
- Remote Config with real-time updates, versioned card pack loader, real connectivity.
- App Check installers for debug/release, Firestore persistence, network permission.
- Firestore rules + 22 emulator tests, 29 new JVM tests, instrumented wiring/offline tests.
- Real accuracy report.

### Still manual

The nine console steps above. The app is built to degrade gracefully while they are missing.

### Phase 3

- Responder Desk using `CloudTriageService` (urgency re-check, duplicate grouping, brief).
- FCM alert push and responder-authored alerts.
- Human sign-off workflow that replaces **TO VERIFY**.
- Fix the retrieval/structuring misses listed in the accuracy report.
- Play Integrity release rollout, crash reporting, remove the demo panel from release builds.
- Instrumented Compose UI tests on a device matrix and a TalkBack/RTL audit.

---

## License

Apache License 2.0. See [LICENSE](LICENSE).

**Starter template:** none. The project scaffold and UI were written for this repository;
no third-party starter template or boilerplate was used.

Lantern is a contest prototype. The bundled safety guidance must be reviewed by a qualified
human before it is used in a real emergency.
