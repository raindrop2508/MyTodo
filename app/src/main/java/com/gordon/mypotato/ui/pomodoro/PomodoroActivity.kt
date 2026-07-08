package com.gordon.mypotato.ui.pomodoro

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityPomodoroBinding
import com.gordon.mypotato.viewmodel.PomodoroPhase
import com.gordon.mypotato.viewmodel.PomodoroViewModel
import com.gordon.mypotato.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private lateinit var viewModel: PomodoroViewModel

    private var taskId: Long = -1L
    private var taskTitle: String = ""

    companion object {
        private const val TAG = "PomodoroActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 进入番茄钟页面")

        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(this))[PomodoroViewModel::class.java]

        readIntentExtras()
        initViews()
        setupListeners()
        collectUiState()

        if (taskId != -1L) {
            viewModel.loadTask(taskId)
        }
    }

    private fun readIntentExtras() {
        Log.d(TAG, "readIntentExtras in")
        val args = PomodoroActivityArgs.fromBundle(intent.extras ?: Bundle())
        taskId = args.taskId
        taskTitle = args.taskTitle
        Log.d(TAG, "readIntentExtras out taskId=$taskId taskTitle=$taskTitle")
    }

    private fun initViews() {
        Log.d(TAG, "initViews: 开始初始化视图")
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (taskTitle.isNotEmpty()) {
            binding.tvTaskTitle.text = taskTitle
        }

        Log.d(TAG, "initViews: 视图初始化完成")
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners: 设置事件监听")
        binding.btnToggle.setOnClickListener {
            toggleTimer()
        }
        binding.btnReset.setOnClickListener {
            viewModel.resetTimer()
        }
    }

    private fun toggleTimer() {
        Log.d(TAG, "toggleTimer")
        if (viewModel.uiState.value.isRunning) {
            viewModel.pauseTimer()
        } else {
            viewModel.startTimer()
        }
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading) {
                    return@collect
                }

                binding.tvTaskTitle.text = state.task?.title ?: taskTitle

                if (!state.isValidTask) {
                    binding.btnToggle.isEnabled = false
                    binding.btnReset.isEnabled = false
                    binding.tvTimer.text = "--:--"
                    binding.progressTimer.progress = 0
                    binding.tvFocusState.text = state.errorMessage ?: getString(R.string.pomodoro_invalid_task)
                    Toast.makeText(this@PomodoroActivity, state.errorMessage, Toast.LENGTH_LONG).show()
                    return@collect
                }

                binding.btnToggle.isEnabled = true
                binding.btnReset.isEnabled = true

                updateTimerText(state.timeLeftMs)
                updateProgress(state.timeLeftMs, state.totalTimeMs)
                updatePhaseState(state.currentPhase)
                updateCycleCount(state.cycleCount)
                updateToggleButton(state.isRunning)
            }
        }
    }

    private fun updateTimerText(timeLeftMs: Long) {
        val minutes = (timeLeftMs / 1000) / 60
        val seconds = (timeLeftMs / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        binding.tvTimer.text = timeFormatted
    }

    private fun updateProgress(timeLeftMs: Long, totalTimeMs: Long) {
        binding.progressTimer.max = totalTimeMs.toInt()
        binding.progressTimer.progress = timeLeftMs.toInt()
    }

    private fun updatePhaseState(phase: PomodoroPhase) {
        when (phase) {
            PomodoroPhase.FOCUS -> {
                binding.tvFocusState.text = getString(R.string.pomodoro_state_focus)
                binding.progressTimer.setIndicatorColor(getColor(R.color.state_error_red))
            }
            PomodoroPhase.SHORT_BREAK -> {
                binding.tvFocusState.text = getString(R.string.pomodoro_state_short_break)
                binding.progressTimer.setIndicatorColor(getColor(R.color.brand_indigo))
            }
            PomodoroPhase.LONG_BREAK -> {
                binding.tvFocusState.text = getString(R.string.pomodoro_state_long_break)
                binding.progressTimer.setIndicatorColor(getColor(R.color.state_chart_orange))
            }
        }
    }

    private fun updateCycleCount(count: Int) {
        binding.tvPomodoroCount.text = getString(R.string.pomodoro_count_hint, count)
    }

    private fun updateToggleButton(isRunning: Boolean) {
        if (isRunning) {
            binding.btnToggle.setText(R.string.pomodoro_btn_pause)
            binding.btnToggle.setIconResource(R.drawable.ic_pause_24dp)
        } else {
            binding.btnToggle.setText(R.string.pomodoro_btn_start)
            binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
        }
    }
}