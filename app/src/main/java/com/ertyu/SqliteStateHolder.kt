package com.ertyu

import com.ertyu.model.SqliteState
import kotlinx.coroutines.flow.MutableStateFlow

object SqliteStateHolder {
    val state = MutableStateFlow(SqliteState())
}