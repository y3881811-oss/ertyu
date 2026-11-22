package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(vm: MainViewModel) {
    val state by VideoPlayerStateHolder.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(buildVideoMediaItem(state.videoPath))
            prepare()
            playWhenReady = true
        }
    }

    // 2. 生命周期绑定
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // 3. 拦截返回键
    BackHandler {
        vm.closeVideoPlayer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.videoName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeVideoPlayer() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 自动加载同名字幕：video.mp4 旁边的 video.srt / .vtt / .ass 会被挂上。
 */
private fun buildVideoMediaItem(path: String): MediaItem {
    val file = java.io.File(path)
    val base = file.name.substringBeforeLast('.')
    val dir = file.parentFile
    val candidates = listOf(
        "srt" to androidx.media3.common.MimeTypes.APPLICATION_SUBRIP,
        "vtt" to androidx.media3.common.MimeTypes.TEXT_VTT,
        "ass" to androidx.media3.common.MimeTypes.TEXT_SSA,
        "ssa" to androidx.media3.common.MimeTypes.TEXT_SSA
    )
    val subs = ArrayList<MediaItem.SubtitleConfiguration>()
    candidates.forEach { (ext, mime) ->
        val f = java.io.File(dir, "$base.$ext")
        if (f.isFile && f.length() > 0) {
            subs.add(
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(f))
                    .setMimeType(mime)
                    .setLanguage("zh")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
            )
        }
    }
    val builder = MediaItem.Builder().setUri(android.net.Uri.fromFile(file))
    if (subs.isNotEmpty()) builder.setSubtitleConfigurations(subs)
    return builder.build()
}
