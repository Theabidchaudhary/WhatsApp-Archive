# Message Memory

A local, temporary Android archive for WhatsApp conversations and media —
built entirely on Android's public `NotificationListenerService` API, with a
rolling 24-hour retention window. No root, no WhatsApp database access, no
bypass of WhatsApp's or Android's security/privacy protections, no cloud, no
accounts, no analytics, no ads, no network access at all.

Start here:

- **[`TECHNICAL_LIMITATIONS.md`](TECHNICAL_LIMITATIONS.md)** — Phase 1. What
  Android/WhatsApp notifications actually expose, what they don't, and why.
  Read this before the code — it explains the hard boundaries every other
  file respects (especially around View Once content and media capture).
- **[`ARCHITECTURE.md`](ARCHITECTURE.md)** — Phase 2. Data model, service
  architecture, storage/retention design, UI navigation map.
- **[`MANUAL_TEST_CHECKLIST.md`](MANUAL_TEST_CHECKLIST.md)** — Phase 6. A
  device test pass for Galaxy Note 10+ / Galaxy S25 Ultra, since notification
  payload behavior and Samsung background management can't be fully verified
  by unit tests alone.

## What it does

After granting Notification Access, the app watches for WhatsApp
(`com.whatsapp` / `com.whatsapp.w4b`) notifications and archives whatever
they legitimately expose — sender, message text, timestamps, and media when
WhatsApp itself attaches it to the notification. Because the archive is
populated the moment a notification is shown, text captured before a sender
uses "Delete for everyone" survives in the archive even though WhatsApp
later hides it — see `TECHNICAL_LIMITATIONS.md` §5 for exactly why that's
true and where it isn't (e.g. content WhatsApp never exposed, like View Once
media in the common case, is honestly reported as unavailable, never
fabricated).

Every captured item — message or media — carries its own absolute
`expiresAt` (capture time + 24h) and is deleted automatically on a rolling
basis, not in a daily batch. A user can manually **Save** any successfully
captured media to their own device storage (via MediaStore/SAF), which
creates a permanent copy independent of the 24h archive.

## Project layout

```
android/            Kotlin + Jetpack Compose app (Material 3, Room, WorkManager, Media3)
TECHNICAL_LIMITATIONS.md
ARCHITECTURE.md
MANUAL_TEST_CHECKLIST.md
```

## Building

```
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Requires the Android SDK (compileSdk/targetSdk 35, minSdk 26) and a JDK 17+.
This was developed and reviewed in an environment without Android SDK/Google
Maven access to actually execute a build — see the note in the project's
final PR/summary. Please run a real build (Android Studio or CI) before
relying on it, and treat `MANUAL_TEST_CHECKLIST.md` as required, not
optional, before any real-world use.

## Privacy

Everything is stored in app-private storage; the Room database is opened
via SQLCipher with a random, per-install passphrase sealed by an
Android-Keystore-backed key (`EncryptedSharedPreferences`) as defense in
depth. The app requests no `INTERNET` permission and has no networking code
— there is nothing to disable to make it "more private," this is the whole
design.
