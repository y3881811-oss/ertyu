package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessManagerScreen(vm: MainViewModel) {
    val state by TerminalStateHolder.processState.collectAsState()

    BackHandler { vm.closeProcessManager() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("进程管理器") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeProcessManager() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(state.error!!, Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("PID", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            Text("USER", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            Text("CPU%", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            Text("MEM%", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                            Text("NAME", Modifier.weight(2f), style = MaterialTheme.typography.labelSmall)
                        }
                        HorizontalDivider()
                    }
                    items(state.processes) { p ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text(p.pid, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                            Text(p.user, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                            Text(p.cpu, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                            Text(p.mem, Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                            Text(p.name, Modifier.weight(2f), fontFamily = FontFamily.Monospace, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}