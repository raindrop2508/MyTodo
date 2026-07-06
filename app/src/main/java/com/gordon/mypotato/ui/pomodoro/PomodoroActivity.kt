package com.gordon.mypotato.ui.pomodoro

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityPomodoroBinding

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding

    private var countDownTimer: CountDownTimer? = null
    private var timerState = TimerState.IDLE

    private val totalTimeMs: Long = 25 * 60 * 1000L
    private var timeLeftMs: Long = totalTimeMs

    private var taskId: Long = -1L
    private var taskTitle: String = ""

    companion object {
        private const val TAG = "PomodoroActivity"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
    }

    private enum class TimerState {
        IDLE, RUNNING, PAUSED
    }

    /**
     * 功能：初始化番茄钟页面及事件监听。
     * 入参：savedInstanceState Bundle? 状态数据
     * 出参：无
     * 异常：无
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 进入番茄钟页面")

        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readIntentExtras()
        initViews()
        setupListeners()
    }

    /**
     * 功能：读取 Intent 传入的任务信息。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun readIntentExtras() {
        Log.d(TAG, "readIntentExtras in")
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        Log.d(TAG, "readIntentExtras out taskId=$taskId taskTitle=$taskTitle")
    }

    /**
     * 功能：初始化视图状态。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun initViews() {
        Log.d(TAG, "initViews: 开始初始化视图")
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (taskTitle.isNotEmpty()) {
            binding.tvTaskTitle.text = taskTitle
        }

        updateTimerText()
        binding.progressTimer.max = totalTimeMs.toInt()
        binding.progressTimer.progress = totalTimeMs.toInt()
        Log.d(TAG, "initViews: 视图初始化完成")
    }

    /**
     * 功能：设置按钮点击事件监听。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun setupListeners() {
        Log.d(TAG, "setupListeners: 设置事件监听")
        binding.btnToggle.setOnClickListener {
            toggleTimer()
        }
        binding.btnReset.setOnClickListener {
            resetTimer()
        }
    }

    /**
     * 功能：切换计时器状态（开始/暂停）。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun toggleTimer() {
        Log.d(TAG, "toggleTimer: 当前状态 = $timerState")
        if (timerState == TimerState.RUNNING) {
            pauseTimer()
            return
        }
        startTimer()
    }

    /**
     * 功能：开始计时器。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun startTimer() {
        Log.d(TAG, "startTimer: 开始计时，剩余时间 = $timeLeftMs")
        if (timerState == TimerState.RUNNING) return

        countDownTimer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                updateTimerText()
                binding.progressTimer.progress = millisUntilFinished.toInt()
            }

            override fun onFinish() {
                Log.d(TAG, "onFinish: 计时结束")
                timeLeftMs = 0
                timerState = TimerState.IDLE
                updateTimerText()
                binding.progressTimer.progress = 0
                binding.btnToggle.setText(R.string.pomodoro_btn_start)
                binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
            }
        }.start()

        timerState = TimerState.RUNNING
        binding.btnToggle.setText(R.string.pomodoro_btn_pause)
        binding.btnToggle.setIconResource(R.drawable.ic_pause_24dp)
        Log.d(TAG, "startTimer: 计时器已启动")
    }

    /**
     * 功能：暂停计时器。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun pauseTimer() {
        Log.d(TAG, "pauseTimer: 暂停计时")
        if (timerState != TimerState.RUNNING) return

        countDownTimer?.cancel()
        timerState = TimerState.PAUSED
        binding.btnToggle.setText(R.string.pomodoro_btn_resume)
        binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
        Log.d(TAG, "pauseTimer: 计时器已暂停")
    }

    /**
     * 功能：重置计时器至初始状态。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun resetTimer() {
        Log.d(TAG, "resetTimer: 重置计时器")
        countDownTimer?.cancel()
        timeLeftMs = totalTimeMs
        timerState = TimerState.IDLE
        updateTimerText()
        binding.progressTimer.progress = totalTimeMs.toInt()
        binding.btnToggle.setText(R.string.pomodoro_btn_start)
        binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
        Log.d(TAG, "resetTimer: 重置完成")
    }

    /**
     * 功能：更新倒计时显示的文本。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun updateTimerText() {
        val minutes = (timeLeftMs / 1000) / 60
        val seconds = (timeLeftMs / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        binding.tvTimer.text = timeFormatted
    }

    /**
     * 功能：页面销毁时释放资源。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 释放计时器资源")
        countDownTimer?.cancel()
    }
}