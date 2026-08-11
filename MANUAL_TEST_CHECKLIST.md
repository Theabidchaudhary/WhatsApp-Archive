# Manual Test Checklist — Message Memory

Run this on a real device, not just an emulator — WhatsApp notification
behavior and Samsung's background management are both things emulators
model imperfectly at best. Target devices: **Galaxy Note 10+** and
**Galaxy S25 Ultra**, each on whatever WhatsApp build is current at test
time (record the exact WhatsApp version — its notification behavior is not
guaranteed stable across versions, per `TECHNICAL_LIMITATIONS.md`).

For each scenario, record: pass / fail / partial, the WhatsApp version, the
Android/One UI version, and — for anything involving media — exactly what
`captureStatus` the app recorded (visible in Settings → Diagnostics and in
the media viewer's own label). A "partial" result that matches the
documented limitation in `TECHNICAL_LIMITATIONS.md` is an expected pass, not
a bug; a "partial" result that *isn't* documented there is a real finding.

## Setup

- [ ] Fresh install, first launch shows Onboarding (not Dashboard).
- [ ] "Enable Notification Access" opens the correct system screen on this
      OEM/version.
- [ ] After granting access and returning to the app (no relaunch), the app
      moves itself off Onboarding automatically.
- [ ] Dashboard shows "● Monitoring Active".
- [ ] Revoke Notification Access from system settings, return to the app:
      dashboard reflects "○ Monitoring Disabled" without a restart.

## Core capture

1. [ ] Send a normal WhatsApp text message to the test device from another
       phone. Confirm a notification appears.
2. [ ] Open Message Memory without touching the WhatsApp notification.
       Confirm the conversation and message text appear, matching exactly.
3. [ ] From the sending device, **Delete for everyone** that same message.
       Confirm WhatsApp itself now shows "This message was deleted" — and
       confirm Message Memory's archived copy is untouched.
4. [ ] Send 5+ messages in rapid succession from one conversation. Confirm
       all are captured, in order, with no duplicates and no drops.
5. [ ] Send messages in two different conversations within a few seconds of
       each other (to exercise notification bundling). Confirm both
       conversations appear separately, not merged.
6. [ ] Send a message in a group chat. Confirm the sender's name is shown
       (not just "Group Name").

## Media

7. [ ] Send an image. Record whether `captureStatus` is `SUCCESS` or
       `UNAVAILABLE`/`PARTIAL` for this WhatsApp build — this determines
       whether the image preview/viewer should show real content or the
       honest "Media unavailable" state. Verify the UI matches whichever it
       actually is.
8. [ ] Send a video. Same check — expected per `TECHNICAL_LIMITATIONS.md`
       is `UNAVAILABLE` on current WhatsApp versions; flag if this device's
       WhatsApp build behaves differently.
9. [ ] Send a voice message. Same check as video.
10. [ ] Send a document. Same check.
11. [ ] Send a View Once photo, then a View Once video. Confirm the app
        never shows a fabricated "Archived" state — either the real content
        appears (only if the probe genuinely succeeded) or the exact honest
        message from the brief is shown: *"View Once content detected, but
        WhatsApp did not make the media available to this app."*
12. [ ] For any media that **was** captured: open it, back out, open it
        again — confirm opening never deletes or degrades the archived
        copy.
13. [ ] Save a captured image to device via the media viewer's Save button.
        Confirm it appears in the user's own gallery/Files app, independent
        of the app, and that it survives even after the archive copy
        expires (test this after step 15).
14. [ ] Share a captured image to another app. Confirm it opens/attaches
        correctly (this exercises the FileProvider path — a raw `file://`
        Uri would crash with `FileUriExposedException` if this regressed).

## Reliability

15. [ ] Reboot the phone. Without opening Message Memory first, send a new
        WhatsApp message. Confirm it's still captured (listener rebinds
        automatically per `TECHNICAL_LIMITATIONS.md` §9).
16. [ ] Force-stop Message Memory from system App Info, then reopen it.
        Confirm previously captured data is still present and new messages
        are still captured going forward.
17. [ ] Toggle Notification Access off then back on. Confirm capture
        resumes without needing a reinstall.
18. [ ] Enable Battery Saver / Data Saver and repeat a few of the Core
        Capture scenarios. Note any capture delay.
19. [ ] Samsung-specific: leave the app fully backgrounded (not force-
        stopped) for several hours with default battery settings, then send
        a message. Note capture latency. Repeat after granting
        "Unrestricted" battery usage via Settings → Background reliability.
        Record whether unrestricted battery usage measurably improves
        latency on this device/OneUI version.

## Retention

20. [ ] Note the exact capture time of a message. Confirm the conversation
        screen shows "Expires in ~23h 59m" immediately after capture.
21. [ ] Either wait for real 24h expiry, or (for faster iteration) use
        adb to fast-forward: temporarily edit a row's `expiresAt` via
        `adb shell run-as app.messagememory.debug` + sqlite3 against the
        (SQLCipher-encrypted) DB is not practical without the passphrase —
        instead prefer waiting the full window in a long-running test pass,
        or add a debug-only "expire everything now" hook if faster
        iteration is needed for CI.
22. [ ] After expiry, confirm: the message/media disappear from the UI, the
        conversation disappears if it had nothing else, and the underlying
        media file is actually gone from app-private storage (not just
        hidden from the UI) — check via Settings → Storage management's
        byte count dropping accordingly.
23. [ ] Confirm a manually-saved copy (step 13) is **not** affected by its
        source item's expiry.

## Search & filters

24. [ ] Search by contact name, by a word inside a captured message, and by
        "image"/"video" as filters — confirm results match expectations and
        respect the 24h window (nothing expired appears).

## Storage management

25. [ ] Settings → Storage management shows a byte total and per-type
        counts that match what's actually captured.
26. [ ] "Clear media but keep messages" removes media files and frees the
        reported storage, while message text remains browsable and no
        longer claims "Media captured" for the cleared items.
27. [ ] "Clear Archive Now" (with its confirmation dialog) empties
        everything immediately.

## Low storage

28. [ ] Fill the device's free space down to a few hundred MB (or use a
        low-storage test device) and repeat a media capture scenario.
        Confirm a failed media write results in `captureStatus = FAILED`
        with the message row still present — never a crash, never a lost
        message.

## Diagnostics

29. [ ] Settings → Diagnostics shows plausible, updating values for
        "Last notification received", "Last successful capture", and
        "Last cleanup" as you exercise the app — and never shows raw
        message content.
