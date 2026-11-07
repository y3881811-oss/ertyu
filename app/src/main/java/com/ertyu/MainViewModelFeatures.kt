package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.FeatureEngines
import com.ertyu.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

val _contentSearchState = MutableStateFlow(ContentSearchState())
val MainViewModel.contentSearchState get() = _contentSearchState

val _duplicateScanState = MutableStateFlow(DuplicateScanState())
val MainViewModel.duplicateScanState get() = _duplicateScanState

fun MainViewModel.startContentSearch(rootDir: File, keyword: String) {
    if (keyword.isBlank()) return
    _contentSearchState.value = ContentSearchState(isSearching = true, query = keyword)
    viewModelScope.launch {
        val results = mutableListOf<ContentSearchResult>()
        FeatureEngines.searchContent(rootDir, keyword).collect { (file, lineInfo) ->
            results.add(ContentSearchResult(file, lineInfo.first, lineInfo.second))
            _contentSearchState.update { it.copy(results = results.toList()) }
        }
        _contentSearchState.update { it.copy(isSearching = false) }
    }
}
fun MainViewModel.closeContentSearch() { _contentSearchState.value = ContentSearchState() }

fun MainViewModel.startDuplicateScan(rootDir: File) {
    _duplicateScanState.value = DuplicateScanState(isScanning = true, progressText = "正在扫描...")
    viewModelScope.launch {
        val groups = mutableListOf<DuplicateGroup>()
        FeatureEngines.scanDuplicates(rootDir).collect { (hash, files) ->
            groups.add(DuplicateGroup(hash, files))
            _duplicateScanState.update { it.copy(groups = groups.toList()) }
        }
        _duplicateScanState.update { it.copy(isScanning = false, progressText = "扫描完成") }
    }
}
fun MainViewModel.toggleDuplicateSelection(path: String) {
    _duplicateScanState.update {
        val newSet = it.selectedFiles.toMutableSet()
        if (!newSet.add(path)) newSet.remove(path)
        it.copy(selectedFiles = newSet)
    }
}
fun MainViewModel.deleteSelectedDuplicates() {
    val state = _duplicateScanState.value
    viewModelScope.launch {
        state.selectedFiles.forEach { path ->
            val f = File(path)
            if (f.exists()) f.delete()
        }
        _duplicateScanState.update { it.copy(selectedFiles = emptySet(), groups = emptyList(), progressText = "已清理完毕") }
    }
}
fun MainViewModel.closeDuplicateScan() { _duplicateScanState.value = DuplicateScanState() }

fun MainViewModel.renameByExif(isLeft: Boolean, item: FileItem) {
    val file = File(item.path)
    val newName = FeatureEngines.generateExifRename(file)
    if (newName != null) {
        rename(isLeft, item, newName)
        _toast.value = "已重命名为: $newName"
    } else {
        _toast.value = "无法读取 EXIF 时间"
    }
}