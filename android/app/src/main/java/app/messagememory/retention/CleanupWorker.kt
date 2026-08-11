package app.messagememory.retention

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.messagememory.MessageMemoryApp
import java.util.concurrent.TimeUnit

/**
 * Runs [CleanupEngine.sweep] on a periodic schedule. Placed on WorkManager's
 * platform floor (15 minutes) — the brief's 24h retention window tolerates
 * that granularity easily, and the UI additionally filters expired rows at
 * read time so nothing stale is ever visibly shown even if this worker is
 * deferred by Doze (ARCHITECTURE.md §5). No network constraint: cleanup
 * must run fully offline since everything is local.
 */
class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val engine = (applicationContext as MessageMemoryApp).container.cleanupEngine
        val result = engine.sweep()
        val output = workDataOf(
            "removedMessages" to result.removedMessages,
            "removedMedia" to result.removedMedia,
            "error" to result.error,
        )
        return if (result.error == null) Result.success(output) else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "message_memory_cleanup"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CleanupWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
