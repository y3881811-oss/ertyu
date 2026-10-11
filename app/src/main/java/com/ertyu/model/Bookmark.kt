package com.ertyu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Bookmark(
    val path: String,
    val name: String,
    val uriString: String
) {
    fun toUri(): Uri = Uri.parse(uriString)
}

@Immutable
data class RecentItem(
    val path: String,
    val name: String,
    val uriString: String,
    val lastAccess: Long
) {
    fun toUri(): Uri = Uri.parse(uriString)
}

data class BookmarkPanelState(
    val visible: Boolean = false,
    val sourceIsLeft: Boolean = true,
    val bookmarks: List<Bookmark> = emptyList(),
    val recents: List<RecentItem> = emptyList(),
    val showRecents: Boolean = false
)