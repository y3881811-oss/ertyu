package com.ertyu.model

data class RecycleBinItem(
    val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val deleteTime: Long,
    val size: Long
)

data class RecycleBinState(
    val visible: Boolean = false,
    val items: List<RecycleBinItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)