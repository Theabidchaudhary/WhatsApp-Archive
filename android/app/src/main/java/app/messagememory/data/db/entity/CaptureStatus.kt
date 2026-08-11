package app.messagememory.data.db.entity

/**
 * Explicit, never-inferred capture outcome. Every message/media row carries
 * one of these; the UI renders directly from it rather than guessing from
 * nullability, so a missing field can never be silently presented as a
 * successful capture. See ARCHITECTURE.md §8.
 */
enum class CaptureStatus {
    SUCCESS,
    PARTIAL,
    UNAVAILABLE,
    FAILED,
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE_NOTE,
    DOCUMENT,
    VIEW_ONCE_IMAGE,
    VIEW_ONCE_VIDEO,
    SYSTEM,
    UNKNOWN,
}

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    VOICE_NOTE,
    DOCUMENT,
}

/** Why [MessageEntity.text] is null even though a message was captured. */
enum class PlaceholderReason {
    VIEW_ONCE,
    SUMMARY_ONLY,
    MEDIA_TYPE_NOT_TEXT,
    NONE,
}
