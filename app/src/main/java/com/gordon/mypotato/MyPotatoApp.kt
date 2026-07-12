package com.gordon.mypotato

import android.app.Application
import com.gordon.mypotato.data.AppDatabase
import com.gordon.mypotato.data.initializer.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyPotatoApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer(
                database.categoryDao(),
                database.taskDao(),
                database.taskStepDao()
            ).initializeIfNeeded()
        }
    }
}
