package com.gordon.mypotato

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gordon.mypotato.data.AppDatabase

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    // 初始化数据库连接
    private val database = AppDatabase.getDatabase(application)
    private val appInfoDao = database.appInfoDao()

    init {
        // 后续根据需要在此处添加初始化逻辑
    }
}
