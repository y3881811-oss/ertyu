package com.ertyu

import com.ertyu.engine.WebDavServer
import com.ertyu.model.WebDavState
import kotlinx.coroutines.flow.MutableStateFlow

object WebDavStateHolder {
    val state = MutableStateFlow(WebDavState())
    var server: WebDavServer? = null
}