# Stressless — Student Task Tracker

Stressless is an Android app (Kotlin + Jetpack Compose) built for South African
tertiary students to plan, prioritise, and track academic tasks and deadlines.

> **Demo video:** [add your unlisted YouTube link here]

## Features
- Email/password registration and login via Firebase Authentication (passwords
  are hashed server-side, never stored in plain text)
- Google Sign-In (SSO)
- Task management: create, edit, delete tasks with priority, category, due
  date, subtasks and progress tracking
- Photo attachments: a photo can be attached to any task. The image is
  compressed and stored as a Base64 blob directly inside the Firestore task
  document (blob storage), since the project uses Firebase's free Spark plan
  and doesn't have paid Cloud Storage
- Calendar view of tasks by date
- Statistics screen (completion rate, tasks by priority)
- Offline mode: tasks can be created/edited without a connection; changes
  queue locally and sync automatically to Firestore when back online
- Real-time push notifications via Firebase Cloud Messaging (topic
  subscription + per-device token registration)
- Local reminder notifications for upcoming due dates
- REST API integration: fetches a daily motivational quote from a public
  REST API (quotable.io), shown on the Dashboard
- Multi-language support: English, Afrikaans, isiZulu
- Dark mode and in-app settings
- Custom app icon and launcher assets

## Tech stack
- Kotlin, Jetpack Compose, Navigation Compose
- Firebase Authentication + Cloud Firestore
- Firebase Cloud Messaging
- Kotlin Coroutines
- JUnit (unit tests)
- GitHub Actions (CI — build and test on every push)

## Architecture
- `TaskRepository` — task state, auth, offline persistence and sync queue
- `FirestoreRepository` — Firestore CRUD layer (the app's cloud database/API layer)
- `RestApiRepository` — external REST API call for the daily quote
- `ImageUtils` — image compression and Base64 encode/decode for task photos
- `NotificationHelper` / `ReminderReceiver` — scheduled local reminder notifications
- `StresslessMessagingService` — handles incoming FCM push notifications

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
```
./gradlew test
```

GitHub Actions automatically builds and runs these tests on every push — see the **Actions** tab for status.

## Release Notes
See [RELEASE_NOTES.md](./RELEASE_NOTES.md) for a full list of changes made since the prototype.

## AI Tool Usage
See [AI_USAGE.md](./AI_USAGE.md) for a short write-up on how AI tools were used during this project.

## Screenshots
[Add app screenshots here]

## Developer
Developed by [your name / student number] — Stack Masters