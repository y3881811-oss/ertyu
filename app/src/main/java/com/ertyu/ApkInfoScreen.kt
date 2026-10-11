package com.ertyu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.ertyu.model.ApkInfo
import com.ertyu.model.ApkSignature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInfoScreen(vm: MainViewModel) {
    val state by vm.apkInfoState.collectAsState()
    val toast by vm.toast.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeApkInfo() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (state.info?.icon != null) {
                        IconButton(onClick = {
                            state.info?.let { vm.saveApkIcon(it) }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "保存图标")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "错误: ${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.info != null -> ApkInfoContent(state.info!!)
            }
        }
    }
}

@Composable
private fun ApkInfoContent(info: ApkInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 顶部图标 + 名称
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = info.icon
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Text("?", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = info.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                if (info.isDebug) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Debug 版本",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        InfoCard("基本信息") {
            InfoLine("版本名", info.versionName)
            InfoLine("版本号", info.versionCode.toString())
            InfoLine("文件大小", info.fileSizeText)
            InfoLine("文件名", info.fileName)
        }

        Spacer(Modifier.height(12.dp))

        InfoCard("SDK 信息") {
            InfoLine(
                "最低版本",
                "Android ${apiToVersion(info.minSdk)}（API ${info.minSdk}）"
            )
            InfoLine(
                "目标版本",
                "Android ${apiToVersion(info.targetSdk)}（API ${info.targetSdk}）"
            )
        }

        Spacer(Modifier.height(12.dp))

        InfoCard("组件") {
            InfoLine("Activity", "${info.activityCount} 个")
            InfoLine("Service", "${info.serviceCount} 个")
            InfoLine("Receiver", "${info.receiverCount} 个")
            InfoLine("Provider", "${info.providerCount} 个")
        }

        Spacer(Modifier.height(12.dp))

        SignatureCard(info.signature)

        Spacer(Modifier.height(12.dp))

        InfoCard("权限（${info.permissions.size}）") {
            if (info.permissions.isEmpty()) {
                Text(
                    "未申请任何权限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                info.permissions.forEach { perm ->
                    Text(
                        text = perm,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        InfoCard("路径") {
            Text(
                text = info.filePath,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SignatureCard(sig: ApkSignature?) {
    InfoCard("签名") {
        if (sig == null) {
            Text(
                "无法读取签名信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            return@InfoCard
        }

        InfoLine("签名方案", sig.schemeText)

        if (sig.signers.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "未找到签名证书",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sig.signers.forEachIndexed { index, signer ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "证书 #${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                MonospaceBlock("主题", signer.subject)
                MonospaceBlock("颁发者", signer.issuer)
                MonospaceBlock("序列号", signer.serialNumber)
                MonospaceBlock("有效期", "${signer.validFrom} ~ ${signer.validTo}")
                Spacer(Modifier.height(6.dp))
                MonospaceBlock("MD5", signer.md5)
                MonospaceBlock("SHA-1", signer.sha1)
                MonospaceBlock("SHA-256", signer.sha256)
            }
        }
    }
}

@Composable
private fun MonospaceBlock(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun apiToVersion(api: Int): String = when (api) {
    1 -> "1.0"
    2 -> "1.1"
    3 -> "1.5"
    4 -> "1.6"
    5 -> "2.0"
    6 -> "2.0.1"
    7 -> "2.1"
    8 -> "2.2"
    9 -> "2.3"
    10 -> "2.3.3"
    11 -> "3.0"
    12 -> "3.1"
    13 -> "3.2"
    14 -> "4.0"
    15 -> "4.0.3"
    16 -> "4.1"
    17 -> "4.2"
    18 -> "4.3"
    19 -> "4.4"
    20 -> "4.4W"
    21 -> "5.0"
    22 -> "5.1"
    23 -> "6.0"
    24 -> "7.0"
    25 -> "7.1"
    26 -> "8.0"
    27 -> "8.1"
    28 -> "9"
    29 -> "10"
    30 -> "11"
    31 -> "12"
    32 -> "12L"
    33 -> "13"
    34 -> "14"
    35 -> "15"
    else -> "未知"
}