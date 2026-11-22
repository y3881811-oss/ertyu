package com.ertyu.model

import com.ertyu.engine.SettingsBackup
import kotlinx.coroutines.flow.MutableStateFlow

data class SettingsBackupState(
    val pending: SettingsBackup.Payload? = null,
    val fileName: String = "",
    val busy: Boolean = false
)

object SettingsBackupHolder {
    val state = MutableStateFlow(SettingsBackupState())
}
