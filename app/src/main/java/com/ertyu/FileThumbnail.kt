package com.ertyu

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ThumbnailLoader
import com.ertyu.model.FileItem

private val IMAGE_EXT_SET = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val VIDEO_EXT_SET = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")

@Composable
fun FileThumbnail(
    item: FileItem,
    modifier: Modifier = Modifier
) {
    val ext = item.name.substringAfterLast('.', "").lowercase()
    val isImage = !item.isDirectory && ext in IMAGE_EXT_SET
    val isVideo = !item.isDirectory && ext in VIDEO_EXT_SET

    if (!isImage && !isVideo) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = modifier.size(24.dp)
        )
        return
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, item.path) {
        value = ThumbnailLoader.load(item.path, isVideo)
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = modifier.size(24.dp)
        )
    }
}