package app.messagememory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.MediaEntity
import app.messagememory.data.db.entity.MessageEntity
import app.messagememory.data.db.entity.MessageType
import app.messagememory.util.Formatters

@Composable
fun MessageBubble(
    message: MessageEntity,
    media: MediaEntity?,
    showSender: Boolean,
    now: Long,
    onOpenMedia: () -> Unit,
) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (showSender && !isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                if (message.hasMedia) {
                    MediaPreview(message, media, onOpenMedia)
                }

                val bodyText = message.text
                if (bodyText != null) {
                    Text(text = bodyText, style = MaterialTheme.typography.bodyLarge)
                } else if (!message.hasMedia) {
                    Text(
                        text = captureStatusLabel(message.captureStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = Formatters.timestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val remaining = (message.expiresAt - now).coerceAtLeast(0)
        Text(
            text = "Archived ${Formatters.timestamp(message.capturedAt, now)} · ${Formatters.remainingLabel(remaining)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MediaPreview(message: MessageEntity, media: MediaEntity?, onOpenMedia: () -> Unit) {
    val isViewOnce = message.messageType == MessageType.VIEW_ONCE_IMAGE || message.messageType == MessageType.VIEW_ONCE_VIDEO
    val captured = media?.captureStatus == CaptureStatus.SUCCESS

    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .size(180.dp)
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = captured, onClick = onOpenMedia),
        contentAlignment = Alignment.Center,
    ) {
        when {
            captured && message.messageType in setOf(MessageType.IMAGE, MessageType.VIEW_ONCE_IMAGE) ->
                AsyncImage(
                    model = media?.localUri,
                    contentDescription = "Captured image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            captured && message.messageType in setOf(MessageType.VIDEO, MessageType.VIEW_ONCE_VIDEO) ->
                Icon(Icons.Filled.PlayCircle, contentDescription = "Play video", modifier = Modifier.size(48.dp))
            captured && (message.messageType == MessageType.AUDIO || message.messageType == MessageType.VOICE_NOTE) ->
                Icon(Icons.Filled.Mic, contentDescription = "Play audio", modifier = Modifier.size(48.dp))
            captured && message.messageType == MessageType.DOCUMENT ->
                Icon(Icons.Filled.Description, contentDescription = "Document", modifier = Modifier.size(48.dp))
            isViewOnce ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null)
                    Text(
                        text = "VIEW ONCE",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = androidx.compose.ui.Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = captureStatusLabel(message.captureStatus, isViewOnce = true),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, top = 2.dp),
                    )
                }
            else ->
                Text(
                    text = captureStatusLabel(message.captureStatus),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = androidx.compose.ui.Modifier.padding(8.dp),
                )
        }
    }
}
