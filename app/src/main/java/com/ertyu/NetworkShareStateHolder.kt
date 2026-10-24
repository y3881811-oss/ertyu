package com.ertyu

import com.ertyu.engine.FtpServerManager
import com.ertyu.engine.WebShareServer
import com.ertyu.model.NetworkShareState
import kotlinx.coroutines.flow.MutableStateFlow

object NetworkShareStateHolder {
    val state = MutableStateFlow(NetworkShareState())
    var webServer: WebShareServer? = null
}