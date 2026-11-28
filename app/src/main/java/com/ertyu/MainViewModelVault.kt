package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.VaultEngine
import com.ertyu.engine.VaultEntry
import com.ertyu.model.FileItem
import com.ertyu.model.VaultHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.openVault() {
    VaultHolder.password = ""
    VaultHolder.state.value = com.ertyu.model.VaultState(
        visible = true,
        hasPassword = VaultEngine.hasPassword()
    )
}

fun MainViewModel.closeVault() {
    VaultHolder.password = ""
    VaultHolder.state.value = com.ertyu.model.VaultState()
}

fun MainViewModel.createVault(password: String, confirm: String): Boolean {
    if (password.length < 4) {
        showToast("密码至少 4 位")
        return false
    }
    if (password != confirm) {
        showToast("两次输入的密码不一致")
        return false
    }
    val ok = VaultEngine.setPassword(password)
    if (!ok) {
        showToast("创建失败")
        return false
    }
    VaultHolder.password = password
    VaultHolder.state.update { it.copy(hasPassword = true, unlocked = true, entries = emptyList()) }
    AppLogger.i("Vault", "隐私空间已创建")
    showToast("隐私空间已创建，请牢记密码（忘记无法恢复）")
    val pending = VaultHolder.pendingItems
    VaultHolder.pendingItems = emptyList()
    if (pending.isNotEmpty()) importToVault(pending)
    return true
}

/** 从文件列表发起：已解锁直接移入，否则先跳到解锁页 */
fun MainViewModel.requestImportToVault(items: List<FileItem>) {
    if (items.isEmpty()) return
    if (VaultHolder.password.isNotBlank()) {
        importToVault(items)
        return
    }
    VaultHolder.pendingItems = items
    openVault()
    showToast("请先解锁/创建隐私空间，解锁后会自动移入")
}

fun MainViewModel.unlockVault(password: String) {
    if (!VaultEngine.verify(password)) {
        VaultHolder.state.update { it.copy(error = "密码错误") }
        return
    }
    VaultHolder.password = password
    VaultHolder.state.update {
        it.copy(unlocked = true, error = null, entries = VaultEngine.list())
    }
    AppLogger.i("Vault", "隐私空间已解锁")
    val pending = VaultHolder.pendingItems
    VaultHolder.pendingItems = emptyList()
    if (pending.isNotEmpty()) importToVault(pending)
}

fun MainViewModel.lockVault() {
    VaultHolder.password = ""
    VaultHolder.state.update { it.copy(unlocked = false, entries = emptyList()) }
}

fun MainViewModel.refreshVault() {
    VaultHolder.state.update { it.copy(entries = VaultEngine.list()) }
}

/** 把选中的文件/文件夹移入隐私空间 */
fun MainViewModel.importToVault(items: List<FileItem>) {
    val pwd = VaultHolder.password
    if (pwd.isBlank()) {
        showToast("请先解锁隐私空间")
        return
    }
    if (items.isEmpty()) return
    viewModelScope.launch {
        VaultHolder.state.update { it.copy(busy = true, busyText = "正在加密 0/${items.size}") }
        var ok = 0
        var fail = 0
        for ((index, item) in items.withIndex()) {
            val file = item.uri.path?.let(::File)
            if (file == null || !file.exists()) {
                fail++
                continue
            }
            VaultHolder.state.update { it.copy(busyText = "正在加密 ${index + 1}/${items.size}：${item.name}") }
            val r = VaultEngine.importEntry(pwd, file) { }
            if (r.isSuccess) ok++ else {
                fail++
                AppLogger.e("Vault", "移入失败 ${item.name}", r.exceptionOrNull())
            }
        }
        VaultHolder.state.update {
            it.copy(busy = false, busyText = "", entries = VaultEngine.list())
        }
        showToast(if (fail == 0) "已移入隐私空间 $ok 项" else "移入完成：成功 $ok，失败 $fail")
        refresh(true)
        refresh(false)
    }
}

fun MainViewModel.exportFromVault(entry: VaultEntry) {
    val pwd = VaultHolder.password
    if (pwd.isBlank()) {
        showToast("请先解锁隐私空间")
        return
    }
    val target = leftPanel.value.currentUri?.path?.let(::File)
    if (target == null) {
        showToast("左面板目录不可用")
        return
    }
    viewModelScope.launch {
        VaultHolder.state.update { it.copy(busy = true, busyText = "正在解密 ${entry.name}") }
        val r = VaultEngine.exportEntry(pwd, entry, target) { }
        VaultHolder.state.update { it.copy(busy = false, busyText = "") }
        r.onSuccess {
            showToast("已导出：${it.name}")
            refresh(true)
        }.onFailure {
            showToast("导出失败：${it.message}")
        }
    }
}

fun MainViewModel.deleteVaultEntry(entry: VaultEntry) {
    VaultEngine.deleteEntry(entry)
    refreshVault()
    showToast("已删除：${entry.name}")
}

fun MainViewModel.destroyVault() {
    VaultEngine.destroy()
    VaultHolder.password = ""
    VaultHolder.state.value = com.ertyu.model.VaultState(visible = true, hasPassword = false)
    showToast("隐私空间已清空（不可恢复）")
    AppLogger.i("Vault", "隐私空间已销毁")
}
