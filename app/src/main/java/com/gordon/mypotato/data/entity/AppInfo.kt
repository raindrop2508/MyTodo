package com.gordon.mypotato.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,

    @ColumnInfo(name = "db_version")
    val dbVersion: Int,

    @ColumnInfo(name = "create_time")
    val createTime: Long
)
