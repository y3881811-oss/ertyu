package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

enum class ChecksumAlgorithm(val label: String, val jca: String) {
    MD5("MD5", "MD5"),
    SHA1("SHA-1", "SHA-1"),
    SHA256("SHA-256", "SHA-256")
}

data class ChecksumRow(
    val path: String,
    val name: String,
    val size: Long,
    val hash: String? = null,
    val error: String? = null
)

data class BatchChecksumState(
    val visible: Boolean = false,
    val algorithm: ChecksumAlgorithm = ChecksumAlgorithm.SHA256,
    val rows: List<ChecksumRow> = emptyList(),
    val running: Boolean = false,
    val progressIndex: Int = 0,
    val error: String? = null
) {
    val doneCount: Int get() = rows.count { it.hash != null }
    val allDone: Boolean get() = rows.isNotEmpty() && rows.all { it.hash != null || it.error != null }
}

object BatchChecksumHolder {
    val state = MutableStateFlow(BatchChecksumState())
}
