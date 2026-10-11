package com.ertyu.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class InstalledApp(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystem: Boolean,
    val apkPath: String,
    val size: Long,
    val sizeText: String,
    val icon: Bitmap?
)

data class InstalledAppsState(
    val visible: Boolean = false,
    val apps: List<InstalledApp> = emptyList(),
    val filter: String = "",
    val showSystem: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredApps: List<InstalledApp>
        get() {
            val base = if (showSystem) apps else apps.filter { !it.isSystem }
            if (filter.isBlank()) return base
            val q = filter.lowercase()
            return base.filter {
                it.label.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q)
            }
        }
}