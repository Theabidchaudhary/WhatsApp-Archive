# Technical Limitations & Investigation

This document is Phase 1 of the Message Memory project. It states, honestly and
specifically, what Android's `NotificationListenerService` (NLS) exposes for
WhatsApp notifications on modern Android/One UI, what is *not* exposed, and
why. Every feature in the app is built to only claim success for what is
actually demonstrable through documented, public Android APIs. Where a claim
depends on WhatsApp's current notification implementation (which WhatsApp can
change at any time without notice, and which can vary by region/build/rollout),
it is flagged as **must be verified empirically on-device** rather than stated
as guaranteed.

Nothing described here reads WhatsApp's database, decrypts anything, requires
root, or touches WhatsApp's process. Everything is standard `NotificationListenerService`
behavior available to any app the user grants Notification Access to.

## 1. What `NotificationListenerService` receives

A bound, system-managed service (`NotificationListenerService`) receives:

- `onNotificationPosted(StatusBarNotification sbn)` — fired whenever a
  notification is posted **or updated**. WhatsApp does not create a new
  notification per message; it *updates* the existing notification for a
  conversation, so this fires repeatedly with a growing/changing payload for
  the same `(packageName, tag, id)` key.
- `onNotificationRemoved(StatusBarNotification sbn, RankingMap, int reason)` —
  fired when the notification is cleared from the shade (user opened the
  chat, swiped it away, or WhatsApp cancelled it). **This is not a deletion
  signal for message content** — see §5.
- `getActiveNotifications()` — a snapshot of everything currently posted,
  useful to reconcile state after the service (re)binds (e.g. after boot).

Each `StatusBarNotification.getNotification().extras` `Bundle` is where the
actual content lives. The listener only sees what the posting app
(WhatsApp) chose to put in that bundle — nothing more.

## 2. WhatsApp's notification style: `MessagingStyle`

WhatsApp has used `NotificationCompat.MessagingStyle` (the "chat bubble"
notification style) for its conversation notifications for several years on
both WhatsApp Messenger (`com.whatsapp`) and WhatsApp Business
(`com.whatsapp.w4b`). This is the single most important fact for this app,
because `MessagingStyle` notifications expose structured, per-message data,
not just a flat string:

- `Notification.EXTRA_MESSAGES` — a `Parcelable[]` of message bundles,
  reconstructible via `Notification.MessagingStyle.Message.getMessagesFromBundleArray(...)`.
  Each message carries:
  - `getText()` — the message body, as displayed.
  - `getTimestamp()` — when WhatsApp says the message arrived.
  - `getPerson()` — a `Person` with a display name and, frequently, an
    `Icon` (avatar) that can be loaded via `Person.getIcon().loadDrawable(context)`.
    This is the sender's WhatsApp/contact avatar as WhatsApp chose to attach
    it to the notification — a small bitmap, not a live profile fetch.
  - `getDataUri()` / `getDataMimeType()` — **set by WhatsApp for some media
    messages** (see §4). When present, this is a `content://` URI the
    listener is allowed to read (Android auto-grants URI read permission to
    a bound, active notification listener for URIs attached to a posted
    notification). When absent, there is no image/media payload to read —
    full stop, no workaround exists that doesn't involve reaching into
    WhatsApp itself, which is out of scope.
  - `Notification.EXTRA_CONVERSATION_TITLE` — the chat/group name.
  - `Notification.EXTRA_SUB_TEXT` / `EXTRA_SUMMARY_TEXT` — used for
    "N new messages" style summaries; **these are not message content** and
    must never be stored as if they were.
- `Notification.EXTRA_TITLE` / `EXTRA_TEXT` — the flattened fallback text
  for non-`MessagingStyle` consumers (e.g. Wear OS, older Android). Used only
  as a fallback when `EXTRA_MESSAGES` is absent.

**Practical consequence:** most individual and group text messages that
reach the notification tray are extractable as structured
(sender, text, timestamp) tuples, not just a flattened preview string. This
is the technical basis for §5 (surviving "Delete for everyone").

### What must be verified on-device (Note10+, S25 Ultra, current WhatsApp build)

- The **exact number of messages** WhatsApp retains in `EXTRA_MESSAGES`
  before truncating/summarizing older ones (historically capped; older
  messages beyond the cap fold into a summary line with no per-message
  text). The app must treat a summary line as "no content available", never
  invent text for it.
- Whether `getDataUri()` is populated for **image** messages on the current
  WhatsApp build (see §4) — this has changed across WhatsApp versions and
  must not be assumed stable.
- Exact avatar `Icon` availability per notification (WhatsApp sometimes
  supplies a generic group icon instead of a per-sender avatar for large
  groups).

## 3. Grouped, bundled, and updated notifications

- WhatsApp posts **one notification per conversation** (one-on-one or
  group), keyed by a stable `(tag, id)`. New messages in the same
  conversation cause `onNotificationPosted` to fire again on the *same* key
  with an updated `EXTRA_MESSAGES` array — not a new notification.
- On Android N+, WhatsApp also sets a group key so multiple conversations'
  notifications are visually stacked by System UI, and posts a **group
  summary notification** (`Notification.FLAG_GROUP_SUMMARY` set, or
  `NotificationCompat.isGroupSummary()` true). The summary carries no unique
  per-conversation content and must be filtered out during ingestion, or it
  will be misread as a new/duplicate conversation.
- **Deduplication strategy required:** because the same `(tag, id)` update
  can re-deliver messages we've already captured, ingestion must diff the
  incoming `EXTRA_MESSAGES` list against what's already stored for that
  conversation (keyed by sender + timestamp + text, since WhatsApp does not
  expose a stable per-message server ID through the notification) and only
  insert genuinely new entries.

## 4. Media: what's actually retrievable

| Content type | Notification-exposed? | Notes |
|---|---|---|
| Text message body | Yes, structured, via `EXTRA_MESSAGES` | High confidence, stable across recent WhatsApp versions. |
| Sender name / avatar | Yes, via `Person` | Avatar resolution is small/whatever WhatsApp attached; not the full-resolution profile photo. |
| Image message — inline preview | **Sometimes**, via `Message.getDataUri()`/`getDataMimeType()` when WhatsApp attaches image data to the notification | Must be verified per WhatsApp build. When absent, only the text placeholder (e.g. "📷 Photo") is available — archive the text, mark media `UNAVAILABLE`. Any resolution attached is whatever WhatsApp chose for the notification preview, not guaranteed original quality. |
| Video message | Generally **not** exposed as retrievable bytes | Notifications typically show a text placeholder only (e.g. "🎥 Video"). No documented `MessagingStyle` field WhatsApp uses to attach video bytes to a notification. Treat as `UNAVAILABLE` unless on-device testing shows otherwise for a specific build. |
| Voice message / audio | Generally **not** exposed as retrievable bytes | Same as video — placeholder text only in current, documented WhatsApp behavior. |
| Documents | Generally **not** exposed as retrievable bytes | Filename may appear in text; the file itself is not attached to the notification. |
| View Once photo/video | **Not exposed** by design | WhatsApp's placeholder text (e.g. "📷 Photo") appears without a usable `getDataUri()` in normal operation. This is consistent with WhatsApp's intent for View Once. The app must never attempt to defeat this — see §6. |

This table encodes the app's default assumptions; the app itself is written
defensively (§ Architecture) to probe `getDataUri()`/`getDataMimeType()` at
runtime rather than hard-code the table, so that if a given media type *is*
exposed on a particular device/build, the app captures it — and if not, it
honestly reports `UNAVAILABLE` instead of guessing.

## 5. Why archived text can outlive "Delete for everyone"

WhatsApp's "Delete for everyone" is a protocol-level message telling the
*recipient's WhatsApp app* to replace the message in its own local chat
history with a "This message was deleted" placeholder. It does not, and
cannot, reach back into Android's notification history or into any other
app's storage — Android does not let one app revoke a notification payload
it already handed to another app's bound listener.

So: if this app's listener already received and stored a message's text
*before* the deletion event happened, that stored copy is unaffected by the
deletion. This is not a bypass of anything — it's simply that Android showed
the content once, to an app the user explicitly granted Notification Access
to, before WhatsApp asked the recipient's own client to hide it locally.

**Edge case, stated honestly:** if a sender deletes a message fast enough
that WhatsApp never posts/updates the notification for it (e.g. deleted
within the same second, before the push notification is delivered and
processed), there is nothing to capture — Android never exposed it to any
listener. The app cannot and does not claim to recover messages it was never
shown.

## 6. View Once content — the hard boundary

Investigation conclusion: on current, documented WhatsApp behavior, View
Once photos/videos do not have their bytes attached to the
`MessagingStyle.Message` the way regular photo messages sometimes do. The
notification shows a placeholder only.

Per the project's explicit constraints, the app will **not**:
- read WhatsApp's database or shared storage looking for View Once media,
- hook, inject into, or instrument the WhatsApp process,
- use root, accessibility-service screen-scraping/screenshotting of
  WhatsApp's UI, or any method designed to defeat View Once's intended
  protection,
- use undocumented/hidden APIs.

Instead, the app's honest behavior is:
1. Detect that a View Once message occurred (WhatsApp's placeholder text is
   recognizable, e.g. contains "View once photo"/"View once video" wording).
2. Attempt the same legitimate `getDataUri()` probe used for regular media.
3. If data is present (would only happen if a future WhatsApp build changes
   behavior) — archive it, clearly labeled View Once, and never auto-delete
   it on "open" the way WhatsApp does (the whole point of this app is that
   opening the archived copy doesn't consume it).
4. If no data is present (the expected, current case) — store the message
   as `capture_status = UNAVAILABLE` with the exact user-facing text
   specified in the product brief: *"View Once content detected, but
   WhatsApp did not make the media available to this app."*

This is a hard product boundary, not just an implementation detail — the UI
layer enforces it too (see `ARCHITECTURE.md`, §UI states) so no code path can
present a fabricated "archived" state for content that was never received.

## 7. Notification removal ≠ message deletion

`onNotificationRemoved` fires when: the user opens WhatsApp (marks as read),
swipes the notification away, or the system/WhatsApp cancels it for any
other reason. None of these mean the message was deleted in WhatsApp, and
none of them should ever trigger deletion of archived content. The app's
*only* deletion trigger is the 24-hour rolling retention expiry (§ Retention
architecture) or an explicit user action ("Clear Archive Now" / "Delete from
archive").

## 8. Package identification

Do not hard-code an assumption that WhatsApp is installed. Identify targets
by checking installed packages via `PackageManager` against the known
package names:
- `com.whatsapp` (WhatsApp Messenger)
- `com.whatsapp.w4b` (WhatsApp Business)

and react to `onNotificationPosted`/`onNotificationRemoved` events whose
`sbn.getPackageName()` matches. If neither package is installed, the
dashboard reflects that plainly rather than silently doing nothing.

## 9. Samsung One UI background behavior

- `NotificationListenerService` is a system-bound service (like an
  Accessibility Service) — `NotificationManagerService` in `system_server`
  keeps it bound and will rebind it after process death, including after
  reboot, based on the listener being enabled in
  `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS`. No `RECEIVE_BOOT_COMPLETED`
  receiver is required for the listener itself to resume working.
- Samsung's One UI battery management (Adaptive Battery, "Sleeping apps",
  "Deep sleeping apps", and, on some versions, "Put unused apps to sleep")
  targets **ordinary app processes**, not the system's binding of an
  enabled notification listener. In practice, however, aggressive Samsung
  RAM/battery management has been widely reported (by users of similar,
  established notification-archiving apps) to delay listener rebinding or
  kill the app's process such that in-memory state (e.g. dedup caches) is
  lost between events — the architecture accounts for this by keeping all
  dedup/state in Room, not memory, so a process restart is harmless (see
  `ARCHITECTURE.md`).
- Recommended, and all this app does: **deep-link the user** to
  - Settings → Apps → Message Memory → Battery → **Unrestricted**
  - Settings → Battery and device care → Background usage limits → remove
    Message Memory from "Sleeping apps" / "Deep sleeping apps"
  - Settings → Notifications → Advanced settings → **Notification access**
    (Samsung's path differs from stock Android's Settings → Apps → Special
    app access → Notification access)

  The app never silently changes these settings — it only opens the
  relevant system screen via documented `Intent` actions
  (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`,
  `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, etc.) and explains why.
- No permanent foreground-service notification is used for listening itself
  (not required — the listener binding is already persistent). A foreground
  service is intentionally avoided per the product brief; this also avoids
  giving One UI's foreground-service battery accounting a reason to flag
  the app.

## 10. Storage & permissions actually required

- Capturing and archiving: **no storage permission needed.** All archive
  data (DB + media files) lives in app-private storage
  (`Context.filesDir` / `getExternalFilesDir`), which has been permission-free
  for the owning app since scoped storage (and was always accessible to the
  owning app even pre-scoped-storage).
- Manual "Save to device": uses `MediaStore` (`MediaStore.Images.Media`,
  `MediaStore.Video.Media`, `MediaStore.Downloads`, API 29+) or the Storage
  Access Framework (`ACTION_CREATE_DOCUMENT`) — neither requires a broad
  storage permission on API 29+, matching the "no unrestricted filesystem
  access" constraint in the brief.
- `POST_NOTIFICATIONS` (API 33+) is only requested if the app itself posts
  a user-visible notification (e.g. an optional "capture summary" or
  low-priority "monitoring active" status notification the user can turn
  off) — not required for the listener to function.
- **No `INTERNET` permission is requested.** The app has no network
  functionality by design (§19 of the brief) and Android will show "no
  internet access" for it in system UI, which is itself a user-visible
  privacy signal.
- `minSdk 26` (Android 8.0) is used as a broadly compatible floor —
  `MessagingStyle`, notification channels, and `Person`/`Icon` APIs are all
  available from API 26. `compileSdk`/`targetSdk 35` track current
  Note10+ (upgradeable to One UI 6) and S25 Ultra (ships One UI 7) software.

## 11. Summary of hard "cannot do" boundaries

These are permanent, not "not implemented yet":

- Cannot recover message content that was never posted to a notification
  (e.g. a message deleted before any notification was ever shown, or a
  conversation with notifications disabled/muted at the OS or app level).
- Cannot reliably recover original-resolution/original-quality media —
  only whatever WhatsApp itself attached to the notification, which may be
  downscaled or absent entirely.
- Cannot access View Once media unless WhatsApp itself changes its
  notification payload to include it (in which case the app already
  handles it correctly and honestly).
- Cannot function without the user granting Notification Access — this is
  a deliberate Android privacy gate, not a bug.
- Cannot guarantee zero-latency capture under aggressive OEM background
  management; the architecture is designed to be resilient to delayed
  delivery, not to defeat OS battery management.
