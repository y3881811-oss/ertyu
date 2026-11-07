package com.ertyu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(vm: MainViewModel) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于软件") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeAbout() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================== App 头部信息 ==================
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "MTFM",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "本地文件管理器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))

            // ================== 应用信息列表 ==================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    // 版本信息
                    AboutRow(
                        icon = Icons.Default.Info,
                        title = "版本",
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        onClick = null // 不可点击
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    // GitHub
                    AboutRow(
                        icon = Icons.Default.Android, // 如果有 Github 图标可以换成自定义的
                        title = "在 GitHub 上查看",
                        subtitle = "y3881811-oss/ertyu",
                        onClick = {
                            val url = "https://github.com/y3881811-oss/ertyu"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 许可证
                    AboutRow(
                        icon = Icons.Default.Description,
                        title = "开源许可证",
                        subtitle = "GPL-3.0 License",
                        onClick = {
                            val url = "https://www.gnu.org/licenses/gpl-3.0.html"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // 隐私政策
                    AboutRow(
                        icon = Icons.Default.Security,
                        title = "隐私政策",
                        subtitle = "本应用不收集任何个人数据",
                        onClick = {
                            // 如果你有网页链接可以在这里跳转，如果没有可以暂时提示
                            vm.showToast("本应用完全离线运行，不收集任何数据")
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ================== 作者信息列表 ==================
            Text(
                text = "作者",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    AboutRow(
                        icon = Icons.Default.Person,
                        title = "y3881811",
                        subtitle = "开发者",
                        onClick = null
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    AboutRow(
                        icon = Icons.Default.Android,
                        title = "在 GitHub 上关注",
                        subtitle = "查看主页",
                        onClick = {
                            val url = "https://github.com/y3881811-oss"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "Made with ❤️ using Jetpack Compose",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}