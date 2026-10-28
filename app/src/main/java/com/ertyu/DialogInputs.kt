package com.ertyu

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.io.File

@Composable
fun DialogInputs(vm: MainViewModel) {
    val newFilePrompt by vm.newFilePrompt.collectAsState()
    if (newFilePrompt) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.dismissNewFilePrompt() },
            title = { Text("新建文件") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("文件名（含扩展名）") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (text.isNotBlank()) vm.createFile(text) else vm.dismissNewFilePrompt() }) { Text("创建") } },
            dismissButton = { TextButton(onClick = { vm.dismissNewFilePrompt() }) { Text("取消") } }
        )
    }

    val newFolderVisible by vm.newFolderDialogVisible.collectAsState()
    val newFolderIsLeft by vm.newFolderDialogIsLeft.collectAsState()
    if (newFolderVisible) {
        TextInputDialog(title = "新建文件夹", placeholder = "文件夹名称", initial = "", onConfirm = { name -> if (name.isNotBlank()) vm.createDirectory(newFolderIsLeft, name); vm.dismissNewFolderDialog() }, onDismiss = { vm.dismissNewFolderDialog() })
    }

    val jumpVisible by vm.jumpDialogVisible.collectAsState()
    val jumpIsLeft by vm.jumpDialogIsLeft.collectAsState()
    if (jumpVisible) {
        val currentPath = vm.flowOf(jumpIsLeft).value.currentUri?.path ?: ""
        TextInputDialog(title = "跳转到路径", placeholder = "/storage/emulated/0/...", initial = currentPath, onConfirm = { input ->
            if (input.isNotBlank()) { val dir = File(input); if (dir.exists() && dir.isDirectory) vm.openDirectory(jumpIsLeft, android.net.Uri.fromFile(dir)) }
            vm.dismissJumpDialog()
        }, onDismiss = { vm.dismissJumpDialog() })
    }

    val renameTarget by vm.renameDialogTarget.collectAsState()
    val renameIsLeft by vm.renameDialogIsLeft.collectAsState()
    renameTarget?.let { target ->
        TextInputDialog(title = "重命名", placeholder = "新名称", initial = target.name, onConfirm = { newName ->
            if (newName.isNotBlank() && newName != target.name) vm.rename(renameIsLeft, target, newName)
            vm.dismissRenameDialog()
        }, onDismiss = { vm.dismissRenameDialog() })
    }
}