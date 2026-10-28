package com.ertyu.model

data class ArchiveViewerState(
    val visible: Boolean = false,
    val archivePath: String = "",
    val archiveName: String = "",
    val sourceIsLeft: Boolean = true,
    val currentDir: String = "", // 新增：当前所在层级路径
    val entries: List<ArchiveEntry> = emptyList(),
    val filter: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // 过滤根据当前层级 + 搜索词计算
    val displayEntries: List<ArchiveEntry>
        get() {
            if (filter.isNotBlank()) {
                return entries.filter { it.path.contains(filter, ignoreCase = true) }
            }
            // 层级计算逻辑：提取当前目录下的直接子项
            val subEntries = entries.filter { it.path.startsWith(currentDir) && it.path != currentDir }
            val folderNames = mutableSetOf<String>()
            val directFiles = mutableListOf<ArchiveEntry>()
            
            subEntries.forEach { entry ->
                val relPath = entry.path.substring(currentDir.length)
                if (relPath.contains("/")) {
                    // 是更深层文件夹里的东西，提取第一层文件夹名
                    val folder = relPath.substringBefore("/")
                    folderNames.add(folder)
                } else {
                    // 当前目录下的文件
                    directFiles.add(entry)
                }
            }
            
            val result = mutableListOf<ArchiveEntry>()
            // 添加虚拟文件夹条目
            folderNames.sorted().forEach { folder ->
                result.add(
                    ArchiveEntry(
                        path = "$currentDir$folder/",
                        name = folder,
                        isDirectory = true,
                        size = 0,
                        compressedSize = 0,
                        depth = 0
                    )
                )
            }
            // 添加文件
            result.addAll(directFiles.sortedBy { it.name.lowercase() })
            return result
        }
}