package com.ertyu

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
    AudioPlayerStateHolder.state.value = AudioPlayerState(
        visible = true,
        audioPath = path,
        audioName = item.name
    )
}

fun MainViewModel.closeAudioPlayer() {
    AudioPlayerStateHolder.state.value = AudioPlayerState()
}