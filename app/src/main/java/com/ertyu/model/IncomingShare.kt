package com.ertyu.model

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow

/** 从其他应用「分享」进来、等待导入的文件 */
object IncomingShareHolder {
    val uris = MutableStateFlow<List<Uri>>(emptyList())
}
