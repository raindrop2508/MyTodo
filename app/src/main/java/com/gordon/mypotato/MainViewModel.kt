package com.gordon.mypotato

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    init {
        _text.value = application.getString(R.string.main_hello_from_viewmodel)
    }

    fun updateText() {
        _text.value = getApplication<Application>().getString(R.string.main_text_updated)
    }
}