package com.ertyu.engine

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SqliteBrowser {
    suspend fun getTables(dbFile: File): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val tables = mutableListOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }
            db.close()
            tables
        }
    }

    suspend fun getTableData(dbFile: File, tableName: String): Result<Pair<List<String>, List<List<String>>>> = withContext(Dispatchers.IO) {
        runCatching {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            // 使用 LIMIT 100 防止大表卡死
            db.rawQuery("SELECT * FROM \"$tableName\" LIMIT 100", null).use { cursor ->
                val columns = cursor.columnNames.toList()
                val rows = mutableListOf<List<String>>()
                while (cursor.moveToNext()) {
                    val row = mutableListOf<String>()
                    for (i in columns.indices) {
                        row.add(cursor.getString(i) ?: "NULL")
                    }
                    rows.add(row)
                }
                db.close()
                columns to rows
            }
        }
    }
}