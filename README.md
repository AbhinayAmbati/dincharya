# Dincharya — the schedule that learns you

Dincharya is a local-first Android app that schedules your day, reminds you
of tasks — and learns from how you actually respond. Every completed,
snoozed or ignored task teaches the built-in rule engine when you really get
things done, and it reshapes your day around that reality.

**v1 is fully local**: no account, no backend, no INTERNET permission — the
app physically cannot send data anywhere. (The Express backend arrives in a
later phase; see `backend/README.md`.)

## Repository layout

```
dincharya/
├── frontend/     Android app (Kotlin + Jetpack Compose)
├── backend/      Placeholder for the Phase-5 Express API (TypeScript)
├── docs/         CONTRIBUTING and ARCHITECTURE
└── .github/      CI: builds the debug APK on every push
```

## Run the app

1. Open Android Studio → **Open** → select the `frontend/` folder.
2. Let Gradle sync (first sync downloads dependencies **and the Noto Serif
   fonts** — the repo itself is text-only; the fonts come from the official
   Noto fonts repository, pinned to an immutable commit).
3. Press **Run** on a device or emulator with Android 8.0+ (API 26).

Minimum requirements: JDK 17, Android Studio Koala (or newer).

Command line:

```bash
cd frontend
bash ./gradlew assembleDebug        # build the APK
bash ./gradlew testDebugUnitTest     # run the unit tests
```

The built APK lands in `frontend/app/build/outputs/apk/debug/app-debug.apk`.

## CI / CD

Every push to any branch runs `.github/workflows/android-build.yml`:
lint → unit tests → debug APK build, and the APK is uploaded as an artifact
to the workflow run (Retention: 14 days). Pushing to `main` therefore always
gives you an installable APK from the **Actions** tab — no signing setup
needed for debug builds.

### Pushing this repo to GitHub for the first time

```bash
cd dincharya
git init
git add .
git commit -m "Dincharya v0.1.0 — local-first scheduler with rule-based adaptation"
git branch -M main
git remote add origin git@github.com:<your-user>/dincharya.git
git push -u origin main
```

If `bash ./gradlew` complains about permissions locally on Linux/macOS, run
`chmod +x gradlew` once. (CI is unaffected — it invokes Gradle via `bash`.)

## How the learning works (in one paragraph)

Every task interaction writes an event (created / completed / snoozed /
ignored / rescheduled) with the hour of day into a local Room database. The
[`AdaptationEngine`](frontend/app/src/main/java/com/dincharya/app/learning/AdaptationEngine.kt)
then applies two rules: tasks snoozed three times get suggested 2 hours
earlier (never before 07:00), and tasks sitting in a historically weak time
window get suggested a move to your best completion hour. Suggestions are
always shown with their reason and can be accepted ("Apply") or declined
("Keep") — Dincharya suggests, it never dictates. Later phases replace the
rules with an on-device ML model behind the same interface.

Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full data flow
and [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) before changing code.

## Roadmap

- **v1 (this repo)** — scheduler, reminders, event logging, rule-based
  adaptation, focus mode, evening review, insights.
- **v2** — on-device logistic regression for reminder timing, realistic
  duration estimates, task breakdown assistant.
- **v3** — bandit reminder timing, location-aware reminders, burnout guard.
- **Phase 5** — Express backend: auth, encrypted backup, cross-device sync.

## License & notices

The bundled Noto Serif fonts are licensed under the SIL Open Font License —
see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
