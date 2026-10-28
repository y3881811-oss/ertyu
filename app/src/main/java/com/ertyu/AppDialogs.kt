package com.ertyu

import androidx.compose.runtime.Composable

@Composable
fun AppDialogs(vm: MainViewModel) {
    DialogDeleteConfirm(vm)
    DialogZip(vm)
    DialogInstall(vm)
    DialogInputs(vm)
    DialogFileActions(vm)
    DialogApkParse(vm)
    DialogParseProgress(vm)
    DialogTransferProgress(vm) // 新增：传输进度
}