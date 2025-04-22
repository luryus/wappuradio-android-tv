package com.lkoskela.wappuradio

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lkoskela.wappuradio.ui.theme.WappuradioTheme
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.transformation.blur.BlurTransformationPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    lateinit var player: Player
    lateinit var session: MediaSession

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        session.release()
        player.release()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")
        player = ExoPlayer.Builder(this).setMediaSourceFactory(
            ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this))
        ).build().apply {
            playWhenReady = true
        }
        session = MediaSession.Builder(this, player).build()

        setContent {
            WappuradioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), shape = RectangleShape
                ) {
                    MainScreen(
                        StreamDetails("https://stream1.wappuradio.fi/wappuradio.mp3"),
                        player,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

data class StreamDetails(val url: String)

@OptIn(UnstableApi::class)
@Composable
fun MainScreenContent(
    time: Long,
    nowPlaying: NowPlayingData?,
    currentProgram: Program?,
    loading: Boolean,
    playPauseButtonState: PlayPauseButtonState?,
    onSeekToLiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {

        currentProgram?.photo?.let { photoUrl ->

            CoilImage(
                imageModel = { photoUrl },
                modifier = Modifier.fillMaxSize(),
                component = rememberImageComponent { +BlurTransformationPlugin() }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background( Color.Black.copy(alpha = 0.8f) )
            )
        }

        Column(
            modifier = Modifier
                .padding(all = 50.dp)
                .fillMaxSize()
        ) {
            Text("Rakkauden Wappuradio.", style = MaterialTheme.typography.headlineLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                PlayPauseButton(playPauseButtonState)
                Text(formatTime(time))

                Button(onClick = onSeekToLiveClick) {
                    Text("Hyppää liveen")
                }

                if (loading) {
                    CircularProgressIndicator()
                }
            }

            currentProgram?.let {
                Row(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .weight(0.5f),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(end = 10.dp)
                    ) {
                        Text(
                            it.title,
                            style = MaterialTheme.typography.headlineMedium,
                        )

                        val timeString = remember(it.start, it.end) {
                            val timeZone = TimeZone.currentSystemDefault()
                            val timeFormatter = DateTimeFormatter.ofPattern("HH.mm")
                            "${timeFormatter.format(it.start.toLocalDateTime(timeZone).toJavaLocalDateTime())} - ${timeFormatter.format(it.end.toLocalDateTime(timeZone).toJavaLocalDateTime())}"
                        }
                        Text(timeString, style = MaterialTheme.typography.labelSmall)

                        val whitespaceRegex = remember { Regex("\\s+") }
                        val compactedDesc = remember(whitespaceRegex, it.desc) {
                            it.desc.replace(whitespaceRegex, " ")
                        }
                        Text(
                            compactedDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.0f, fill = false),
                            softWrap = true, overflow = TextOverflow.Ellipsis
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Äänessä", style = MaterialTheme.typography.titleMedium)
                            Text(it.host, style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Tuottaja", style = MaterialTheme.typography.titleMedium)
                            Text(it.prod, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    CoilImage(
                        imageModel = { it.photo },
                        modifier = Modifier.size(180.dp)
                    )
                }
            }

            nowPlaying?.let {
                Column(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 20.dp))

                    Text(
                        "NYT SOI",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.25.sp,
                        ),
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    Text(
                        it.song,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(stream: StreamDetails, player: Player, modifier: Modifier = Modifier) {
    val nowPlayingClient = remember { NowPlayingClient() }
    val currentProgramClient = remember { ProgramClient() }

    LaunchedEffect(player, stream) {
        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setTitle("Rakkauden Wappuradio")
                    .build()
            )
            .build()
        player.addMediaItem(mediaItem)
        player.prepare()
    }

    val timeFlow = remember(player) { player.getTimeFlow() }
    val time by timeFlow.collectAsStateWithLifecycle(0)
    val nowPlaying by nowPlayingClient.nowPlayingFlow.collectAsStateWithLifecycle(null)
    val currentProgram by currentProgramClient.programFlow.collectAsStateWithLifecycle(null)
    val loadingState = rememberPlayerLoadingState(player)
    val playPauseButtonState = rememberPlayPauseButtonState(player)

    MainScreenContent(
        time = time,
        nowPlaying = nowPlaying,
        currentProgram = currentProgram,
        loading = loadingState.loading,
        playPauseButtonState = playPauseButtonState,
        onSeekToLiveClick = { player.seekToDefaultPosition(0) },
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Preview(
    showBackground = true,
    showSystemUi = true,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1280px,height=720px,dpi=213"
)
@Composable
fun MainScreenPreview() {
    val prog = Program(
        "",
        Instant.DISTANT_PAST,
        Instant.DISTANT_FUTURE,
        "Preview Program",
        "Ransu, Eno-Elmeri",
        "Producer",
        "Lorem \n\nipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Lelietje-van-dalen_of_meiklokje_%28Convallaria_majalis%29._14-05-2021_%28actm.%29_01.jpg/960px-Lelietje-van-dalen_of_meiklokje_%28Convallaria_majalis%29._14-05-2021_%28actm.%29_01.jpg",
        "",
        Instant.DISTANT_FUTURE,
        ""
    )
    WappuradioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), shape = RectangleShape
        ) {
            MainScreenContent(
                time = 0L,
                nowPlaying = NowPlayingData("Preview Song"),
                currentProgram = prog,
                loading = true,
                playPauseButtonState = null,
                onSeekToLiveClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


fun Player.getTimeFlow() = flow {
    while (true) {
        delay(1000)
        emit(currentPosition)
    }
}

fun formatTime(time: Long): String {
    val seconds = (time / 1000) % 60
    val minutes = (time / (1000 * 60)) % 60
    val hours = (time / (1000 * 60 * 60)) % 24
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@OptIn(UnstableApi::class)
@Composable
fun PlayPauseButton(state: PlayPauseButtonState?, modifier: Modifier = Modifier) {
    IconButton(
        onClick = { state?.onClick() },
        modifier = modifier,
        enabled = state?.isEnabled ?: true
    ) {
        if (state?.showPlay != false) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_pause_24),
                contentDescription = "Pause"
            )
        }
    }
}

@Composable
fun rememberPlayerLoadingState(player: Player): PlayerLoadingState {
    val state = remember(player) { PlayerLoadingState(player) }
    LaunchedEffect(player) { state.observe() }
    return state
}

class PlayerLoadingState(private val player: Player) {

    var loading by mutableStateOf(false)
        private set

    suspend fun observe(): Nothing = player.listen { events ->
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
            loading = this.playbackState == Player.STATE_BUFFERING
        }
    }
}