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
import com.gordon.mypotato.ui.tasks.TaskEditActivity
import com.gordon.mypotato.viewmodel.PomodoroPhase
import com.gordon.mypotato.viewmodel.PomodoroViewModel
import com.gordon.mypotato.viewmodel.ViewModelFactory
import com.gordon.mypotato.ui.tasks.TaskEditActivityArgs
import com.gordon.mypotato.domain.TaskType
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
        binding.ivEditTask.setOnClickListener {
            openTaskEdit()
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

    /**
     * 功能：打开任务编辑页面。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun openTaskEdit() {
        Log.d(TAG, "openTaskEdit in taskId=$taskId")
        if (taskId != -1L) {
            val intent = android.content.Intent(this, TaskEditActivity::class.java).apply {
                putExtras(TaskEditActivityArgs(taskId).toBundle())
            }
            startActivity(intent)
            Log.d(TAG, "openTaskEdit out taskId=$taskId")
        }
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading) {
                    return@collect
                }

                binding.tvTaskTitle.text = state.task?.title ?: taskTitle
                binding.tvTaskDesc.text = state.task?.content ?: ""

                state.task?.let { task ->
                    updateTaskTags(task)
                }

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

    /**
     * 功能：更新任务标签的显示状态。
     * 入参：task Task 对象，包含任务类型、紧急/重要属性。
     * 出参：无。
     * 异常：无。
     */
    private fun updateTaskTags(task: com.gordon.mypotato.domain.Task) {
        binding.tvTagType.text = if (task.isLongTask()) {
            getString(R.string.pomodoro_tag_long)
        } else {
            getString(R.string.pomodoro_tag_work)
        }

        binding.tvTagUrgent.visibility = if (task.isUrgent) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.tvTagImportant.visibility = if (task.isImportant) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}