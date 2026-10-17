package com.ertyu

import com.ertyu.model.FileItem
import com.ertyu.model.VideoPlayerState

private val VIDEO_EXTS = setOf(
    "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts"
)

fun MainViewModel.openVideoPlayer(item: FileItem) {
    val path = item.uri.path ?: run {
        _toast.value = "无效路径"
        return
    }
    if (item.name.substringAfterLast('.', "").lowercase() !in VIDEO_EXTS) {
        _toast.value = "不支持的视频格式"
        return
    }
    VideoPlayerStateHolder.state.value = VideoPlayerState(
        visible = true,
        videoPath = path,
        videoName = item.name
    )
}

fun MainViewModel.closeVideoPlayer() {
    VideoPlayerStateHolder.state.value = VideoPlayerState()
}