package com.ertyu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.PanelMode

@Composable
fun DialogFileActions(vm: MainViewModel) {
    val actionTarget by vm.actionDialogTarget.collectAsState()
    val actionIsLeft by vm.actionDialogIsLeft.collectAsState()
    val settings by vm.settingsState.collectAsState()
    
    actionTarget?.let { target ->
        val format = ArchiveFormat.detect(target.name)
        val isApk = target.name.substringAfterLast('.', "").lowercase() in listOf("apk", "apks", "xapk", "apkm")
        val showCrossPanel = settings.panelMode != PanelMode.SINGLE
        FileActionDialog(
            item = target, archiveFormat = format, isApk = isApk, showCrossPanel = showCrossPanel,
            onDismiss = { vm.dismissActionDialog() },
            onRename = { vm.dismissActionDialog(); vm.openRenameDialog(actionIsLeft, target) },
            onCopy = { vm.dismissActionDialog(); vm.copyToOtherPanel(actionIsLeft, target) },
            onMove = { vm.dismissActionDialog(); vm.moveToOtherPanel(actionIsLeft, target) },
            onDelete = { vm.dismissActionDialog(); vm.requestDelete(actionIsLeft, target) },
            onSelect = { vm.dismissActionDialog(); vm.enterSelection(actionIsLeft, target.uriString) },
            onExtractHere = { vm.dismissActionDialog(); vm.extractZipHere(actionIsLeft, target) },
            onExtractOther = { vm.dismissActionDialog(); vm.extractZipToOtherPanel(actionIsLeft, target) },
            onZipThis = { vm.dismissActionDialog(); vm.enterSelection(actionIsLeft, target.uriString); vm.requestCompress(actionIsLeft) },
            onInfo = { vm.dismissActionDialog(); vm.openInfoDialog(target) },
            onInstall = { vm.dismissActionDialog(); vm.installApk(target) },
            onBrowseApk = { vm.dismissActionDialog(); vm.openApkBrowser(actionIsLeft, target) },
            onHexView = { vm.dismissActionDialog(); vm.openHexViewer(target) },
            onChecksum = { vm.dismissActionDialog(); vm.openChecksum(target) },
            onApkSign = { vm.dismissActionDialog(); vm.openApkSigner(target) },
            onApkClone = { vm.dismissActionDialog(); vm.openApkClone(target) },
            onCrypto = { vm.dismissActionDialog(); vm.openCrypto(actionIsLeft, target) }
        )
    }

    val infoTarget by vm.infoDialogTarget.collectAsState()
    infoTarget?.let { target -> FileInfoDialog(item = target, vm = vm, onDismiss = { vm.dismissInfoDialog() }) }

    val sortVisible by vm.sortDialogVisible.collectAsState()
    val sortIsLeft by vm.sortDialogIsLeft.collectAsState()
    if (sortVisible) {
        val currentSort = vm.flowOf(sortIsLeft).value.sortMode
        SortDialog(current = currentSort, onSelect = { vm.setSortMode(sortIsLeft, it); vm.dismissSortDialog() }, onDismiss = { vm.dismissSortDialog() })
    }

    val compressVisible by vm.compressPromptVisible.collectAsState()
    if (compressVisible) {
        CompressFormatDialog(onSelect = { format -> vm.compressWithFormat(format) }, onDismiss = { vm.dismissCompressPrompt() })
    }
}