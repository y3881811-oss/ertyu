package com.ertyu.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.ertyu.model.AppSettings
import com.ertyu.model.PanelMode
import com.ertyu.model.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 设置备份/恢复（JSON 文件，放在 Download 目录） */
object SettingsBackup {

    private const val MAGIC = "EYU_SETTINGS"
    private const val VERSION = 1

    data class Payload(val settings: AppSettings, val showFolderSize: Boolean)

    fun toJson(s: AppSettings, showFolderSize: Boolean): String {
        val root = JSONObject()
        root.put("magic", MAGIC)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("panelMode", s.panelMode.name)
        root.put("showHiddenByDefault", s.showHiddenByDefault)
        root.put("sortModeDefault", s.sortModeDefault.name)
        root.put("isGridMode", s.isGridMode)
        root.put("useRecycleBin", s.useRecycleBin)
        root.put("engineType", s.engineType)
        root.put("webDavSharePath", s.webDavSharePath)
        root.put("webDavPort", s.webDavPort)
        root.put("webDavUsername", s.webDavUsername)
        root.put("webDavPassword", s.webDavPassword)
        root.put("showFolderSize", showFolderSize)
        val arr = JSONArray()
        s.customPaths.forEach { arr.put(it) }
        root.put("customPaths", arr)
        return root.toString(2)
    }

    fun parse(json: String): Payload? {
        return try {
            val root = JSONObject(json)
            if (root.optString("magic") != MAGIC) return null
            val panelMode = runCatching { PanelMode.valueOf(root.optString("panelMode", PanelMode.AUTO.name)) }
                .getOrDefault(PanelMode.AUTO)
            val sortMode = runCatching { SortMode.valueOf(root.optString("sortModeDefault", SortMode.NAME_ASC.name)) }
                .getOrDefault(SortMode.NAME_ASC)
            val paths = mutableListOf<String>()
            val arr = root.optJSONArray("customPaths")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val p = arr.optString(i, "")
                    if (p.isNotBlank()) paths.add(p)
                }
            }
            val engine = root.optInt("engineType", 0).coerceIn(0, 2)
            val settings = AppSettings(
                panelMode = panelMode,
                showHiddenByDefault = root.optBoolean("showHiddenByDefault", false),
                sortModeDefault = sortMode,
                isGridMode = root.optBoolean("isGridMode", false),
                useRecycleBin = root.optBoolean("useRecycleBin", true),
                engineType = engine,
                webDavSharePath = root.optString("webDavSharePath", ""),
                webDavPort = root.optInt("webDavPort", 7326),
                webDavUsername = root.optString("webDavUsername", "mtfm"),
                webDavPassword = root.optString("webDavPassword", "mtfm123"),
                customPaths = paths
            )
            Payload(settings, root.optBoolean("showFolderSize", true))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun writeToDownload(settings: AppSettings, showFolderSize: Boolean): File? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(Environment.getExternalStorageDirectory(), "Download")
                if (!dir.exists()) dir.mkdirs()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                val out = File(dir, "EYU-settings-$stamp.json")
                out.writeText(toJson(settings, showFolderSize))
                out
            } catch (_: Exception) {
                null
            }
        }

    suspend fun readUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
