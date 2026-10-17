package com.ertyu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.ertyu.model.PanelMode

@Composable
fun MainScreen(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWide = configuration.screenWidthDp >= 600

    val showDual = when (settings.panelMode) {
        PanelMode.DUAL -> true
        PanelMode.SINGLE -> false
        PanelMode.AUTO -> isLandscape || isWide
    }

    if (showDual) {
        Row(modifier = Modifier.fillMaxSize()) {
            FilePanel(isLeft = true, vm = vm, modifier = Modifier.weight(1f))
            VerticalDivider()
            FilePanel(isLeft = false, vm = vm, modifier = Modifier.weight(1f))
        }
    } else {
        var activeLeft by remember { mutableStateOf(true) }
        FilePanel(
            isLeft = activeLeft,
            vm = vm,
            onSwitchPanel = { activeLeft = !activeLeft },
            modifier = Modifier.fillMaxSize()
        )
    }
}