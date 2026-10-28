package com.ertyu.model

data class DexViewerState(
    val visible: Boolean = false,
    val apkPath: String = "",
    val apkName: String = "",
    val dexName: String = "",
    val isLoading: Boolean = false,
    val classes: List<String> = emptyList(),
    val filter: String = "",
    val selectedClass: String? = null,
    val smaliContent: String = "",
    val isLoadingSmali: Boolean = false,
    val error: String? = null
) {
    val filteredClasses: List<String>
        get() = if (filter.isBlank()) classes
        else classes.filter { it.contains(filter, ignoreCase = true) }
}