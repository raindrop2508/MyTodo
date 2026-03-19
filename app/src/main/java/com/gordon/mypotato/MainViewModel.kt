package com.gordon.mypotato

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.data.AppDatabase
import com.gordon.mypotato.data.entity.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val appInfoDao = database.appInfoDao()

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    // Observe latest database entry
    val latestAppInfo: LiveData<AppInfo?> = appInfoDao.getLatestAppInfo()

    init {
        _text.value = application.getString(R.string.main_hello_from_viewmodel)
        insertInitialData()
    }

    fun updateText() {
        // Update UI text
        _text.value = getApplication<Application>().getString(R.string.main_text_updated)

        // Insert new record into database
        insertNewRecord()
    }

    private fun insertInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = AppInfo(
                dbVersion = 1,
                createTime = System.currentTimeMillis()
            )
            appInfoDao.insert(info)
        }
    }

    private fun insertNewRecord() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = AppInfo(
                dbVersion = 1,
                createTime = System.currentTimeMillis()
            )
            appInfoDao.insert(info)
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}