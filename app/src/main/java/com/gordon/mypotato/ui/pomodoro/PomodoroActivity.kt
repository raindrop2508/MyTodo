package com.gordon.mypotato.ui.pomodoro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gordon.mypotato.R

class PomodoroActivity : AppCompatActivity() {

    /**
     * 功能：初始化番茄钟占位页面。
     * 入参：savedInstanceState Bundle? 状态数据
     * 出参：无
     * 异常：无
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pomodoro)
    }
}
