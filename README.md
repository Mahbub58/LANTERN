# Lantern

**Offline-first flood and earthquake guidance for the people who need it most.**

Lantern is an Android citizen app built for the **#BuildHybridAI 2026** contest. When the
signal is gone, Lantern still answers: six human-reviewable safety cards ship inside the
APK, retrieval runs on the phone, and a grounded on-device model (Gemini Nano in Phase 2)
answers only from those cards. When a connection returns, the same app can ask a cloud
model for a deeper answer and can sync damage reports that were queued offline.

> **Safety first.** Every answer screen starts with
> “Call your local emergency number first if you can.”
> Lantern never produces medical doses or diagnoses, and every bundled card is marked
> **TO VERIFY** until a human checks it against public guidance.

---

## What works in Phase 1

- Full Jetpack Compose UI, dark-only, Material 3, single activity, type-safe Navigation.
- First-launch language picker and in-app language switching: **English**, **اردو** (RTL),
  and **Roman Urdu**.
- Ask → Answer with source badges (on-device / cloud / card text), numbered steps,
  “never do this” warnings, and local emergency-number shortcuts when nothing matches.
- Three-step damage report flow: describe → review/edit → confirmation, with location
  opt-in and an offline queue that flips **QUEUED → SENT** when the debug panel goes online.
- Alerts list with offline-cached warnings, severity levels and saved-ago labels.
- Settings: language, emergency numbers, on-device model status, card version, anonymous ID.
- Hidden demo panel: tap the **Lantern** title on the Ask screen **7 times**.
- Every screen has loading / empty / error / offline states and `@Preview`s for those states.
- TalkBack content descriptions, ≥48 dp touch targets, RTL layouts.
- Domain layer is pure Kotlin: **no `android.*` imports**, no Firebase.

Everything runs on in-memory fakes. There is **no network call and no Firebase** in Phase 1.

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
│  Fake repositories (offline queue, alerts, config, auth, location)  │
│  Fake on-device + cloud inference (deterministic, grounded JSON)    │
│  Asset safety cards · DTOs + mappers · Hilt modules (swap points)   │
└─────────────────────────────────────────────────────────────────────┘
```

### Routing policy (`AiRouter`, pure Kotlin)

1. **No match** — retrieval score below `MINIMUM_MATCH_SCORE` (0.18): return a NoMatch
   answer with local emergency numbers. No model is called.
2. **On-device** — score ≥ `confidenceThreshold` (default 0.55) and Nano is ready: ask the
   on-device model for strict JSON, then validate card id, safety and grounding.
3. **Cloud fallback** — score below threshold, or the on-device answer is missing/invalid,
   and the device is online with cloud fallback enabled: scrub PII, then ask the cloud.
4. **Card text** — offline, cloud disabled, or cloud answer rejected: return the vetted
   card text unchanged.
5. Every outcome returns `Answer(card, text, InferenceSource)` so the UI can be honest
   about where the words came from.

`GroundedAnswerValidator` rejects wrong card ids, URLs, dosage/diagnosis language, and any
sentence whose vocabulary is not ≥60 % supported by the retrieved card.

---

## Project layout

```
app/src/main/java/com/softvinax/lantern/
├── domain/            pure Kotlin: models, AI, repositories, use cases
├── data/fake/         in-memory implementations used in Phase 1
├── di/                Hilt modules; each @Binds is a Phase 2 swap point
└── presentation/
    ├── theme/         Color.kt, Type.kt, Theme.kt, Locale.kt (tokens only)
    ├── components/    buttons, chips, cards, badges, states, top/bottom bars
    ├── navigation/    type-safe routes, NavHost, root gate
    ├── language/      first-run picker
    ├── ask/           Ask screen + ViewModel
    ├── answer/        Answer screen + ViewModel
    ├── report/        report flow, review, confirmation, my reports
    ├── alerts/        Alerts screen + ViewModel
    ├── settings/      Settings screen + ViewModel
    └── debug/         hidden demo panel
app/src/main/assets/safety_cards.json     6 cards (3 flood, 3 earthquake)
app/src/main/res/values*/strings.xml      EN, UR, Roman Urdu
app/src/test/                              JUnit4 + Turbine + MockK
```

Design tokens live only in `presentation/theme/Color.kt`; screens read `LanternTheme.colors`.
Midnight `#060A18`, amber→orange primary gradient `#FBBF24 → #F97316`, teal `#2DD4BF`
(on-device/offline), blue→indigo `#60A5FA → #818CF8` (cloud), coral `#FB7185` reserved for
urgency.

---

## Build and run

Requirements: Android Studio with AGP 9.2+, JDK 17+, Android SDK 37.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Install the debug APK on a device or emulator (minSdk 26):

```bash
./gradlew :app:installDebug
```

The app starts **offline on purpose** so the first demo proves that guidance works without
a signal.

### Two-minute demo script

1. Pick **English** (or اردو / Roman Urdu). The choice is stored on the phone.
2. On Ask, tap a quick chip or type “Flood water is entering my home”.
3. Answer shows the vetted card, an **On-device** badge and the TO VERIFY source line.
4. Open **Report**, describe damage, optionally share approximate location, review and send.
   Because the phone is offline, the confirmation says **Queued**.
5. Tap the **Lantern** title **7 times** → demo panel. Switch **Simulated signal** to online.
6. The queued report flips to **Sent** within a second; “My reports” reflects it.
7. Ask a question that needs more detail and use the blue **deeper cloud answer** button.
8. Try a question with no vetted card (e.g. a medical question) → NoMatch + emergency numbers.

---

## Tests

Unit tests cover the decision core and the UI state machines:

- `AiRouterTest` — all five outcomes: NoMatch, on-device, low-confidence cloud fallback,
  on-device-failure cloud fallback, card text (offline/disabled), plus deeper and rejection.
- `PiiScrubberTest`, `GroundedAnswerValidatorTest`, `SafetyGuardTest`,
  `LexicalSafetyCardRetrieverTest`.
- Use-case tests: `AskLanternUseCaseTest`, `StructureReportUseCaseTest`,
  `ReportUseCasesTest`, `ConfigUseCasesTest`.
- ViewModel tests with Turbine: Ask, Answer, Report (all four), Alerts, Language, Settings,
  Debug, Root.
- `FakeReportRepositoryTest` proves QUEUED → SENT on reconnect.

Run them with `./gradlew :app:testDebugUnitTest`.

---

## Status: done, faked, and Phase 2

### Done (real code)

- Clean architecture boundaries, Hilt wiring, type-safe navigation.
- `AiRouter` routing policy and all safety validators.
- Lexical retrieval in English, Urdu and Roman Urdu.
- Deterministic report structuring (`StructureReportUseCase`).
- Complete Compose UI, all states, previews, RTL and TalkBack labels.
- Offline report queue semantics and debug network simulation.
- 6 safety cards with EN/UR/Roman copy, source “TO VERIFY”, no doses.
- Unit tests listed above.

### Faked in Phase 1

| Area | Phase 1 | Phase 2 swap |
| --- | --- | --- |
| Safety cards | `FakeSafetyCardRepository` reads bundled JSON | Versioned download + local cache |
| On-device AI | `FakeOnDeviceInference` (800 ms, grounded JSON) | Gemini Nano via ML Kit / AICore |
| Cloud AI | `FakeCloudInference` (1200 ms, card-grounded) | Firebase AI Logic / Gemini API |
| Reports | `FakeReportRepository` in memory | Firestore offline persistence + Storage |
| Alerts | `FakeAlertRepository` seeded list | Firestore / FCM topic feed |
| Config | `FakeConfigRepository` in memory | DataStore + Remote Config |
| Auth | `FakeAuthRepository` anonymous id | Firebase Anonymous Auth |
| Location | `FakeLocationRepository` fixed demo point | FusedLocationProvider + cache |
| Connectivity | `FakeConnectivityRepository` starts offline | ConnectivityManager |
| Network simulation | `NetworkSimulator` in debug panel | Removed from release builds |

Each swap is a single `@Binds` line in `di/DataModule.kt` or `di/AiModule.kt`; presentation
and domain do not change.

### Phase 2 / not yet built

- Real Firebase project, security rules, offline persistence and FCM alerts.
- Responder dashboard and report triage.
- Human sign-off workflow that replaces **TO VERIFY** with a verifier name and date.
- Card authoring + versioning pipeline and localization QA.
- Instrumented Compose UI tests on a device matrix.
- Accessibility audit with TalkBack and large-font/RTL screenshot tests.
- Release signing, crash reporting, and removal of the demo panel from release builds.

---

## Safety and privacy notes

- Lantern is **not** a substitute for emergency services. The first line of every answer
  screen says to call the local emergency number first.
- No medical doses or diagnoses are generated or stored. `SafetyGuard` rejects them in
  on-device and cloud output.
- Free text is scrubbed on-device (`PiiScrubber`) before any cloud call: emails, phones,
  CNIC numbers, coordinates, URLs, house/street identifiers and labelled names are replaced.
- Reports leave the phone only when the person presses **Send**. Drafts stay local.
- Emergency numbers in the demo are placeholders; responders must verify them per region.

---

## License

Apache License 2.0. See [LICENSE](LICENSE).

Lantern is a contest prototype. The bundled safety guidance must be reviewed by a qualified
human before it is used in a real emergency.
