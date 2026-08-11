# Architecture — Message Memory

Phase 2 of the project. Builds directly on `TECHNICAL_LIMITATIONS.md` — read
that first; this document assumes its conclusions.

## 1. Module / package layout

Single Gradle module app (no need for multi-module complexity at this
scope) with clean internal package separation:

```
app/src/main/java/app/messagememory/
├── MessageMemoryApp.kt              Application, WorkManager Configuration.Provider
├── di/                              Manual DI container (no DI framework needed at this size)
│   └── AppContainer.kt
├── notification/                    Notification capture pipeline
│   ├── WhatsAppListenerService.kt   NotificationListenerService
│   ├── WhatsAppPackages.kt          Package identification (§8 of limitations doc)
│   ├── NotificationParser.kt        StatusBarNotification -> ParsedNotification (pure fn, unit-testable)
│   ├── MessagingStyleReader.kt      EXTRA_MESSAGES extraction helpers
│   ├── MediaProbe.kt                Defensive getDataUri()/getDataMimeType() probing
│   └── IngestPipeline.kt            ParsedNotification -> Room writes, dedup, expiry stamping
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── ConversationDao.kt
│   │   ├── MessageDao.kt
│   │   ├── MediaDao.kt
│   │   └── entities: ConversationEntity, MessageEntity, MediaEntity
│   ├── repo/
│   │   ├── ArchiveRepository.kt     Conversation/Message/Media reads+writes, Flow-based
│   │   └── SearchRepository.kt
│   ├── files/
│   │   └── MediaStorage.kt          App-private media dir, hashing/dedup, thumbnail cache
│   └── export/
│       └── SaveToDeviceExporter.kt  MediaStore/SAF "Save to device"
├── retention/
│   ├── RetentionPolicy.kt           24h rolling expiry calculation (pure fn, unit-testable)
│   ├── CleanupWorker.kt             WorkManager periodic worker
│   └── CleanupEngine.kt             Idempotent sweep: DB + files + thumbnails + orphans
├── permissions/
│   ├── NotificationAccessState.kt   Enabled-listener detection
│   └── SamsungBackgroundGuidance.kt Deep-link helpers to OEM settings screens
├── diagnostics/
│   └── DiagnosticsRepository.kt     Last event/capture/cleanup timestamps, DB/storage status
└── ui/
    ├── onboarding/
    ├── dashboard/                   Conversation list
    ├── conversation/                Chat detail screen
    ├── media/                       Image/audio/video viewers
    ├── search/
    ├── settings/                    Storage management, Samsung guidance, diagnostics
    ├── theme/
    └── navigation/
```

## 2. Data model (Room)

Matches the brief's schema, refined from the notification-payload realities
in `TECHNICAL_LIMITATIONS.md`.

```
ConversationEntity
  id: Long (autogenerate PK)
  whatsappConversationKey: String     // stable hash of (packageName, sbn.tag ?: sbn.key)
  title: String                       // EXTRA_CONVERSATION_TITLE or Person name for 1:1
  isGroup: Boolean
  profileIdentifier: String?          // Person.key/uri if WhatsApp supplies one; usually null
  avatarLocalUri: String?             // copy of Person icon, app-private storage
  sourcePackage: String               // com.whatsapp | com.whatsapp.w4b
  lastMessageTimestamp: Long
  createdAt: Long
  expiresAt: Long                     // = lastMessageTimestamp's message expiry; recalculated as rolling window advances
  unreadCount: Int
  messageCount: Int
  mediaCount: Int

MessageEntity
  id: Long (autogenerate PK)
  conversationId: Long (FK)
  dedupKey: String                    // hash(sender + timestamp + text) — see §4 dedup
  senderName: String
  senderIdentifier: String?           // Person.key if present
  isOutgoing: Boolean                 // true if sender matches the device owner's own WhatsApp identity when identifiable; else false/unknown
  text: String?                       // null when no content was ever exposed (placeholder-only)
  placeholderReason: String?          // e.g. "VIEW_ONCE", "SUMMARY_ONLY", "MEDIA_TYPE_NOT_TEXT" when text is null
  messageType: enum(TEXT, IMAGE, VIDEO, AUDIO, VOICE_NOTE, DOCUMENT, VIEW_ONCE_IMAGE, VIEW_ONCE_VIDEO, SYSTEM, UNKNOWN)
  timestamp: Long                     // as reported by the notification
  capturedAt: Long                    // device clock at ingest
  expiresAt: Long                     // capturedAt + 24h, absolute, rolling — never batch/midnight-based
  originalNotificationKey: String     // sbn.key, for correlating updates
  wasSeenDeletedInWhatsApp: Boolean   // set true if a later notification/placeholder implies WhatsApp now shows it as deleted (best-effort, informational only — see below)
  hasMedia: Boolean
  mediaId: Long?
  quotedText: String?                 // MessagingStyle historic message text used as a quote, when WhatsApp supplies one
  quotedSender: String?
  captureStatus: enum(SUCCESS, PARTIAL, UNAVAILABLE, FAILED)

MediaEntity
  id: Long (autogenerate PK)
  messageId: Long (FK)
  conversationId: Long (FK, denormalized for fast queries)
  type: enum(IMAGE, VIDEO, AUDIO, VOICE_NOTE, DOCUMENT)
  localUri: String?                   // app-private file:// URI; null if captureStatus != SUCCESS/PARTIAL
  mimeType: String?
  filename: String?
  sizeBytes: Long?
  durationMs: Long?
  contentHash: String?                // for dedup (§6)
  capturedAt: Long
  expiresAt: Long
  isViewOnce: Boolean
  isOpened: Boolean                   // archive-local "opened" flag — never affects retention; opening never deletes
  manuallySaved: Boolean
  savedUri: String?                   // MediaStore/SAF URI once user does Save to device
  captureStatus: enum(SUCCESS, PARTIAL, UNAVAILABLE, FAILED)
  captureStatusDetail: String?        // human-readable reason, surfaced in diagnostics/UI
```

Notes:
- `expiresAt` lives on every row independently and is **not** recomputed
  relative to "now" — it is stamped once at capture time
  (`capturedAt + 24h`) so retention is a true rolling window per item, per
  the brief's explicit requirement.
- `wasSeenDeletedInWhatsApp` is informational only (derived from WhatsApp
  itself later showing "This message was deleted" in a subsequent
  notification update for the same conversation, if that ever surfaces
  distinctly) — it is never used to trigger any deletion in the archive.
  Absence of this signal is the normal case, since WhatsApp typically just
  stops updating the notification rather than announcing the deletion.

## 3. Notification capture pipeline

```
StatusBarNotification (WhatsAppListenerService.onNotificationPosted)
  -> filter: package in {com.whatsapp, com.whatsapp.w4b}, not a group summary
  -> NotificationParser.parse(sbn): pure function -> ParsedNotification
       - reads EXTRA_CONVERSATION_TITLE / EXTRA_TITLE (conversation identity)
       - reads EXTRA_MESSAGES via MessagingStyleReader (list of ParsedMessage)
       - falls back to EXTRA_TEXT/EXTRA_TITLE flat text if EXTRA_MESSAGES absent
       - for each message with getDataUri()/getDataMimeType() set, runs
         MediaProbe to confirm the Uri is actually openable before claiming media
  -> IngestPipeline.ingest(parsed):
       - upsert Conversation by whatsappConversationKey
       - for each ParsedMessage: compute dedupKey; skip if a Message with that
         dedupKey already exists for this conversation (handles WhatsApp's
         re-post-with-growing-history update pattern, §3 of limitations doc)
       - stamp capturedAt = now, expiresAt = now + 24h
       - if media present: hand off to MediaStorage.persist() inside the same
         logical unit of work, but on a failure path that does NOT roll back
         the message insert — message and media capture are independent
         (brief requirement: "a failed media capture doesn't prevent the
         message from being archived")
       - update Conversation aggregates (lastMessageTimestamp, counts)
  -> onNotificationRemoved: update DiagnosticsRepository only; never deletes
     archived content (§7 of limitations doc)
```

`WhatsAppListenerService` itself stays thin — it delegates to
`IngestPipeline` on a `CoroutineScope` backed by `Dispatchers.IO`, so the
system callback returns quickly (required — NLS callbacks run on the main
thread and slow work risks ANRs / listener rebinding).

### Dedup key

`dedupKey = sha256(conversationKey + "|" + senderIdentifier_or_name + "|" +
timestamp + "|" + text)`. Timestamp + text + sender is the strongest
available signal since WhatsApp does not expose a stable per-message ID
through the notification API (§3 of limitations doc). Collisions are
acceptable-risk (astronomically unlikely for real chat content) and the
alternative — no dedup — would duplicate every message on every
notification update.

## 4. Storage architecture

- **App-private directory** (`context.filesDir/archive/media/<conversationId>/<mediaId>.<ext>`)
  for all captured media — never the public gallery, satisfying "do not
  place temporary archive files in the public gallery unless the user
  explicitly chooses Save."
- Thumbnails cached separately under `context.cacheDir/thumbs/`, so they can
  be purged independently and regenerated cheaply; cache entries carry the
  same `expiresAt` bookkeeping as their parent media so cleanup sweeps both
  together.
- **Sensitive-data-at-rest:** Room database and media files are written to
  app-private storage, which is sandboxed per-app by Android by default. As
  a defense-in-depth measure (brief §19), the Room database is opened via
  SQLCipher-style passphrase-protected `SupportFactory` backed by a key
  sealed in Android Keystore (`AndroidKeyStore` + `AES/GCM` wrap), so the
  DB file itself is not plaintext SQLite if the device is later
  rooted/backed-up outside the app sandbox. This is genuinely optional
  hardening the brief calls out as "consider... where practical" — it is
  not a substitute for, or claim about, defeating a compromised OS.
- **Save to device** never writes into the app-private tree — it always
  goes through `MediaStore` (API 29+ scoped storage) or SAF
  (`ACTION_CREATE_DOCUMENT`), producing a URI the user's own Files/Gallery
  app can see, fully independent of the 24h archive lifecycle. The archive
  copy still expires on schedule even after a manual save, since the manual
  save is explicitly a separate, permanent, user-owned copy per the brief.

## 5. Retention architecture

Rolling 24h absolute expiry, not "delete at midnight":

- `RetentionPolicy.expiryFor(capturedAtMillis) = capturedAtMillis + 24.hours`
  — pure function, unit tested directly against the brief's example
  (captured 14:32:10 → expires next day 14:32:10).
- `CleanupEngine.sweep(now)`:
  1. Query all `Message`/`Media` rows where `expiresAt <= now`.
  2. For expired `Media`: delete the local file, delete its cache thumbnail,
     then delete the DB row — file deletion happens before the DB row is
     removed, and the whole sweep runs inside a Room transaction per batch
     so a crash mid-sweep leaves either "not yet processed" or "fully
     processed" rows, never a dangling DB row pointing at a deleted file
     (idempotency: re-running the sweep against the same expired-but-not-
     yet-file-deleted row just retries the file delete, which is safe if
     the file is already gone).
  3. For expired `Message`: delete row (media already handled in step 2 if
     any).
  4. Recompute affected `Conversation` aggregates; delete a `Conversation`
     row if it has zero remaining messages and zero remaining media.
  5. Orphan sweep: any file under the media directory with no matching
     `Media.localUri` row (can happen if a crash occurred between file
     write and DB commit) is deleted — file writes happen to a temp name
     and are renamed into place only after the DB row commits, so an orphan
     is only ever a leftover temp file or a file whose DB row already
     expired and was cleaned; either way it's safe to delete.
- Triggers: a `WorkManager` `PeriodicWorkRequest` (15-minute minimum
  interval, the Android platform floor) plus an explicit `CleanupEngine.sweep()`
  call on process start / app resume (brief requirement: works even fully
  offline, since everything is local — no network constraint is placed on
  the worker).
- No item's actual visible removal is ever more than ~15 minutes late
  relative to its exact `expiresAt` (WorkManager's own scheduling
  granularity) — the UI additionally filters out anything `expiresAt <= now`
  at read time regardless of whether the physical sweep has run yet, so the
  user never *sees* stale-but-technically-expired content even if disk
  cleanup lags by a few minutes.

## 6. Duplicate media detection

When persisting media, `MediaStorage` computes a content hash
(`SHA-256`, streamed) plus `(sizeBytes, mimeType)`. If a byte-identical file
already exists for the *same conversation* within its retention window, the
new `MediaEntity` row points at the existing file (`localUri` shared) instead
of writing a second copy — satisfies "never retain unnecessary duplicate
copies" while keeping each `Message`/`Media` row logically independent
(their own `expiresAt`; the underlying file is only deleted once no
non-expired row references it — reference-counted delete in
`CleanupEngine`).

## 7. Service architecture & reliability

- `WhatsAppListenerService` requires zero foreground-service scaffolding —
  it's a system-bound service per `TECHNICAL_LIMITATIONS.md` §9. All mutable
  state needed for dedup/aggregation lives in Room, not in-memory fields, so
  process death (Samsung RAM management, Doze, force-stop-and-reopen) never
  loses state — the next `onNotificationPosted`/app-resume sweep just
  reconciles from `getActiveNotifications()` and the DB as normal.
- `CleanupWorker` is a `CoroutineWorker`; failures return `Result.retry()`
  (bounded by WorkManager's own backoff) rather than crashing, and every
  step inside `CleanupEngine` is wrapped so one bad row (e.g. unreadable
  file) doesn't abort the whole sweep — this satisfies "never lose the
  database because a media operation fails."
- All Room writes for message ingest and cleanup use `@Transaction`-annotated
  DAO methods to keep multi-table updates atomic.

## 8. UI states — never fake success

Every `captureStatus` value maps to one, and only one, UI treatment, sourced
directly from stored data, never inferred at render time:

| captureStatus | UI label |
|---|---|
| SUCCESS (message) | plain message bubble |
| SUCCESS (media) | "Media captured" |
| PARTIAL | message shown, media badge reads "Media unavailable" |
| UNAVAILABLE + View Once | "View Once content detected, but WhatsApp did not make the media available to this app." |
| UNAVAILABLE (other) | "Media unavailable" |
| FAILED | "Capture failed" with a diagnostics link |

The Settings → Diagnostics screen surfaces the same enums in aggregate
(counts per status) without exposing message text, per the brief's "don't
expose private content unnecessarily in diagnostics."

## 9. UI navigation map

```
Onboarding (first launch, shown until Notification Access granted)
  -> Dashboard (conversation list)
       -> Conversation detail
            -> Image viewer / Video player / Audio player (media items)
       -> Search
       -> Settings
            -> Storage management (usage, Clear Archive Now, Clear media but keep messages)
            -> Samsung background optimization guidance
            -> Diagnostics
            -> About / privacy explanation
```

Dashboard always shows the monitoring-state indicator
(`● Monitoring Active` / `○ Monitoring Disabled`) regardless of navigation
depth — implemented as a persistent top-bar affordance rather than a modal,
so the user is never confused about whether capture is currently happening.

## 10. What's deliberately NOT built

- No account system, no network stack beyond what's on-device (no
  `INTERNET` permission at all).
- No accessibility-service-based screen scraping of WhatsApp — the entire
  pipeline is notification-only, per the brief's hard boundary.
- No attempt to defeat FLAG_SECURE, no root checks/usage, no WhatsApp APK
  instrumentation.
