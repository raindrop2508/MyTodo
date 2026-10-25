package com.gordon.mypotato.ui.pomodoro

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityPomodoroBinding
import com.gordon.mypotato.domain.PomodoroPhase
import com.gordon.mypotato.domain.PomodoroPrompt
import com.gordon.mypotato.ui.tasks.TaskEditActivity
import com.gordon.mypotato.ui.tasks.TaskEditActivityArgs
import com.gordon.mypotato.viewmodel.PomodoroUiState
import com.gordon.mypotato.viewmodel.PomodoroViewModel
import com.gordon.mypotato.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private lateinit var viewModel: PomodoroViewModel

    private var taskId: Long = -1L
    private var taskTitle: String = ""
    private var stepId: Long = -1L
    private var resumeSessionId: Long = -1L
    private var shownPrompt: PomodoroPrompt? = null
    private var phasePromptDialog: AlertDialog? = null

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

        if (resumeSessionId > 0L) {
            viewModel.resumeActiveSession(resumeSessionId)
        } else if (taskId != -1L) {
            if (stepId > 0L) {
                viewModel.setStep(stepId)
            }
            viewModel.loadTask(taskId)
        }
    }

    private fun readIntentExtras() {
        Log.d(TAG, "readIntentExtras in")
        val args = PomodoroActivityArgs.fromBundle(intent.extras ?: Bundle())
        taskId = args.taskId
        taskTitle = args.taskTitle
        stepId = args.stepId
        resumeSessionId = args.resumeSessionId
        Log.d(
            TAG,
            "readIntentExtras out taskId=$taskId taskTitle=$taskTitle stepId=$stepId resumeSessionId=$resumeSessionId"
        )
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
        binding.btnEndSession.setOnClickListener {
            onEndSessionClicked()
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

    private fun onEndSessionClicked() {
        val state = viewModel.uiState.value
        when (state.currentPhase) {
            PomodoroPhase.FOCUS -> viewModel.endWorkSession()
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> viewModel.endBreakEarly()
        }
    }

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
                    binding.btnEndSession.isEnabled = false
                    binding.btnEndSession.visibility = View.GONE
                    binding.tvTimer.text = "--:--"
                    binding.progressTimer.progress = 0
                    val invalidMessage = getString(R.string.pomodoro_only_long_task)
                    binding.tvFocusState.text = invalidMessage
                    Toast.makeText(this@PomodoroActivity, invalidMessage, Toast.LENGTH_LONG).show()
                    return@collect
                }

                updateTimerText(state.timeLeftMs)
                updateProgress(state.timeLeftMs, state.totalTimeMs)
                updatePhaseState(state.currentPhase)
                updateCycleCount(state.cycleCount)
                updateControls(state)
                maybeShowPhasePrompt(state.prompt)
            }
        }
    }

    private fun updateControls(state: PomodoroUiState) {
        val prompting = state.prompt != null
        binding.btnToggle.isEnabled = !prompting && state.isValidTask
        binding.btnEndSession.isEnabled = !prompting && state.hasActiveSession

        when {
            prompting -> {
                binding.btnEndSession.visibility = View.GONE
            }
            state.isIdle -> {
                binding.btnEndSession.visibility = View.GONE
            }
            state.currentPhase == PomodoroPhase.FOCUS -> {
                binding.btnEndSession.visibility = View.VISIBLE
                binding.btnEndSession.setText(R.string.pomodoro_btn_end_session)
            }
            else -> {
                binding.btnEndSession.visibility = View.VISIBLE
                binding.btnEndSession.setText(R.string.pomodoro_btn_end_break_early)
            }
        }

        updateToggleButton(state)
    }

    private fun updateToggleButton(state: PomodoroUiState) {
        when {
            state.isRunning -> {
                binding.btnToggle.setText(R.string.pomodoro_btn_pause)
                binding.btnToggle.setIconResource(R.drawable.ic_pause_24dp)
            }
            state.hasActiveSession -> {
                binding.btnToggle.setText(R.string.pomodoro_btn_resume)
                binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
            }
            else -> {
                binding.btnToggle.setText(R.string.pomodoro_btn_start)
                binding.btnToggle.setIconResource(R.drawable.ic_play_24dp)
            }
        }
    }

    private fun maybeShowPhasePrompt(prompt: PomodoroPrompt?) {
        if (prompt == null) {
            shownPrompt = null
            phasePromptDialog?.dismiss()
            phasePromptDialog = null
            return
        }
        if (shownPrompt == prompt && phasePromptDialog?.isShowing == true) {
            return
        }
        shownPrompt = prompt
        phasePromptDialog?.dismiss()

        val dialog = when (prompt) {
            PomodoroPrompt.AFTER_FOCUS -> AlertDialog.Builder(this)
                .setTitle(R.string.pomodoro_prompt_focus_title)
                .setMessage(R.string.pomodoro_prompt_focus_message)
                .setCancelable(false)
                .setPositiveButton(R.string.pomodoro_prompt_start_break) { _, _ ->
                    viewModel.confirmStartBreak()
                }
                .setNegativeButton(R.string.pomodoro_prompt_end_session) { _, _ ->
                    viewModel.confirmEndToIdle()
                }
                .create()

            PomodoroPrompt.AFTER_BREAK -> AlertDialog.Builder(this)
                .setTitle(R.string.pomodoro_prompt_break_title)
                .setMessage(R.string.pomodoro_prompt_break_message)
                .setCancelable(false)
                .setPositiveButton(R.string.pomodoro_prompt_next_focus) { _, _ ->
                    viewModel.confirmStartNextFocus()
                }
                .setNegativeButton(R.string.pomodoro_prompt_end_session) { _, _ ->
                    viewModel.confirmEndToIdle()
                }
                .create()
        }
        phasePromptDialog = dialog
        dialog.show()
    }

    private fun updateTimerText(timeLeftMs: Long) {
        val minutes = (timeLeftMs / 1000) / 60
        val seconds = (timeLeftMs / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        binding.tvTimer.text = timeFormatted
    }

    private fun updateProgress(timeLeftMs: Long, totalTimeMs: Long) {
        val max = totalTimeMs.toInt().coerceAtLeast(1)
        binding.progressTimer.max = max
        binding.progressTimer.progress = timeLeftMs.toInt().coerceIn(0, max)
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
        binding.tvPomodoroCount.text = getString(R.string.pomodoro_count_hint, count + 1)
    }

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

    override fun onDestroy() {
        phasePromptDialog?.dismiss()
        phasePromptDialog = null
        super.onDestroy()
    }
}
