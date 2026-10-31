package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.MainViewModel
import com.ertyu.model.FileItem
import com.ertyu.model.PanelState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilePanel(
    panelId: String,
    isLeft: Boolean,
    vm: MainViewModel,
    items: List<FileItem>,
    state: PanelState,
    useGrid: Boolean,
    onOpen: (FileItem) -> Unit
) {
    var showSelectionSheet by remember { mutableStateOf(false) }
    var actionItem by remember { mutableStateOf<FileItem?>(null) }

    // 选择模式下长按：保证该项被选中，然后弹菜单
    fun handleLongClick(item: FileItem) {
        if (!state.isSelectionMode) {
            vm.enterSelection(isLeft, item.uriString)
        }
        if (item.uriString !in state.selectedUris) {
            vm.toggleSelection(isLeft, item.uriString)
        }
        showSelectionSheet = true
    }

    fun handleClick(item: FileItem) {
        if (state.isSelectionMode) {
            vm.toggleSelection(isLeft, item.uriString)
        } else {
            onOpen(item)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        if (state.isSelectionMode) {
            SelectionToolbar(
                selectedCount = state.selectedUris.size,
                onSelectAll = { vm.selectAll(isLeft) },
                onInvertSelection = { vm.invertSelection(isLeft) },
                onCopy = { vm.batchCopy(isLeft) },
                onMove = { vm.batchMove(isLeft) },
                onDelete = { vm.batchDelete(isLeft) },
                onCompress = { vm.requestCompress(isLeft) },
                onBatchRename = { vm.openBatchRename(isLeft) },
                onExit = { vm.clearSelection(isLeft) }
            )
        }

        if (useGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(items, key = { it.uriString }) { item ->
                    FileGridItem(
                        item = item,
                        isSelected = item.uriString in state.selectedUris,
                        selectionMode = state.isSelectionMode,
                        onClick = { handleClick(item) },
                        onLongClick = { handleLongClick(item) }
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(items, key = { it.uriString }) { item ->
                    FileRow(
                        item = item,
                        isSelected = item.uriString in state.selectedUris,
                        selectionMode = state.isSelectionMode,
                        onClick = { handleClick(item) },
                        onLongClick = { handleLongClick(item) }
                    )
                }
            }
        }
    }

    val selectedItems = items.filter { it.uriString in state.selectedUris }

    SelectionActionSheet(
        visible = showSelectionSheet,
        selectedItems = selectedItems,
        onDismiss = { showSelectionSheet = false },
        onSelectAll = { vm.selectAll(isLeft) },
        onInvertSelection = { vm.invertSelection(isLeft) },
        onCopy = { vm.batchCopy(isLeft) },
        onMove = { vm.batchMove(isLeft) },
        onDelete = { vm.batchDelete(isLeft) },
        onCompress = { vm.requestCompress(isLeft) },
        onBatchRename = { vm.openBatchRename(isLeft) },
        // 单文件项：先关 Sheet，再走原有对话框
        onOpenSingle = { item -> showSelectionSheet = false; onOpen(item) },
        onRenameSingle = { item -> showSelectionSheet = false; actionItem = item },
        onPropertiesSingle = { item -> showSelectionSheet = false; actionItem = item },
        onExtractSingle = { item -> showSelectionSheet = false; actionItem = item },
        onCompressSingle = { item -> showSelectionSheet = false; actionItem = item },
        onShareSingle = { item -> showSelectionSheet = false; actionItem = item },
        onInstallSingle = { item -> showSelectionSheet = false; actionItem = item },
        onSignSingle = { item -> showSelectionSheet = false; actionItem = item }
    )

    actionItem?.let { item ->
        FileActionDialog(
            isLeft = isLeft,
            item = item,
            vm = vm,
            onDismiss = { actionItem = null }
        )
    }
}