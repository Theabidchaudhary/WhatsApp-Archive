package app.messagememory.notification

import app.messagememory.data.db.entity.CaptureStatus

/**
 * Pure decision table for a message's overall [CaptureStatus], kept
 * separate from [IngestPipeline] so the "never fake success" rules
 * (ARCHITECTURE.md §8) are directly unit testable.
 */
object IngestLogic {
    fun resolveMessageCaptureStatus(
        hasText: Boolean,
        hasMediaCandidate: Boolean,
        mediaCaptureStatus: CaptureStatus?,
    ): CaptureStatus {
        if (!hasMediaCandidate) {
            return if (hasText) CaptureStatus.SUCCESS else CaptureStatus.UNAVAILABLE
        }
        // A media candidate exists; its own probe result decides the rest.
        return when (mediaCaptureStatus) {
            CaptureStatus.SUCCESS -> if (hasText) CaptureStatus.SUCCESS else CaptureStatus.SUCCESS
            CaptureStatus.PARTIAL -> CaptureStatus.PARTIAL
            CaptureStatus.UNAVAILABLE -> if (hasText) CaptureStatus.PARTIAL else CaptureStatus.UNAVAILABLE
            CaptureStatus.FAILED -> if (hasText) CaptureStatus.PARTIAL else CaptureStatus.FAILED
            null -> if (hasText) CaptureStatus.SUCCESS else CaptureStatus.UNAVAILABLE
        }
    }
}
