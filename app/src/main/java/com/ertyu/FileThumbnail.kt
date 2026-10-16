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

@Composable
fun FileThumbnail(
    item: FileItem,
    modifier: Modifier = Modifier
) {
    val isImage = !item.isDirectory && isImageFile(item.name)

    if (!isImage) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder
            else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = modifier.size(24.dp)
        )
        return
    }

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        item.path
    ) {
        value = ThumbnailLoader.load(item.path)
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = modifier.size(24.dp)
        )
    }
}

private fun isImageFile(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in IMAGE_EXT_SET
}

private val IMAGE_EXT_SET = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp"
)