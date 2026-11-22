package com.ertyu

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.UpdateDownloader
import com.ertyu.engine.UpdateVerifier
import com.ertyu.model.UpdateDownloadHolder
import com.ertyu.model.UpdateDownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private object UpdateJobs {
    var job: Job? = null
}

/** 内置下载更新：SHA256 校验 + 签名校验通过后才触发系统安装 */
fun MainViewModel.startUpdateDownload(url: String) {
    if (UpdateDownloadHolder.state.value.visible &&
        !UpdateDownloadHolder.state.value.done &&
        UpdateDownloadHolder.state.value.error == null
    ) {
        showToast("正在下载更新，请稍候")
        return
    }
    UpdateJobs.job?.cancel()

    val app = getApplication<android.app.Application>()
    val dir = File(app.cacheDir, "updates").apply { mkdirs() }
    val fileName = url.substringBefore('?').substringAfterLast('/').ifBlank { "update.apk" }
    val dest = File(dir, fileName)
    val sumsUrl = url.substringBefore('?').substringBeforeLast('/') + "/SHA256SUMS.txt"

    // 顺手清掉上几次留下的旧包/半包，只保留本次要下的文件
    com.ertyu.engine.UpdateCacheCleaner.clean(app, keep = dest)

    AppLogger.i("Update", "开始下载更新: $url")
    UpdateDownloadHolder.state.value = UpdateDownloadState(visible = true, phase = "连接中…")

    fun fail(message: String) {
        AppLogger.e("Update", "更新失败: $message")
        UpdateDownloadHolder.state.update { it.copy(phase = "失败", error = message) }
        showToast("更新失败：$message")
    }

    UpdateJobs.job = viewModelScope.launch {
        try {
            // 1) 取校验和（拿不到不致命，后面还有签名校验兜底）
            val sumsText = UpdateDownloader.fetchText(sumsUrl)
            val expected = sumsText?.let { UpdateVerifier.parseSums(it)[fileName] }
            if (expected != null) {
                AppLogger.i("Update", "已获取 SHA256: $expected")
            } else {
                AppLogger.i("Update", "未取到 SHA256SUMS.txt，仅做签名校验")
            }

            // 2) 下载
            UpdateDownloadHolder.state.update { it.copy(phase = "下载中…") }
            val dl = UpdateDownloader.download(url, dest) { done, total ->
                UpdateDownloadHolder.state.update {
                    it.copy(
                        phase = "下载中…",
                        downloaded = done,
                        total = total,
                        percent = if (total > 0) ((done * 100) / total).toInt() else 0
                    )
                }
            }
            if (dl.isFailure) {
                fail(dl.exceptionOrNull()?.message ?: "下载失败")
                return@launch
            }
            AppLogger.i("Update", "下载完成: ${dest.name}（${ItemSizeEngine.format(dest.length())}）")

            // 3) SHA256
            UpdateDownloadHolder.state.update { it.copy(phase = "校验文件…") }
            if (expected != null) {
                val actual = UpdateVerifier.sha256Of(dest)
                if (actual == null || !actual.equals(expected, ignoreCase = true)) {
                    dest.delete()
                    fail("SHA256 不一致，文件可能被篡改或下载不完整")
                    return@launch
                }
                AppLogger.i("Update", "SHA256 校验通过")
            } else {
                AppLogger.i("Update", "跳过 SHA256（未提供校验和）")
            }

            // 4) 签名必须与当前 App 一致（能挡住代理替换/二次打包）
            UpdateDownloadHolder.state.update { it.copy(phase = "校验签名…") }
            val (sigOk, detail) = UpdateVerifier.verifySignature(app, dest)
            AppLogger.i("Update", "签名校验: $detail")
            if (!sigOk) {
                dest.delete()
                fail("签名校验失败：$detail")
                return@launch
            }

            // 5) 触发安装
            UpdateDownloadHolder.state.update { it.copy(phase = "启动安装…") }
            val installed = triggerInstall(app, dest)
            if (!installed) {
                fail("无法启动安装程序")
                return@launch
            }
            AppLogger.i("Update", "已触发安装: ${dest.name}")
            UpdateDownloadHolder.state.update { it.copy(phase = "已启动安装", done = true, percent = 100) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            UpdateDownloadHolder.state.update { it.copy(phase = "已取消") }
            throw e
        } catch (e: Exception) {
            fail(e.message ?: "未知错误")
        } finally {
            UpdateJobs.job = null
        }
    }
}

fun MainViewModel.cancelUpdateDownload() {
    UpdateJobs.job?.cancel()
    UpdateJobs.job = null
    UpdateDownloadHolder.state.update { it.copy(phase = "已取消", done = true) }
}

fun MainViewModel.dismissUpdateDownload() {
    UpdateJobs.job?.cancel()
    UpdateJobs.job = null
    UpdateDownloadHolder.state.value = UpdateDownloadState()
}

private fun triggerInstall(context: Context, apk: File): Boolean = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    true
} catch (e: Exception) {
    AppLogger.e("Update", "启动安装失败", e)
    false
}
