package com.ertyu

import kotlinx.coroutines.flow.update

fun MainViewModel.toggleHidden(isLeft: Boolean) {
    val flow = flowOf(isLeft)
    flow.update { state ->
        state.copy(showHidden = !state.showHidden)
    }
    refresh(isLeft)
}