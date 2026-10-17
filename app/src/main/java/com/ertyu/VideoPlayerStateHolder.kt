package com.ertyu

import com.ertyu.model.VideoPlayerState
import kotlinx.coroutines.flow.MutableStateFlow

object VideoPlayerStateHolder {
    val state = MutableStateFlow(VideoPlayerState())
}