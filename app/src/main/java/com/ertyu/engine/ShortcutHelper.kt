package com.ertyu.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ertyu.MainActivity

/** 给某个目录创建桌面快捷方式；点开直接进入该目录 */
object ShortcutHelper {
    const val EXTRA_OPEN_DIR = "eyu_open_dir"

    fun createFolderShortcut(context: Context, path: String, label: String): Boolean = try {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_OPEN_DIR, path)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val icon = try {
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_manage)
        } catch (_: Exception) {
            null
        }
        val builder = ShortcutInfoCompat.Builder(context, "dir_" + path.hashCode())
            .setShortLabel(label.take(10).ifBlank { "目录" })
            .setLongLabel(label.ifBlank { path })
            .setIntent(intent)
        if (icon != null) builder.setIcon(icon)
        ShortcutManagerCompat.pushDynamicShortcut(context, builder.build())
    } catch (_: Exception) {
        false
    }
}
