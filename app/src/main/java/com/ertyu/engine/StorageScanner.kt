package com.ertyu.engine

import android.os.Environment
import java.io.File

object StorageScanner {

    data class StorageVolume(
        val name: String,
        val path: String,
        val isInternal: Boolean = false
    )

    /**
     * 扫描所有本地存储卷
     * 返回：内部存储、根目录、所有外置SD卡/U盘、Termux目录（如果存在）
     */
    fun scanAll(): List<StorageVolume> {
        val result = mutableListOf<StorageVolume>()

        // 1. 根目录
        result.add(StorageVolume("根目录", "/"))

        // 2. 内部存储
        val internal = Environment.getExternalStorageDirectory().absolutePath
        result.add(StorageVolume("内部存储", internal, isInternal = true))

        // 3. 外置存储（SD卡、U盘）
        val storageDir = File("/storage")
        val volumes = storageDir.listFiles()
        if (volumes != null) {
            for (vol in volumes) {
                if (!vol.isDirectory) continue
                val name = vol.name
                // 排除模拟存储、根目录自身、以及无效的链接
                if (name == "emulated" || name == "self" || name == internal) continue
                
                // 外置存储通常形如 XXXX-XXXX
                if (name.matches(Regex("[A-Za-z0-9]+-[A-Za-z0-9]+"))) {
                    result.add(StorageVolume("外置存储 ($name)", vol.absolutePath))
                }
            }
        }

        // 4. 检查常见特殊目录（如 Termux）
        val termuxDir = File(internal, "Termux")
        if (termuxDir.exists() && termuxDir.isDirectory) {
            result.add(StorageVolume("Termux Home", termuxDir.absolutePath))
        }

        return result
    }
}