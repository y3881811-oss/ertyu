package com.ertyu

import androidx.compose.runtime.Composable

@Composable
fun AppDialogs(vm: MainViewModel) {
    DialogDeleteConfirm(vm)
    DialogZip(vm)
    DialogInstall(vm)
    DialogInputs(vm)
    DialogFileActions(vm)
    DialogApkParse(vm) // 挂载解析确认弹窗
}