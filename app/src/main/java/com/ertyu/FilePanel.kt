@Composable
private fun FileInfoDialog(item: FileItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件属性") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("名称", item.name)
                InfoRow("类型", item.typeText)
                if (item.isDirectory) {
                    val countText = if (item.childCount >= 0)
                        "目录（包含 ${item.childCount} 项）" else "目录"
                    InfoRow("大小", countText)
                } else {
                    InfoRow("大小", "${item.sizeText}（${item.size} 字节）")
                }
                InfoRow("修改时间", item.lastModifiedText)
                InfoRow("路径", item.path)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}