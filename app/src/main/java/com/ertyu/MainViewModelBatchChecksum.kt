package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ChecksumExporter
import com.ertyu.engine.FileChecksum
import com.ertyu.model.BatchChecksumHolder
import com.ertyu.model.BatchChecksumState
import com.ertyu.model.ChecksumAlgorithm
import com.ertyu.model.ChecksumRow
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private object ChecksumJobs {
    var job: Job? = null
}

fun MainViewModel.openBatchChecksum(items: List<FileItem>) {
    val files = items.filter { !it.isDirectory }
    if (files.isEmpty()) {
        showToast("请选择文件（文件夹不支持校验）")
        return
    }
    BatchChecksumHolder.state.value = BatchChecksumState(
        visible = true,
        rows = files.map { ChecksumRow(path = it.path, name = it.name, size = it.size) }
    )
    startBatchChecksum()
}

fun MainViewModel.closeBatchChecksum() {
    ChecksumJobs.job?.cancel()
    ChecksumJobs.job = null
    BatchChecksumHolder.state.value = BatchChecksumState()
}

fun MainViewModel.setChecksumAlgorithm(algorithm: ChecksumAlgorithm) {
    if (BatchChecksumHolder.state.value.algorithm == algorithm) return
    BatchChecksumHolder.state.update {
        it.copy(
            algorithm = algorithm,
            rows = it.rows.map { r -> r.copy(hash = null, error = null) },
            progressIndex = 0
        )
    }
    startBatchChecksum()
}

fun MainViewModel.startBatchChecksum() {
    val snap = BatchChecksumHolder.state.value
    if (snap.running || snap.rows.isEmpty()) return
    ChecksumJobs.job?.cancel()
    BatchChecksumHolder.state.update { it.copy(running = true, progressIndex = 0, error = null) }

    ChecksumJobs.job = viewModelScope.launch(Dispatchers.IO) {
        try {
            val algorithm = snap.algorithm
            snap.rows.forEachIndexed { index, row ->
                currentCoroutineContext().ensureActive()
                val f = File(row.path)
                val hash = if (f.isFile) FileChecksum.calculate(f, algorithm.jca) else null
                val error = when {
                    !f.exists() -> "文件不存在"
                    hash == null -> "读取失败"
                    else -> null
                }
                BatchChecksumHolder.state.update { st ->
                    st.copy(
                        rows = st.rows.mapIndexed { i, r ->
                            if (i == index) r.copy(hash = hash, error = error) else r
                        },
                        progressIndex = index + 1
                    )
                }
            }
            BatchChecksumHolder.state.update { it.copy(running = false) }
            AppLogger.i("Checksum", "批量校验完成 ${snap.rows.size} 项（${algorithm.label}）")
        } catch (e: kotlinx.coroutines.CancellationException) {
            BatchChecksumHolder.state.update { it.copy(running = false) }
            throw e
        } catch (e: Exception) {
            AppLogger.e("Checksum", "批量校验失败", e)
            BatchChecksumHolder.state.update { it.copy(running = false, error = e.message ?: "校验失败") }
        } finally {
            ChecksumJobs.job = null
        }
    }
}

fun MainViewModel.cancelBatchChecksum() {
    ChecksumJobs.job?.cancel()
    ChecksumJobs.job = null
    BatchChecksumHolder.state.update { it.copy(running = false) }
    showToast("已停止校验")
}

fun MainViewModel.exportChecksumCsv() {
    val snap = BatchChecksumHolder.state.value
    if (snap.rows.none { it.hash != null }) {
        showToast("还没有计算完成的哈希")
        return
    }
    viewModelScope.launch {
        val file = ChecksumExporter.export(snap.rows, snap.algorithm)
        withContext(Dispatchers.Main) {
            if (file != null) {
                showToast("已导出：${file.absolutePath}")
                AppLogger.i("Checksum", "导出 CSV：${file.absolutePath}")
            } else {
                showToast("导出失败，请检查存储权限")
            }
        }
    }
}
