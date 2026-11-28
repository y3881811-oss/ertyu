package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class PermissionTarget(
    val path: String,
    val name: String,
    val isDirectory: Boolean
)

object PermissionHolder {
    val target = MutableStateFlow<PermissionTarget?>(null)
    val currentMode = MutableStateFlow<String>("")
}
