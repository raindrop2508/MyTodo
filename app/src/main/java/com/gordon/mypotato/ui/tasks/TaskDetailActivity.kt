package com.gordon.mypotato.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityTaskDetailBinding
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.ui.pomodoro.PomodoroActivity
import com.gordon.mypotato.viewmodel.TaskDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var viewModel: TaskDetailViewModel
    private var taskId: Long = -1

    companion object {
        private const val TAG = "TaskDetailActivity"
        const val EXTRA_TASK_ID = "taskId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate in")
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TaskDetailViewModel::class.java]
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        setupToolbar()
        setupListeners()
        collectUiState()

        if (taskId != -1L) {
            viewModel.loadTask(taskId)
        } else {
            Log.e(TAG, "Invalid taskId provided")
            finish()
        }
        Log.d(TAG, "onCreate out taskId=$taskId")
    }

    private fun setupToolbar() {
        Log.d(TAG, "setupToolbar in")
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    Log.d(TAG, "Click edit")
                    val task = viewModel.uiState.value.task
                    if (task != null) {
                        startActivity(
                            Intent(this, TaskEditActivity::class.java).apply {
                                putExtra(TaskEditActivity.EXTRA_TASK_ID, task.id)
                            }
                        )
                    }
                    true
                }
                R.id.action_delete -> {
                    Log.d(TAG, "Click delete")
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
        Log.d(TAG, "setupToolbar out")
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.task_detail_delete_title)
            .setMessage(R.string.task_detail_delete_message)
            .setPositiveButton(R.string.task_detail_delete_confirm) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteTask(taskId)
                }
            }
            .setNegativeButton(R.string.task_detail_delete_cancel, null)
            .show()
    }

    private fun setupListeners() {
        binding.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val task = viewModel.uiState.value.task
                if (task != null && task.isCompleted() != isChecked) {
                    viewModel.toggleTaskStatus(taskId)
                }
            }
        }

        binding.tvAddStep.setOnClickListener {
            showAddStepDialog()
        }

        binding.btnPomodoro.setOnClickListener {
            val task = viewModel.uiState.value.task
            if (task != null) {
                startActivity(Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("taskId", task.id)
                    putExtra("taskTitle", task.title)
                })
            }
        }
    }

    private fun showAddStepDialog() {
        val input = android.widget.EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.task_detail_new_step)
            .setView(input)
            .setPositiveButton(R.string.today_bottom_sheet_create_btn) { _, _ ->
                val title = input.text.toString()
                if (title.isNotBlank()) {
                    viewModel.addStep(title)
                }
            }
            .setNegativeButton(R.string.today_bottom_sheet_cancel_btn, null)
            .show()
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isDeleted) {
                    finish()
                    return@collect
                }
                if (state.isLoading) {
                    return@collect
                }
                state.task?.let { renderTask(it, state.category, state.steps) }
            }
        }
    }

    private fun renderTask(task: Task, category: Category?, steps: List<TaskStep>) {
        binding.tvTitle.text = task.title
        binding.tvTitle.alpha = if (task.isCompleted()) 0.5f else 1f
        
        binding.cbTaskDone.setOnCheckedChangeListener(null)
        binding.cbTaskDone.isChecked = task.isCompleted()
        binding.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { viewModel.toggleTaskStatus(taskId) }
        }

        renderCategoryAndTags(task, category)
        binding.tvContent.text = task.content ?: ""
        binding.tvCreateTime.text = getString(R.string.task_detail_create_time_format, formatTime(task.createdAt * 1000))
        
        if (task.isLongTask()) {
            renderLongTaskInfo(task, steps)
        } else {
            renderSingleTaskInfo()
        }
    }

    private fun renderCategoryAndTags(task: Task, category: Category?) {
        binding.tvCategory.text = category?.name ?: getString(R.string.today_bottom_sheet_category_none)
        if (category != null) {
            try {
                val bgColor = android.graphics.Color.parseColor(category.colorHex)
                binding.tvCategory.backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
                val luminance = android.graphics.Color.luminance(bgColor)
                val textColor = if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                binding.tvCategory.setTextColor(textColor)
            } catch (e: Exception) {
            }
        }

        binding.tvUrgent.visibility = if (task.isUrgent) View.VISIBLE else View.GONE
        binding.tvImportant.visibility = if (task.isImportant) View.VISIBLE else View.GONE
    }

    private fun renderSingleTaskInfo() {
        binding.tvType.text = getString(R.string.today_task_type_once)
        binding.layoutAccumulatedTime.visibility = View.GONE
        binding.cardSteps.visibility = View.GONE
        binding.btnPomodoro.visibility = View.GONE
        binding.btnSingleTaskHint.visibility = View.VISIBLE
    }

    private fun renderLongTaskInfo(task: Task, steps: List<TaskStep>) {
        binding.tvType.text = getString(R.string.today_task_type_long)
        binding.layoutAccumulatedTime.visibility = View.VISIBLE
        binding.cardSteps.visibility = View.VISIBLE
        binding.btnPomodoro.visibility = View.VISIBLE
        binding.btnSingleTaskHint.visibility = View.GONE

        binding.tvAccumulatedTime.text = getString(R.string.task_detail_accumulated_time_format, task.totalDurationSec / 60)

        renderSteps(steps)
        renderStepProgress(steps)
    }

    private fun renderStepProgress(steps: List<TaskStep>) {
        val totalCount = steps.size
        val completedCount = steps.count { it.status == com.gordon.mypotato.domain.StepStatus.COMPLETED.value }
        val progressValue = if (totalCount == 0) 0 else completedCount * 100 / totalCount
        binding.tvProgress.text = getString(R.string.task_detail_progress_format, completedCount, totalCount)
        binding.pbSteps.progress = progressValue
    }

    private fun renderSteps(steps: List<TaskStep>) {
        binding.layoutStepList.removeAllViews()
        for (step in steps) {
            val stepView = layoutInflater.inflate(R.layout.item_step, binding.layoutStepList, false)
            val titleView = stepView.findViewById<TextView>(R.id.tv_step_title)
            titleView.text = step.title

            val cb = stepView.findViewById<CheckBox>(R.id.cb_step)
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = step.status == com.gordon.mypotato.domain.StepStatus.COMPLETED.value
            cb.setOnCheckedChangeListener { _, _ ->
                viewModel.toggleStepStatus(step.id)
            }

            val timeView = stepView.findViewById<TextView>(R.id.tv_step_time)
            if (step.spentDurationSec > 0) {
                timeView.text = "${step.spentDurationSec / 60}分钟"
                timeView.visibility = View.VISIBLE
            } else {
                timeView.visibility = View.GONE
            }

            val doneTimeView = stepView.findViewById<TextView>(R.id.tv_step_done_time)
            updateStepItemState(titleView, doneTimeView, step)
            binding.layoutStepList.addView(stepView)
        }
    }

    private fun updateStepItemState(titleView: TextView, doneTimeView: TextView, step: TaskStep) {
        val isDone = step.status == com.gordon.mypotato.domain.StepStatus.COMPLETED.value
        titleView.alpha = if (isDone) 0.5f else 1f
        if (isDone && step.completedAt != null) {
            doneTimeView.text = getString(R.string.task_detail_done_time_format, formatTime(step.completedAt!! * 1000))
            doneTimeView.visibility = View.VISIBLE
        } else {
            doneTimeView.visibility = View.GONE
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.CHINA).format(Date(timestamp))
    }
}
