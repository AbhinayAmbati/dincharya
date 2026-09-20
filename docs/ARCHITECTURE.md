# Architecture

## The core loop

```
   Plan ──────► Remind ──────► Observe ──────► Adapt
   │             │               │               │
 Add Task    ReminderWorker   EventLogger    AdaptationEngine
 (Room)      (WorkManager)    (task_events)  (rules over events)
                                                │
                                                ▼
                                     Suggestion card (Today screen)
                                     "Apply" → TaskRepository.rescheduleTask
```

Every layer exists to feed that loop.

## Layers (frontend)

| Layer | Package | Owns | Must NOT do |
|---|---|---|---|
| UI | `ui/` | Compose screens, ViewModels, theme | Talk to DAOs directly (go through `TaskRepository`) |
| Data | `data/` | Room entities, DAOs, `TaskRepository` | Know anything about Android UI |
| Learning | `learning/` | `EventLogger`, `AdaptationEngine` | Touch Room or Android framework APIs |
| Notifications | `notifications/` | Reminder scheduling + notification building | Contain business logic (delegate to the repository) |
| App | `app/` | `Graph` (service locator), `SettingsStore` | Grow beyond simple wiring |

Key decisions:

- **Local-first.** Room is the source of truth; the app has no INTERNET
  permission. The future backend only ever holds an encrypted mirror.
- **Pure-Kotlin learning.** `AdaptationEngine` takes plain lists in and
  returns value objects out, so it is unit-tested without Android and the
  future ML model can replace the rules behind the same signature
  (`evaluate(task, events) -> Adaptation?`).
- **Logging is inseparable from mutation.** Only `TaskRepository` writes to
  the DB, and every mutation also writes an event via `EventLogger`. A code
  path that changes a task without logging it is a bug.
- **Suggest, don't dictate.** The engine only produces `Adaptation` values;
  applying them always goes through the Today screen's Apply button, which
  writes a RESCHEDULED event (so the engine learns from its own moves).

## Reminder delivery

`ReminderScheduler` books a `OneTimeWorkRequest` (WorkManager) per task,
unique by task id, so re-scheduling replaces the old reminder and survives
reboots. `ReminderWorker` shows the notification with **Done** / **Snooze
10 min** actions; those buttons land in `ReminderActionReceiver`, which
calls the repository (goAsync keeps the process alive for the DB write).

## Screens

| Screen | Route | Purpose |
|---|---|---|
| Onboarding | `onboarding` | 3 intro pages + chronotype quiz (seeds the timing prior) |
| Today | `today` | Momentum, task sections, adaptation suggestion cards |
| Add Task | `add` | Fast capture: title, category, priority, duration, time |
| Insights | `insights` | Completion by time-of-day, by category, honest mirror |
| Focus | `focus` | Single-task countdown timer |
| Review | `review` | Evening close-out: 3 numbers + one reflection sentence |
| Settings | `settings` | Theme, reminders, privacy line, replay onboarding |

Navigation is a `NavHost` with sealed-class routes (`ui/navigation/Screen.kt`).

## Theming

Pure black-and-white Material 3 scheme (`ui/theme/`) with Noto Serif for
all text. **Status is never communicated by colour** — completed tasks use
a filled check circle and strikethrough, insights bars use length. This is
an accessibility requirement, not a style preference.
