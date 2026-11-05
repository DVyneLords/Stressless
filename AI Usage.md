# AI Tool Usage Write-Up

**Tool used:** Claude (Anthropic)

I used Claude throughout the final development phase of Stressless, mainly for debugging build errors, reviewing and completing feature code, and preparing submission documentation.

**Debugging Gradle and build errors.** A large part of my sessions involved fixing a broken Gradle sync. My `app/build.gradle.kts` was missing its entire `plugins {}` and `android {}` blocks, causing every dependency line to throw "Unresolved reference" errors. I pasted screenshots of the Build panel and the file contents, and Claude identified the file was incomplete (not a syntax typo) and provided the missing blocks as a find-and-replace. Later, an unused `com.google.devtools.ksp` plugin caused a `kotlin.sourceSets` conflict with Android's built-in Kotlin support; Claude traced this to the specific plugin declaration and had me remove it from both the root and app-level Gradle files. It also caught a `compileSdk` mismatch after an AAR metadata error listed dependencies requiring API 37.

**Code review and bug fixes.** I pasted my Kotlin source files (Login, Register, Task screens, repositories) for review. Claude flagged a genuine risk I hadn't noticed: I had uploaded a `Task.kt` file that appeared to duplicate every class already declared in `TaskRepository.kt`, which would have caused duplicate-class compile errors. It also caught several non-functional placeholder buttons ("Backup & Sync", "Export Data", an empty second sign-in button) left over from earlier iterations, and a mismatch between my About screen's feature list (which claimed five supported languages) and my actual `Strings.kt` implementation (three).

**Feature completion.** The brief required a REST API connection separate from Firestore. Claude wrote a small `RestApiRepository` object using `HttpURLConnection` and `org.json` (no new dependency needed) to fetch a daily quote from a public API, plus the Dashboard UI changes to display it, given as complete find-and-replace instructions specifying exact files and code blocks.

**App icon.** For replacing the default launcher icon with my own design, Claude walked me through Android Studio's Image Asset Studio (right-click `app` → New → Image Asset → Foreground Layer → my logo file) rather than writing code, since this is a built-in IDE tool.

**Documentation.** This README, the release notes, and this write-up were drafted by Claude based on the actual features and fixes made during our sessions, then reviewed and edited by me.

**What I did myself:** all architectural decisions (Firestore structure, offline-sync queue design, screen navigation flow), writing the original app before this final phase, testing every fix on my own device, and verifying that Claude's suggested code actually matched my project's existing patterns before accepting it.

**Reflection:** Claude was most useful for fast root-cause diagnosis of build failures I couldn't parse myself, and for catching bugs I'd have otherwise shipped (the duplicate Task class, the missing `imageBase64` serialization field). I made sure to understand each fix rather than paste blindly, since I'm responsible for explaining this code.

