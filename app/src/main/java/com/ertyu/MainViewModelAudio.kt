package com.ertyu

import com.ertyu.engine.audio.AudioPlayerManager
import com.ertyu.model.AudioPlayerState
import com.ertyu.model.FileItem

private val AUDIO_EXTS = setOf(
    "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
)

fun MainViewModel.openAudioPlayer(item: FileItem) {
    val path = item.uri.path ?: run {
        _toast.value = "无效路径"
        return
    }
    if (item.name.substringAfterLast('.', "").lowercase() !in AUDIO_EXTS) {
        _toast.value = "不支持的音频格式"
        return
    }

    // 从左右面板收集音频，构成播放队列
    val allItems = (leftPanel.value.items + rightPanel.value.items)
        .distinctBy { it.uriString }
    val audioItems = allItems.filter {
        !it.isDirectory &&
            it.name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS
    }

    val tracks = audioItems.map {
        AudioPlayerManager.AudioTrack(
            uri = it.uri,
            title = it.name,
            artist = it.uri.path?.substringBeforeLast('/') ?: ""
        )
    }
    val startIndex = audioItems.indexOfFirst { it.uriString == item.uriString }
        .coerceAtLeast(0)

    AudioPlayerManager.play(getApplication(), tracks, startIndex)

    AudioPlayerStateHolder.state.value = AudioPlayerState(
        visible = true,
        audioPath = path,
        audioName = item.name
    )
}

fun MainViewModel.closeAudioPlayer() {
    AudioPlayerManager.pause()
    AudioPlayerStateHolder.state.value = AudioPlayerState()
}

/** 只收起播放界面，不停止播放（后台继续） */
fun MainViewModel.minimizeAudioPlayer() {
    val current = AudioPlayerStateHolder.state.value
    AudioPlayerStateHolder.state.value = current.copy(visible = false)
}