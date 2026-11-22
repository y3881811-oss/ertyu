package com.ertyu.model

import com.ertyu.engine.LanScanner
import kotlinx.coroutines.flow.MutableStateFlow

data class LanScanState(
    val visible: Boolean = false,
    val scanning: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val selfIp: String = "",
    val hosts: List<LanScanner.Host> = emptyList(),
    val error: String? = null
)

object LanScanHolder {
    val state = MutableStateFlow(LanScanState())
}
