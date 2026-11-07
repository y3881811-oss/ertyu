package com.ertyu

import kotlinx.coroutines.flow.MutableStateFlow

val _aboutVisible = MutableStateFlow(false)

fun MainViewModel.openAbout() {
    _aboutVisible.value = true
}

fun MainViewModel.closeAbout() {
    _aboutVisible.value = false
}