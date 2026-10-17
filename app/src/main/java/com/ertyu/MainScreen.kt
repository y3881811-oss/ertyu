package com.ertyu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.ertyu.model.PanelMode
import kotlinx.coroutines.launch

@Composable
fun MainScreen(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWide = configuration.screenWidthDp >= 600

    val showDual = when (settings.panelMode) {
        PanelMode.DUAL -> true
        PanelMode.SINGLE -> false
        PanelMode.AUTO -> isLandscape || isWide
    }

    // 抽屉状态
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(vm = vm) {
                scope.launch { drawerState.close() }
            }
        }
    ) {
        if (showDual) {
            Row(modifier = Modifier.fillMaxSize()) {
                FilePanel(
                    isLeft = true,
                    vm = vm,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider()
                FilePanel(
                    isLeft = false,
                    vm = vm,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            var activeLeft by remember { mutableStateOf(true) }
            FilePanel(
                isLeft = activeLeft,
                vm = vm,
                onSwitchPanel = { activeLeft = !activeLeft },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}