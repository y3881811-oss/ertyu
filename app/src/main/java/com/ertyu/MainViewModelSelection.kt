package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.RecycleBinManager
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.enterSelection(isLeft: Boolean, uriString: String) {
    flowOf(isLeft).update { it.copy(selectedUris = setOf(uriString), isSelectionMode = true, lastSelectedUri = uriString) }
}

fun MainViewModel.handleSelectionClick(isLeft: Boolean, item: FileItem) {
    flowOf(isLeft).update { state ->
        val currentSelected = state.selectedUris.toMutableSet()
        val uriString = item.uriString
        if (currentSelected.contains(uriString)) {
            currentSelected.remove(uriString)
            val newMode = currentSelected.isNotEmpty()
            state.copy(selectedUris = currentSelected, isSelectionMode = newMode, lastSelectedUri = if (newMode) uriString else null)
        } else {
            val lastUri = state.lastSelectedUri
            val lastIndex = state.items.indexOfFirst { it.uriString == lastUri }
            val currentIndex = state.items.indexOfFirst { it.uriString == uriString }
            if (lastIndex != -1 && currentIndex != -1) {
                val start = minOf(lastIndex, currentIndex)
                val end = maxOf(lastIndex, currentIndex)
                for (i in start..end) currentSelected.add(state.items[i].uriString)
            } else currentSelected.add(uriString)
            state.copy(selectedUris = currentSelected, isSelectionMode = true, lastSelectedUri = uriString)
        }
    }
}

fun MainViewModel.toggleSelection(isLeft: Boolean, uriString: String) {
    flowOf(isLeft).update { state ->
        val set = state.selectedUris.toMutableSet()
        if (!set.add(uriString)) set.remove(uriString)
        state.copy(selectedUris = set, isSelectionMode = set.isNotEmpty(), lastSelectedUri = if (set.isNotEmpty()) uriString else null)
    }
}

fun MainViewModel.selectAll(isLeft: Boolean) {
    flowOf(isLeft).update { state -> state.copy(selectedUris = state.items.map { it.uriString }.toSet(), isSelectionMode = true, lastSelectedUri = null) }
}

fun MainViewModel.invertSelection(isLeft: Boolean) {
    flowOf(isLeft).update { state ->
        val newSelected = state.items.map { it.uriString }.toSet() - state.selectedUris
        state.copy(selectedUris = newSelected, isSelectionMode = newSelected.isNotEmpty(), lastSelectedUri = null)
    }
}

fun MainViewModel.selectByType(isLeft: Boolean, type: String) {
    flowOf(isLeft).update { state ->
        val newSelected = state.items.filter { !it.isDirectory && it.name.substringAfterLast('.', "").lowercase() == type }.map { it.uriString }.toSet()
        state.copy(selectedUris = state.selectedUris + newSelected, isSelectionMode = true, lastSelectedUri = null)
    }
}

fun MainViewModel.clearSelection(isLeft: Boolean) {
    flowOf(isLeft).update { it.copy(selectedUris = emptySet(), isSelectionMode = false, lastSelectedUri = null) }
}

fun MainViewModel.batchDelete(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    viewModelScope.launch {
        var toTrashCount = 0
        val useTrash = _settingsState.value.useRecycleBin
        targets.forEach { item ->
            val path = item.uri.path
            if (useTrash && path != null) {
                val file = File(path)
                if (RecycleBinManager.moveToTrash(getApplication(), file, item.isDirectory, item.size)) toTrashCount++
                else engine.delete(item.uri)
            } else engine.delete(item.uri)
        }
        clearSelection(isLeft)
        refresh(isLeft)
        if (toTrashCount > 0) _toast.value = "已处理 $toTrashCount 项到回收站"
    }
}