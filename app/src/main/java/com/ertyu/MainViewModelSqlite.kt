package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.SqliteBrowser
import com.ertyu.model.FileItem
import com.ertyu.model.SqliteState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openSqlite(item: FileItem) {
    val path = item.uri.path ?: return
    SqliteStateHolder.state.value = SqliteState(
        visible = true, dbPath = path, dbName = item.name, isLoading = true
    )
    viewModelScope.launch {
        val result = SqliteBrowser.getTables(File(path))
        result.onSuccess { tables ->
            SqliteStateHolder.state.update { it.copy(isLoading = false, tables = tables) }
        }.onFailure { e ->
            SqliteStateHolder.state.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}

fun MainViewModel.selectSqliteTable(tableName: String) {
    val state = SqliteStateHolder.state.value
    SqliteStateHolder.state.update { it.copy(isLoading = true, selectedTable = tableName) }
    viewModelScope.launch {
        val result = SqliteBrowser.getTableData(File(state.dbPath), tableName)
        result.onSuccess { (cols, rows) ->
            SqliteStateHolder.state.update { it.copy(isLoading = false, columns = cols, rows = rows) }
        }.onFailure { e ->
            SqliteStateHolder.state.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}

fun MainViewModel.closeSqliteTable() {
    SqliteStateHolder.state.update { it.copy(selectedTable = null, columns = emptyList(), rows = emptyList()) }
}

fun MainViewModel.closeSqlite() {
    SqliteStateHolder.state.value = SqliteState()
}