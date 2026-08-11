package app.messagememory

import android.app.Application
import app.messagememory.di.AppContainer
import app.messagememory.retention.CleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MessageMemoryApp : Application() {
    val container by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CleanupWorker.schedulePeriodic(this)
        // Also sweep on every process start/resume (brief §6) so retention
        // stays accurate even if WorkManager's own schedule is deferred by
        // Doze/OEM background restrictions.
        appScope.launch { container.cleanupEngine.sweep() }
    }
}
