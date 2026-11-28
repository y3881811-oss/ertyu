package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class ManifestState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val apkName: String = "",
    val packageName: String = "",
    val xml: String = "",
    val permissions: List<String> = emptyList(),
    val error: String? = null
)

object ManifestHolder {
    val state = MutableStateFlow(ManifestState())
}
