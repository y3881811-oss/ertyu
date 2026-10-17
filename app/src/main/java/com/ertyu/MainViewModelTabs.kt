package com.ertyu

import android.net.Uri
import android.os.Environment
import java.io.File

fun MainViewModel.saveCurrentTab(isLeft: Boolean) {
    val tabsFlow = if (isLeft) TabManager.leftTabList else TabManager.rightTabList
    val activeFlow = if (isLeft) TabManager.leftActiveIndex else TabManager.rightActiveIndex
    val currentPanel = if (isLeft) _leftPanel else _rightPanel

    val tabs = tabsFlow.value.toMutableList()
    val idx = activeFlow.value
    if (idx in tabs.indices) {
        tabs[idx] = currentPanel.value
        tabsFlow.value = tabs
    }
}

fun MainViewModel.addTab(isLeft: Boolean) {
    saveCurrentTab(isLeft)
    val tabsFlow = if (isLeft) TabManager.leftTabList else TabManager.rightTabList
    val activeFlow = if (isLeft) TabManager.leftActiveIndex else TabManager.rightActiveIndex
    val currentPanel = if (isLeft) _leftPanel else _rightPanel

    val tabs = tabsFlow.value.toMutableList()
    val newPanel = com.ertyu.model.PanelState()
    tabs.add(newPanel)
    tabsFlow.value = tabs
    activeFlow.value = tabs.lastIndex
    currentPanel.value = newPanel

    // 新标签默认打开存储根目录
    val root = Uri.fromFile(Environment.getExternalStorageDirectory())
    openDirectory(isLeft, root)
}

fun MainViewModel.switchTab(isLeft: Boolean, index: Int) {
    val tabsFlow = if (isLeft) TabManager.leftTabList else TabManager.rightTabList
    val activeFlow = if (isLeft) TabManager.leftActiveIndex else TabManager.rightActiveIndex
    val currentPanel = if (isLeft) _leftPanel else _rightPanel

    if (index == activeFlow.value) return
    saveCurrentTab(isLeft)
    
    val tabs = tabsFlow.value
    if (index in tabs.indices) {
        activeFlow.value = index
        currentPanel.value = tabs[index]
    }
}

fun MainViewModel.closeTab(isLeft: Boolean, index: Int) {
    saveCurrentTab(isLeft)
    val tabsFlow = if (isLeft) TabManager.leftTabList else TabManager.rightTabList
    val activeFlow = if (isLeft) TabManager.leftActiveIndex else TabManager.rightActiveIndex
    val currentPanel = if (isLeft) _leftPanel else _rightPanel

    val tabs = tabsFlow.value.toMutableList()
    if (tabs.size <= 1) return
    tabs.removeAt(index)
    tabsFlow.value = tabs

    if (activeFlow.value == index) {
        val newIndex = if (index > 0) index - 1 else 0
        activeFlow.value = newIndex
        currentPanel.value = tabs[newIndex]
    } else if (activeFlow.value > index) {
        activeFlow.value = activeFlow.value - 1
    }
}