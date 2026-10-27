package com.ertyu.model

enum class PanelMode(val label: String) {
    AUTO("自动（横屏/平板双面板，竖屏单面板）"),
    DUAL("始终双面板"),
    SINGLE("始终单面板")
}

data class AppSettings(
    val panelMode: PanelMode = PanelMode.AUTO,
    val showHiddenByDefault: Boolean = false,
    val sortModeDefault: SortMode = SortMode.NAME_ASC,
    val isGridMode: Boolean = false,
    val useRecycleBin: Boolean = true, // 新增：默认开启回收站
    val webDavSharePath: String = "",
    val webDavPort: Int = 7326,
    val webDavUsername: String = "mtfm",
    val webDavPassword: String = "mtfm123",
    val customPaths: List<String> = emptyList()
) {
    val isSinglePanel: Boolean get() = panelMode == PanelMode.SINGLE
    val isDualPanel: Boolean get() = panelMode == PanelMode.DUAL
}