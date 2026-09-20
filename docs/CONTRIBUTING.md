# Contributing to Dincharya

Thanks for working on this! The rules below exist so that someone who has
never seen the codebase can find things and trust them.

## Folder structure

- `frontend/` — the Android app. Everything Android lives here.
- `backend/` — the Express API (arrives Phase 5; see its README).
- `docs/` — architecture and this file.
- `.github/workflows/` — CI. Android CI builds a debug APK on every push.

Inside `frontend/`, packages follow layers (see `docs/ARCHITECTURE.md`):
`data/` (Room + repository), `learning/` (event logging + adaptation),
`notifications/` (reminders), `app/` (wiring), `ui/` (screens, one folder
per screen). New code goes in the layer that owns the concern — when in
doubt, put it in the LOWEST layer that has everything it needs.

## Comments: the two rules

1. **Every class and every public function gets a KDoc/JSDoc block** that
   explains *what and why*, not a restatement of the code. One sentence is
   often enough; add more when there is a decision worth remembering
   ("WorkManager instead of exact alarms because ...").

2. **Inline comments explain WHY, never WHAT.** Bad:
   `// increment i`. Good: `// floor to 07:00 — nobody wants a 5 AM nudge`.

A new developer should be able to answer "what does this do and why is it
like this?" for any file using only the file itself.

## Code style

- Kotlin official style; 4-space indent. Keep line length reasonable (~120).
- All user-facing strings go in `res/values/strings.xml` — no hardcoded UI
  text in composables (times and counts formatted in code are fine).
- Compose screens stay dumb: state down, events up. Business logic belongs
  in ViewModels or the repository — never inside a composable.
- Accessibility is a requirement, not a nice-to-have:
  - touch targets ≥ 44dp,
  - status is communicated by shape/weight/length, NEVER by colour alone,
  - every interactive element has a readable label for TalkBack.
- If you change the adaptation rules, the tests in
  `AdaptationEngineTest` are the spec — update rule and test together.

## Testing

```bash
cd frontend
bash ./gradlew testDebugUnitTest
```

Unit tests live in `frontend/app/src/test/`. The adaptation engine and any
future ML code MUST have tests — it is the component that acts on the
user's behalf.

## CI

`.github/workflows/android-build.yml` runs on every push: assembleDebug +
unit tests, and uploads the APK as a workflow artifact. A red CI means the
push is broken — fix it before continuing.

## Commit messages

Short imperative subject, optional body explaining WHY:
`Move snoozed tasks 2h earlier (was 1h; 1h barely escapes the bad window)`.
