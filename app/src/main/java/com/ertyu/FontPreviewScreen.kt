package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPreviewScreen(vm: MainViewModel) {
    val state by FontPreviewStateHolder.state.collectAsState()
    val customFontFamily = remember(state.fontPath) {
        try {
            FontFamily(androidx.compose.ui.text.font.Typeface(android.graphics.Typeface.createFromFile(File(state.fontPath))))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    BackHandler { vm.closeFontPreview() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.fontName) },
                navigationIcon = {
                    IconButton(onClick = { vm.closeFontPreview() }) {
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
                .padding(16.dp)
        ) {
            Text("字体效果预览：", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            // 标题字号
            Text("永字八法", fontFamily = customFontFamily, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            
            // 中文字段
            Text(
                "天地玄黄，宇宙洪荒。日月盈昃，辰宿列张。寒来暑往，秋收冬藏。",
                fontFamily = customFontFamily, fontSize = 24.sp
            )
            Spacer(Modifier.height(16.dp))
            
            // 英文字段
            Text(
                "The quick brown fox jumps over the lazy dog.\n0123456789\nABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz",
                fontFamily = customFontFamily, fontSize = 20.sp
            )
        }
    }
}