package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.model.PanelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    val isSingle = settings.panelMode == PanelMode.SINGLE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSingle) "操作说明（单面板）"
                        else "操作说明（双面板）"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeHelp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isSingle) {
                HelpSection("基本浏览") {
                    HelpLine("点击文件夹", "进入该目录")
                    HelpLine("点击文件", "打开（文本/图片/APK/压缩包等）")
                    HelpLine("顶部 ↑ 按钮", "返回上一级")
                    HelpLine("点击顶部路径文字", "手动输入路径跳转")
                    HelpLine("右上角 ⇄ 按钮", "切换到另一个面板目录")
                }

                HelpSection("选择与批量操作") {
                    HelpLine("长按文件", "弹出操作菜单")
                    HelpLine("点右侧 ⋮", "弹出操作菜单")
                    HelpLine("进入多选模式后", "点击选择多项，顶部工具栏批量操作")
                    HelpLine("多选工具栏", "全选 / 压缩 / 复制 / 移动 / 删除")
                }

                HelpSection("压缩与解压（单面板）") {
                    HelpLine("点击 ZIP 等压缩包", "弹出\"解压到当前目录\"")
                    HelpLine("长按压缩包 → 操作菜单", "解压到当前目录")
                    HelpLine("长按普通文件 → 操作菜单", "压缩为… → 选格式")
                    HelpLine("多选后点工具栏 📦", "批量压缩为一个包")
                    Spacer(Modifier.height(4.dp))
                    HelpNote(
                        "单面板模式下不支持\"解压到另一侧\"、" +
                                "\"复制到另一侧\"等跨面板操作。"
                    )
                }

                HelpSection("其他功能") {
                    HelpLine("右上角 ⋮ → 搜索", "在当前目录下递归搜索")
                    HelpLine("右上角 ⋮ → 书签", "收藏夹 / 最近访问")
                    HelpLine("右上角 ⋮ → 已安装应用", "查看手机上的 App，可提取 APK")
                    HelpLine("长按文件 → HEX 查看", "十六进制查看文件内容")
                    HelpLine("长按 APK → 安装 / 浏览内部", "安装 APK 或查看内部文件")
                }
            } else {
                HelpSection("基本浏览") {
                    HelpLine("左右面板", "可分别浏览不同目录")
                    HelpLine("点击文件夹", "进入该目录")
                    HelpLine("点击文件", "打开（文本/图片/APK/压缩包等）")
                    HelpLine("点击顶部路径文字", "手动输入路径跳转")
                }

                HelpSection("跨面板操作") {
                    HelpLine("复制到另一侧", "把文件/文件夹复制到另一面板的当前目录")
                    HelpLine("移动到另一侧", "移动（同分区秒完成，跨分区自动复制+删除）")
                    HelpLine("解压到另一侧", "压缩包解压到另一面板目录")
                    HelpLine("批量复制/移动", "多选后点工具栏对应按钮")
                }

                HelpSection("压缩与解压（双面板）") {
                    HelpLine("点击 ZIP 等压缩包", "弹出选择框：解压到当前目录 / 解压到另一侧")
                    HelpLine("长按压缩包 → 操作菜单", "两个解压位置任选")
                    HelpLine("长按普通文件 → 操作菜单", "压缩为… → 选格式（ZIP/TAR/TAR.GZ）")
                    HelpLine("多选后点工具栏 📦", "批量压缩为一个包")
                    Spacer(Modifier.height(4.dp))
                    HelpNote(
                        "推荐操作流程：左面板选到源文件，" +
                                "右面板切到目标目录，然后一次操作完成。"
                    )
                }

                HelpSection("其他功能") {
                    HelpLine("右上角 ⋮ → 搜索", "在当前目录下递归搜索")
                    HelpLine("右上角 ⋮ → 书签", "收藏夹 / 最近访问")
                    HelpLine("右上角 ⋮ → 已安装应用", "查看手机上的 App，可提取 APK")
                    HelpLine("长按文件 → HEX 查看", "十六进制查看文件内容")
                    HelpLine("长按 APK → 安装 / 浏览内部", "安装 APK 或查看内部文件")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun HelpLine(action: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HelpNote(text: String) {
    Text(
        text = "💡 $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 6.dp)
    )
}