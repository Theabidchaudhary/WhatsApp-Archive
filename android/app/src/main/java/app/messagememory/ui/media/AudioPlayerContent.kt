package app.messagememory.ui.media

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** Archived audio / voice-message player (brief §13) — replayable indefinitely within the 24h window; opening never deletes the archive copy. */
@Composable
fun AudioPlayerContent(uri: String, title: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(0f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (true) {
            durationMs = exoPlayer.duration.coerceAtLeast(0).toFloat()
            positionMs = exoPlayer.currentPosition.toFloat()
            if (!isPlaying) break
            delay(200)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)

        Slider(
            value = if (durationMs > 0) positionMs.coerceIn(0f, durationMs) else 0f,
            onValueChange = { exoPlayer.seekTo(it.toLong()) },
            valueRange = 0f..(durationMs.takeIf { it > 0 } ?: 1f),
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(formatMillis(positionMs.toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatMillis(durationMs.toLong()), style = MaterialTheme.typography.labelSmall)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            listOf(1f, 1.5f, 2f).forEach { speed ->
                TextButton(onClick = { exoPlayer.setPlaybackSpeed(speed) }) {
                    Text("${speed}x")
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
