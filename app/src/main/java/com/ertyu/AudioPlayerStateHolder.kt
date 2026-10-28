package com.ertyu

import com.ertyu.model.AudioPlayerState
import kotlinx.coroutines.flow.MutableStateFlow

object AudioPlayerStateHolder {
    val state = MutableStateFlow(AudioPlayerState())
}