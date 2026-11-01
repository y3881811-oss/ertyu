package com.ertyu.engine.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

object AudioPlayerManager {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** 连接 Service；已连接则直接回调 */
    fun connect(context: Context, onReady: () -> Unit = {}) {
        if (controller != null) {
            onReady()
            return
        }
        val appContext = context.applicationContext
        val token = SessionToken(
            appContext,
            ComponentName(appContext, AudioPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(appContext, token).buildAsync().also { future ->
            future.addListener({
                controller = future.get()
                onReady()
            }, MoreExecutors.directExecutor())
        }
    }

    /** 设置播放队列并开始播放 */
    fun play(context: Context, tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        connect(context) {
            controller?.let { c ->
                val mediaItems = tracks.map { track ->
                    MediaItem.Builder()
                        .setUri(track.uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .build()
                        )
                        .build()
                }
                // 必须在主线程
                c.setMediaItems(mediaItems, startIndex.coerceIn(0, tracks.lastIndex), 0L)
                c.prepare()
                c.play()
            }
        }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun pause() {
        controller?.pause()
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    data class AudioTrack(
        val uri: Uri,
        val title: String,
        val artist: String = ""
    )
}