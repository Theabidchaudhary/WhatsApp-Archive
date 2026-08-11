package app.messagememory.ui.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.MessageType
import app.messagememory.di.AppContainer
import app.messagememory.ui.components.captureStatusLabel
import app.messagememory.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(container: AppContainer, messageId: Long, onBack: () -> Unit) {
    val viewModel: MediaViewerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MediaViewerViewModel(messageId, container.archiveRepository, container.saveToDeviceExporter) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(messageId) { viewModel.markOpened() }

    val media = state.media
    val message = state.message

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { viewModel.saveToDevice(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(message?.senderName ?: "Media") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (media?.captureStatus == CaptureStatus.SUCCESS && media.localUri != null) {
                        IconButton(onClick = {
                            saveLauncher.launch(
                                container.saveToDeviceExporter.suggestedFileName(media.filename, media.mimeType, "bin"),
                            )
                        }) { Icon(Icons.Filled.Save, contentDescription = "Save to device") }
                        IconButton(onClick = {
                            ShareUtil.shareIntent(context, media.localUri, media.mimeType)?.let {
                                context.startActivity(android.content.Intent.createChooser(it, "Share"))
                            }
                        }) { Icon(Icons.Filled.Share, contentDescription = "Share") }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                message == null -> Text("Loading…")
                media == null || media.captureStatus != CaptureStatus.SUCCESS || media.localUri == null ->
                    UnavailableState(
                        text = captureStatusLabel(
                            media?.captureStatus ?: message.captureStatus,
                            isViewOnce = media?.isViewOnce ?: (message.messageType == MessageType.VIEW_ONCE_IMAGE || message.messageType == MessageType.VIEW_ONCE_VIDEO),
                        ),
                    )
                message.messageType == MessageType.IMAGE || message.messageType == MessageType.VIEW_ONCE_IMAGE ->
                    ZoomableImage(model = media.localUri, contentDescription = "Archived image")
                message.messageType == MessageType.VIDEO || message.messageType == MessageType.VIEW_ONCE_VIDEO ->
                    VideoPlayerContent(uri = media.localUri)
                message.messageType == MessageType.AUDIO || message.messageType == MessageType.VOICE_NOTE ->
                    AudioPlayerContent(uri = media.localUri, title = message.senderName)
                else -> UnavailableState(text = "This file type doesn't have a preview yet — use Share to open it in another app.")
            }
        }
    }
}

@Composable
private fun UnavailableState(text: String) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
