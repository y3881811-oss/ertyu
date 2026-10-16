package com.ertyu.engine

import android.content.Context
import com.ertyu.model.AppSettings
import com.ertyu.model.PanelMode
import com.ertyu.model.SortMode

object SettingsStore {

    private const val PREF = "mtfm_settings"
    private const val KEY_PANEL_MODE = "panel_mode"
    private const val KEY_SHOW_HIDDEN = "show_hidden"
    private const val KEY_SORT_MODE = "sort_mode"

    fun load(context: Context): AppSettings {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val modeName = sp.getString(KEY_PANEL_MODE, PanelMode.AUTO.name)
        val mode = runCatching {
            PanelMode.valueOf(modeName ?: PanelMode.AUTO.name)
        }.getOrDefault(PanelMode.AUTO)
        val sortName = sp.getString(KEY_SORT_MODE, SortMode.NAME_ASC.name)
        val sort = runCatching {
            SortMode.valueOf(sortName ?: SortMode.NAME_ASC.name)
        }.getOrDefault(SortMode.NAME_ASC)
        return AppSettings(
            panelMode = mode,
            showHiddenByDefault = sp.getBoolean(KEY_SHOW_HIDDEN, false),
            sortModeDefault = sort
        )
    }

    fun save(context: Context, s: AppSettings) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PANEL_MODE, s.panelMode.name)
            .putBoolean(KEY_SHOW_HIDDEN, s.showHiddenByDefault)
            .putString(KEY_SORT_MODE, s.sortModeDefault.name)
            .apply()
    }
}