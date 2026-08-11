package app.messagememory.di

import android.content.Context
import app.messagememory.data.db.AppDatabase
import app.messagememory.data.files.MediaStorage
import app.messagememory.data.repo.ArchiveRepository
import app.messagememory.diagnostics.DiagnosticsRepository
import app.messagememory.notification.IngestPipeline
import app.messagememory.retention.CleanupEngine

/**
 * Manual DI container — deliberately no framework (Hilt/Koin) given the
 * project's size; a handful of `by lazy` singletons is simpler to read and
 * debug than generated DI code here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val mediaStorage: MediaStorage by lazy { MediaStorage(appContext) }
    val diagnosticsRepository: DiagnosticsRepository by lazy { DiagnosticsRepository(appContext) }

    val archiveRepository: ArchiveRepository by lazy {
        ArchiveRepository(database.conversationDao(), database.messageDao(), database.mediaDao(), mediaStorage)
    }

    val ingestPipeline: IngestPipeline by lazy {
        IngestPipeline(
            context = appContext,
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            mediaDao = database.mediaDao(),
            mediaStorage = mediaStorage,
            diagnostics = diagnosticsRepository,
        )
    }

    val cleanupEngine: CleanupEngine by lazy {
        CleanupEngine(
            database.conversationDao(),
            database.messageDao(),
            database.mediaDao(),
            mediaStorage,
            diagnosticsRepository,
        )
    }
}
