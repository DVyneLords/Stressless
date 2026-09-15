# Release Notes

## v1.0.0 — Final Submission (Part 3)

This release builds on the Part 1/2 prototype with the following additions and fixes.

### New features added since the prototype

- **Photo attachments for tasks (blob storage)** — Tasks can now have a photo attached. The image is compressed and encoded as Base64, then stored directly inside the Firestore task document (`imageBase64` field), since the project uses Firebase's free Spark plan and doesn't have paid Cloud Storage access. This satisfies the blob storage requirement without needing a paid tier.
- **REST API integration** — Added `RestApiRepository`, which calls a public REST API (quotable.io) over HTTPS to fetch a daily motivational quote, displayed on the Dashboard.
- **Real-time push notifications** — Integrated Firebase Cloud Messaging. Devices subscribe to a shared `task_reminders` topic and register their FCM token to the user's profile document, so notifications can be sent from the Firebase Console or targeted server-side.
- **Offline mode with sync** — Added a local SharedPreferences-backed cache and pending-sync/pending-delete queues in `TaskRepository`. Changes made offline are saved locally and automatically pushed to Firestore once connectivity returns.
- **Single Sign-On (SSO)** — Added Google Sign-In on the Login screen, exchanging a Google ID token for a Firebase Auth session.
- **Multi-language support** — Added Afrikaans and isiZulu translations alongside English, switchable from Settings.
- **App icon** — Replaced the default Android launcher icon with a custom-designed logo (adaptive + legacy icon formats).

### Fixes and cleanup

- Fixed a missing `plugins {}` / `android {}` block in `app/build.gradle.kts` that was breaking Gradle sync entirely.
- Removed an unused `kotlin.sourceSets` conflict caused by an unnecessary KSP plugin that was never actually used for annotation processing.
- Updated `compileSdk` to 37 to match the minimum API level required by updated AndroidX/Navigation dependencies.
- Removed non-functional placeholder buttons ("Backup & Sync", "Export Data", second sign-in placeholder, "Forgot Password") that had no logic behind them.
- Fixed a corrected `TaskRepository.kt` where `imageBase64` was missing from serialization (`toJson()`/`toTaskLocal()`), which would have caused attached photos to be lost on local reload.
- Fixed a mismatch in `AboutScreen.kt`'s feature list, which listed five languages when only three are implemented.
- Added GitHub Actions workflow to automatically build and run unit tests on every push.

### Known limitations

- REST API quote fetch has no offline fallback message (fails silently if the request errors).
- "Forgot password" flow was removed rather than implemented — not required for this submission's core functionality.