package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.BookmarkStore
import com.ertyu.engine.SearchEngine
import com.ertyu.model.Bookmark
import com.ertyu.model.BookmarkPanelState
import com.ertyu.model.FileItem
import com.ertyu.model.RecentItem
import com.ertyu.model.SearchFilter
import com.ertyu.model.SearchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

// ==================== 搜索 ====================

fun MainViewModel.openSearch(isLeft: Boolean) {
    val rootPath = flowOf(isLeft).value.currentUri?.path ?: return
    _searchState.value = SearchState(
        visible = true,
        sourceIsLeft = isLeft,
        rootPath = rootPath
    )
}

fun MainViewModel.closeSearch() {
    searchJob?.cancel()
    searchJob = null
    _searchState.value = SearchState()
}

fun MainViewModel.updateSearchQuery(q: String) {
    _searchState.update { it.copy(query = q) }
}

/** 改过滤条件：已有结果就自动重搜，不用再点一次 */
fun MainViewModel.setSearchFilter(filter: SearchFilter) {
    _searchState.update { it.copy(filter = filter) }
    val s = _searchState.value
    if (s.query.isNotBlank() && !s.isSearching) startSearch()
}

fun MainViewModel.startSearch() {
    val state = _searchState.value
    if (state.query.isBlank()) return
    // 记一条搜索历史（去重，最多 20 条）
    com.ertyu.engine.SearchHistoryStore.add(getApplication(), state.query)
    searchJob?.cancel()
    _searchState.update {
        it.copy(
            isSearching = true,
            results = emptyList(),
            scannedDirs = 0,
            scannedFiles = 0,
            error = null,
            truncated = false
        )
    }

    val startedAt = System.currentTimeMillis()
    searchJob = viewModelScope.launch {
        try {
            val root = File(state.rootPath)
            val (results, truncated) = SearchEngine.search(
                root, state.query, state.filter
            ) { dirs, files, list ->
                _searchState.update {
                    it.copy(
                        scannedDirs = dirs,
                        scannedFiles = files,
                        results = list
                    )
                }
            }
            _searchState.update {
                it.copy(
                    isSearching = false,
                    results = results,
                    truncated = truncated,
                    elapsedMs = System.currentTimeMillis() - startedAt
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _searchState.update {
                it.copy(isSearching = false, error = e.message)
            }
        }
    }
}

fun MainViewModel.cancelSearch() {
    searchJob?.cancel()
    searchJob = null
    _searchState.update { it.copy(isSearching = false) }
}

fun MainViewModel.handleSearchResultClick(item: FileItem) {
    val isLeft = _searchState.value.sourceIsLeft
    if (item.isDirectory) {
        val uri = item.uri
        closeSearch()
        openDirectory(isLeft, uri)
    } else {
        openFile(item)
    }
}

fun MainViewModel.revealInPanel(item: FileItem) {
    val isLeft = _searchState.value.sourceIsLeft
    val parent = item.uri.path?.let { File(it).parentFile } ?: return
    if (!parent.exists()) return
    closeSearch()
    openDirectory(isLeft, Uri.fromFile(parent))
}

// ==================== 书签 / 最近 ====================

fun MainViewModel.openBookmarkPanel(isLeft: Boolean) {
    _bookmarkPanelState.value = BookmarkPanelState(
        visible = true,
        sourceIsLeft = isLeft,
        bookmarks = BookmarkStore.loadBookmarks(getApplication()),
        recents = BookmarkStore.loadRecents(getApplication()),
        showRecents = false
    )
}

fun MainViewModel.closeBookmarkPanel() {
    _bookmarkPanelState.value = BookmarkPanelState()
}

fun MainViewModel.toggleBookmarkTab(showRecents: Boolean) {
    _bookmarkPanelState.update {
        it.copy(
            showRecents = showRecents,
            bookmarks = BookmarkStore.loadBookmarks(getApplication()),
            recents = BookmarkStore.loadRecents(getApplication())
        )
    }
}

fun MainViewModel.addBookmark(isLeft: Boolean) {
    val uri = flowOf(isLeft).value.currentUri ?: return
    val path = uri.path ?: return
    val f = File(path)
    if (!f.isDirectory) return
    val list = BookmarkStore.loadBookmarks(getApplication()).toMutableList()
    if (list.any { it.uriString == uri.toString() }) {
        _toast.value = "已收藏"
        return
    }
    list.add(
        0,
        Bookmark(
            path = path,
            name = f.name.ifBlank { path },
            uriString = uri.toString()
        )
    )
    BookmarkStore.saveBookmarks(getApplication(), list)
    _toast.value = "已添加书签"
    _bookmarkPanelState.update { it.copy(bookmarks = list) }
}

fun MainViewModel.removeBookmark(bm: Bookmark) {
    val list = BookmarkStore.loadBookmarks(getApplication())
        .filterNot { it.uriString == bm.uriString }
    BookmarkStore.saveBookmarks(getApplication(), list)
    _bookmarkPanelState.update { it.copy(bookmarks = list) }
}

fun MainViewModel.openBookmark(bm: Bookmark) {
    val isLeft = _bookmarkPanelState.value.sourceIsLeft
    val uri = bm.toUri()
    closeBookmarkPanel()
    openDirectory(isLeft, uri)
}

fun MainViewModel.openRecent(item: RecentItem) {
    val isLeft = _bookmarkPanelState.value.sourceIsLeft
    val uri = item.toUri()
    closeBookmarkPanel()
    openDirectory(isLeft, uri)
}

fun MainViewModel.clearRecents() {
    BookmarkStore.clearRecents(getApplication())
    _bookmarkPanelState.update { it.copy(recents = emptyList()) }
}