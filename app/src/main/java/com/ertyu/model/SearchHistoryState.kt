package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

/** 最近搜索词（最新在前，最多 20 条） */
object SearchHistoryHolder {
    val items = MutableStateFlow<List<String>>(emptyList())
}
