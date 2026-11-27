package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(vm: MainViewModel) {
    val state by AudioPlayerStateHolder.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(state.audioPath))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }

    // 监听播放状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.toFloat().coerceAtLeast(1f)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // 更新进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPos = exoPlayer.currentPosition.toFloat()
            delay(500)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    BackHandler { vm.closeAudioPlayer() }

    // 均衡器
    val eqState by com.ertyu.engine.audio.EqualizerController.state.collectAsState()
    var showEq by remember { mutableStateOf(false) }

    // 恢复上次的倍速
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching {
            exoPlayer.setPlaybackSpeed(com.ertyu.engine.audio.AudioSpeedPrefs.get(appContext))
        }
    }

    if (showEq) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEq = false },
            title = { Text("均衡器") },
            text = {
                androidx.compose.foundation.layout.Column {
                    if (!eqState.supported) {
                        Text(
                            eqState.error ?: "此设备不支持均衡器",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("启用", modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(
                                checked = eqState.enabled,
                                onCheckedChange = {
                                    com.ertyu.engine.audio.EqualizerController.setEnabled(appContext, it)
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        com.ertyu.engine.audio.EqualizerController.presets.forEachIndexed { index, preset ->
                            androidx.compose.material3.FilterChip(
                                selected = eqState.presetIndex == index && eqState.enabled,
                                onClick = {
                                    com.ertyu.engine.audio.EqualizerController.setPreset(appContext, index)
                                },
                                label = { Text(preset.name) },
                                modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                            )
                        }
                        Text(
                            "均衡器作用于后台播放服务，切换耳机/扬声器会自动重挂。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEq = false }) { Text("关闭") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.audioName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { vm.closeAudioPlayer() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEq = true }) {
                        Icon(
                            Icons.Default.Equalizer,
                            contentDescription = "均衡器",
                            tint = if (eqState.enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val speedState = androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableIntStateOf(
                            com.ertyu.engine.audio.AudioSpeedPrefs.indexOf(
                                com.ertyu.engine.audio.AudioSpeedPrefs.get(appContext)
                            )
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        val speeds = com.ertyu.engine.audio.AudioSpeedPrefs.speeds
                        speedState.intValue = (speedState.intValue + 1) % speeds.size
                        val target = speeds[speedState.intValue]
                        runCatching { exoPlayer.setPlaybackSpeed(target) }
                        com.ertyu.engine.audio.AudioSpeedPrefs.set(appContext, target)
                        vm.showToast("倍速 ${target}x")
                    }) {
                        androidx.compose.material3.Text(
                            "${com.ertyu.engine.audio.AudioSpeedPrefs.speeds[speedState.intValue]}x"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // 封面占位
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                state.audioName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // 同名 .lrc 歌词（没有就不显示）
            LyricsBar(
                audioPath = state.audioPath,
                positionMs = currentPos.toLong(),
                onSeek = { ms -> exoPlayer.seekTo(ms) }
            )

            Spacer(Modifier.weight(1f))

            // 进度条
            Slider(
                value = currentPos.coerceIn(0f, duration),
                onValueChange = { exoPlayer.seekTo(it.toLong()) },
                valueRange = 0f..duration
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPos.toLong()), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(duration.toLong()), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            // 播放控制
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) }) {
                    Text("-10s", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.size(32.dp))
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.size(32.dp))
                IconButton(onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) }) {
                    Text("+10s", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}