package com.ertyu.engine

import android.content.Context
import com.ertyu.model.AppSettings
import com.ertyu.model.PanelMode
import com.ertyu.model.SortMode
import org.json.JSONArray

object SettingsStore {

    private const val PREF = "mtfm_settings"
    private const val KEY_PANEL_MODE = "panel_mode"
    private const val KEY_SHOW_HIDDEN = "show_hidden"
    private const val KEY_SORT_MODE = "sort_mode"
    private const val KEY_IS_GRID_MODE = "is_grid_mode"
    private const val KEY_USE_RECYCLE_BIN = "use_recycle_bin"
    private const val KEY_ENGINE_TYPE = "engine_type"
    private const val KEY_WEBDAV_PATH = "webdav_path"
    private const val KEY_WEBDAV_PORT = "webdav_port"
    private const val KEY_WEBDAV_USER = "webdav_user"
    private const val KEY_WEBDAV_PASS = "webdav_pass"
    private const val KEY_CUSTOM_PATHS = "custom_paths"

    fun load(context: Context): AppSettings {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val modeName = sp.getString(KEY_PANEL_MODE, PanelMode.AUTO.name)
        val mode = runCatching { PanelMode.valueOf(modeName ?: PanelMode.AUTO.name) }.getOrDefault(PanelMode.AUTO)
        val sortName = sp.getString(KEY_SORT_MODE, SortMode.NAME_ASC.name)
        val sort = runCatching { SortMode.valueOf(sortName ?: SortMode.NAME_ASC.name) }.getOrDefault(SortMode.NAME_ASC)

        val customPathsJson = sp.getString(KEY_CUSTOM_PATHS, "[]") ?: "[]"
        val customPaths = mutableListOf<String>()
        try {
            val arr = JSONArray(customPathsJson)
            for (i in 0 until arr.length()) {
                customPaths.add(arr.getString(i))
            }
        } catch (e: Exception) {}

        return AppSettings(
            panelMode = mode,
            showHiddenByDefault = sp.getBoolean(KEY_SHOW_HIDDEN, false),
            sortModeDefault = sort,
            isGridMode = sp.getBoolean(KEY_IS_GRID_MODE, false),
            useRecycleBin = sp.getBoolean(KEY_USE_RECYCLE_BIN, true),
            engineType = sp.getInt(KEY_ENGINE_TYPE, 0),
            webDavSharePath = sp.getString(KEY_WEBDAV_PATH, "") ?: "",
            webDavPort = sp.getInt(KEY_WEBDAV_PORT, 7326),
            webDavUsername = sp.getString(KEY_WEBDAV_USER, "mtfm") ?: "mtfm",
            webDavPassword = sp.getString(KEY_WEBDAV_PASS, "mtfm123") ?: "mtfm123",
            customPaths = customPaths
        )
    }

    fun save(context: Context, s: AppSettings) {
        val arr = JSONArray()
        s.customPaths.forEach { arr.put(it) }

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PANEL_MODE, s.panelMode.name)
            .putBoolean(KEY_SHOW_HIDDEN, s.showHiddenByDefault)
            .putString(KEY_SORT_MODE, s.sortModeDefault.name)
            .putBoolean(KEY_IS_GRID_MODE, s.isGridMode)
            .putBoolean(KEY_USE_RECYCLE_BIN, s.useRecycleBin)
            .putInt(KEY_ENGINE_TYPE, s.engineType)
            .putString(KEY_WEBDAV_PATH, s.webDavSharePath)
            .putInt(KEY_WEBDAV_PORT, s.webDavPort)
            .putString(KEY_WEBDAV_USER, s.webDavUsername)
            .putString(KEY_WEBDAV_PASS, s.webDavPassword)
            .putString(KEY_CUSTOM_PATHS, arr.toString())
            .apply()
    }
}