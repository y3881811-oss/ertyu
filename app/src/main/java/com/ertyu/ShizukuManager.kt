package com.ertyu

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuManager {
    const val REQUEST_CODE = 1001

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) { false }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission() {
        if (isAvailable() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }
}