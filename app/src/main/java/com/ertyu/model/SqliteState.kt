package com.ertyu.model

data class SqliteState(
    val visible: Boolean = false,
    val dbPath: String = "",
    val dbName: String = "",
    val isLoading: Boolean = false,
    val tables: List<String> = emptyList(),
    val selectedTable: String? = null,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val error: String? = null
)