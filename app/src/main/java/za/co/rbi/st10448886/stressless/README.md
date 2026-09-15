# Stressless — Student Task Tracker

Stressless is an Android app (Kotlin + Jetpack Compose) built for South African
tertiary students to plan, prioritise, and track academic tasks and deadlines.

## Features
- Email/password registration and login via Firebase Authentication (passwords
  are hashed server-side, never stored in plain text)
- Google Sign-In (SSO)
- Task management: create, edit, delete tasks with priority, category, due
  date, subtasks and progress tracking
- Calendar view of tasks by date
- Statistics screen (completion rate, tasks by priority)
- Offline mode: tasks can be created/edited without a connection; changes
  queue locally and sync automatically to Firestore when back online
- Local reminder notifications for upcoming due dates
- Multi-language support: English, Afrikaans, isiZulu
- Dark mode and in-app settings

## Tech stack
- Kotlin, Jetpack Compose, Navigation Compose
- Firebase Authentication + Cloud Firestore
- Kotlin Coroutines
- JUnit (unit tests)

## Architecture
- `TaskRepository` — task state, auth, offline persistence and sync queue
- `FirestoreRepository` — Firestore CRUD layer (the app's cloud database/API layer)
- `NotificationHelper` / `ReminderReceiver` — scheduled local reminder notifications

## Running the project
1. Clone this repo.
2. Open in Android Studio.
3. Ensure `app/google-services.json` is present (your own Firebase config).
4. Run via the green Run button, or `./gradlew assembleDebug`.

## Testing
Unit tests: `app/src/test/java/za/co/rbi/st10448886/stressless/`
- `TaskRepositoryTest.kt` — task CRUD, notifications, subtask progress, string lookups
- `CalendarUtilsTest.kt` — date comparison helper

Run locally: