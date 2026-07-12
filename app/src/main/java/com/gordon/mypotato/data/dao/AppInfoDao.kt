package com.gordon.mypotato.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gordon.mypotato.data.entity.AppInfo

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM app_info ORDER BY create_time DESC LIMIT 1")
    fun getLatestAppInfo(): LiveData<AppInfo?>

    @Insert
    fun insert(appInfo: AppInfo)
}
