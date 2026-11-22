package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class BatchInstallState(
    val visible: Boolean = false,
    val items: List<FileItem> = emptyList(),
    val index: Int = 0,
    val error: String? = null
) {
    val total: Int get() = items.size
    val finished: Boolean get() = index >= items.size
}

object BatchInstallHolder {
    val state = MutableStateFlow(BatchInstallState())
}
