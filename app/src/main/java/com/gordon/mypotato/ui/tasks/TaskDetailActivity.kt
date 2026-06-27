package com.gordon.mypotato.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityTaskDetailBinding
import com.gordon.mypotato.ui.pomodoro.PomodoroActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private var taskId: String = ""
    private var isLongTask: Boolean = false
    private var taskTitle: String = "未命名任务"
    private var category: String = "工作"
    private var urgent: Boolean = false
    private var important: Boolean = false
    private var hasInitializedSteps: Boolean = false
    private val steps = mutableListOf<StepMock>()

    companion object {
        private const val TAG = "TaskDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate in")
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getStringExtra(TaskEditActivity.EXTRA_TASK_ID) ?: ""
        isLongTask = intent.getBooleanExtra(TaskEditActivity.EXTRA_IS_LONG_TASK, false)
        taskTitle = intent.getStringExtra(TaskEditActivity.EXTRA_TASK_TITLE) ?: "未命名任务"
        category = intent.getStringExtra(TaskEditActivity.EXTRA_CATEGORY) ?: "工作"
        urgent = intent.getBooleanExtra(TaskEditActivity.EXTRA_URGENT, false)
        important = intent.getBooleanExtra(TaskEditActivity.EXTRA_IMPORTANT, false)

        setupToolbar()
        renderTaskInfo(taskTitle, category, urgent, important)

        if (isLongTask) {
            renderLongTask()
        } else {
            renderSingleTask()
        }
        Log.d(TAG, "onCreate out taskId=$taskId isLongTask=$isLongTask")
    }

    /**
     * 功能：配置顶部 Toolbar。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun setupToolbar() {
        Log.d(TAG, "setupToolbar in")
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    Log.d(TAG, "Click edit")
                    startActivity(
                        Intent(this, TaskEditActivity::class.java).apply {
                            putExtra(TaskEditActivity.EXTRA_TASK_ID, taskId)
                            putExtra(TaskEditActivity.EXTRA_TASK_TITLE, taskTitle)
                            putExtra(TaskEditActivity.EXTRA_IS_LONG_TASK, isLongTask)
                            putExtra(TaskEditActivity.EXTRA_CATEGORY, category)
                            putExtra(TaskEditActivity.EXTRA_URGENT, urgent)
                            putExtra(TaskEditActivity.EXTRA_IMPORTANT, important)
                        }
                    )
                    // TODO: 后续接入编辑结果回传并刷新详情页。
                    true
                }
                R.id.action_delete -> {
                    Log.d(TAG, "Click delete")
                    // TODO: 后续接入删除确认与删除逻辑。
                    true
                }
                else -> false
            }
        }
        Log.d(TAG, "setupToolbar out")
    }

    /**
     * 功能：渲染任务基本信息与标签。
     * 入参：
     *  taskTitle: String 任务标题
     *  category: String 分类标签文本
     *  urgent: Boolean 是否紧急
     *  important: Boolean 是否重要
     * 出参：无
     * 异常：无
     */
    private fun renderTaskInfo(taskTitle: String, category: String, urgent: Boolean, important: Boolean) {
        binding.tvTitle.text = taskTitle
        binding.tvCategory.text = category
        
        if (urgent) {
            binding.tvUrgent.visibility = View.VISIBLE
        } else {
            binding.tvUrgent.visibility = View.GONE
        }

        if (important) {
            binding.tvImportant.visibility = View.VISIBLE
        } else {
            binding.tvImportant.visibility = View.GONE
        }
    }

    /**
     * 功能：渲染单次任务特有的 UI 状态。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun renderSingleTask() {
        binding.tvType.text = "单次任务"
        binding.layoutAccumulatedTime.visibility = View.GONE
        binding.cardSteps.visibility = View.GONE
        binding.btnPomodoro.visibility = View.GONE
        binding.btnSingleTaskHint.visibility = View.VISIBLE
        binding.layoutNote.visibility = View.GONE
        
        binding.tvContent.text = "回复关于产品咨询的邮件"
        binding.tvCreateTime.text = "创建时间：2026/5/21 15:18:09"
    }

    /**
     * 功能：渲染长时任务特有的 UI 状态及步骤列表。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun renderLongTask() {
        Log.d(TAG, "renderLongTask in")
        binding.tvType.text = "长时任务"
        binding.layoutAccumulatedTime.visibility = View.VISIBLE
        binding.cardSteps.visibility = View.VISIBLE
        binding.btnPomodoro.visibility = View.VISIBLE
        binding.btnSingleTaskHint.visibility = View.GONE
        binding.layoutNote.visibility = View.VISIBLE
        
        binding.tvContent.text = "设计Todo应用的主要页面和交互流程"
        binding.tvCreateTime.text = "创建时间：2026/5/21 16:18:09"
        binding.tvAccumulatedTime.text = "累计用时：1小时0分钟"

        binding.btnPomodoro.setOnClickListener {
            startActivity(Intent(this, PomodoroActivity::class.java))
        }

        ensureMockStepsInitialized()
        setupStepActions()
        renderSteps()
        renderStepProgress()
        Log.d(TAG, "renderLongTask out stepCount=${steps.size}")
    }

    /**
     * 功能：初始化长时任务的 Mock 步骤数据，仅首次进入时执行。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun ensureMockStepsInitialized() {
        Log.d(TAG, "ensureMockStepsInitialized in hasInitializedSteps=$hasInitializedSteps")
        if (hasInitializedSteps) {
            Log.d(TAG, "ensureMockStepsInitialized skip alreadyInitialized=true")
            return
        }
        steps.clear()
        steps.addAll(
            listOf(
                StepMock("设计Today页面", "30分钟", true, "完成于 2026/5/21 14:18:09"),
                StepMock("设计Tasks页面", "20分钟", true, "完成于 2026/5/21 14:48:09"),
                StepMock("设计Statistics页面", "", false, ""),
                StepMock("设计Task Detail页面", "", false, ""),
                StepMock("设计Settings页面", "", false, ""),
            ),
        )
        hasInitializedSteps = true
        // TODO: 后续接入真实任务数据源。
        Log.d(TAG, "ensureMockStepsInitialized out stepCount=${steps.size}")
    }

    /**
     * 功能：绑定步骤区的页面级交互入口。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun setupStepActions() {
        Log.d(TAG, "setupStepActions in")
        binding.tvAddStep.setOnClickListener {
            Log.d(TAG, "setupStepActions clickAddStep")
            addStep()
        }
        Log.d(TAG, "setupStepActions out")
    }

    /**
     * 功能：向当前步骤列表追加一个新的未完成步骤。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun addStep() {
        Log.d(TAG, "addStep in currentCount=${steps.size}")
        steps.add(
            StepMock(
                title = getString(R.string.task_detail_new_step),
                time = "",
                done = false,
                doneTime = "",
            ),
        )
        // TODO: 后续接入步骤标题编辑能力。
        // TODO: 后续接入步骤删除能力。
        renderSteps()
        renderStepProgress()
        Log.d(TAG, "addStep out newCount=${steps.size}")
    }

    /**
     * 功能：根据当前步骤完成情况刷新进度数字与进度条。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun renderStepProgress() {
        Log.d(TAG, "renderStepProgress in")
        val totalCount = steps.size
        val completedCount = steps.count { it.done }
        val progressValue =
            if (totalCount == 0) {
                0
            } else {
                completedCount * 100 / totalCount
            }
        binding.tvProgress.text = getString(R.string.task_detail_progress_format, completedCount, totalCount)
        binding.pbSteps.progress = progressValue
        Log.d(TAG, "renderStepProgress out completed=$completedCount total=$totalCount progress=$progressValue")
    }

    /**
     * 功能：渲染当前步骤列表并绑定勾选联动。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun renderSteps() {
        Log.d(TAG, "renderSteps in stepCount=${steps.size}")
        binding.layoutStepList.removeAllViews()
        for (step in steps) {
            val stepView = layoutInflater.inflate(R.layout.item_step, binding.layoutStepList, false)
            val titleView = stepView.findViewById<TextView>(R.id.tv_step_title)
            titleView.text = step.title

            val cb = stepView.findViewById<CheckBox>(R.id.cb_step)
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = step.done
            cb.setOnCheckedChangeListener { _, isChecked ->
                Log.d(TAG, "renderSteps onCheckedChanged title=${step.title} isChecked=$isChecked")
                step.done = isChecked
                step.doneTime =
                    if (isChecked) {
                        buildDoneTimeText()
                    } else {
                        ""
                    }
                updateStepItemState(titleView, doneTimeView = stepView.findViewById(R.id.tv_step_done_time), step = step)
                renderStepProgress()
            }

            val timeView = stepView.findViewById<TextView>(R.id.tv_step_time)
            if (step.time.isNotEmpty()) {
                timeView.text = step.time
                timeView.visibility = View.VISIBLE
            } else {
                timeView.visibility = View.GONE
            }

            val doneTimeView = stepView.findViewById<TextView>(R.id.tv_step_done_time)
            updateStepItemState(titleView = titleView, doneTimeView = doneTimeView, step = step)
            binding.layoutStepList.addView(stepView)
        }
        Log.d(TAG, "renderSteps out")
    }

    /**
     * 功能：刷新单个步骤项的完成态展示。
     * 入参：titleView 标题控件，doneTimeView 完成时间控件，step 当前步骤数据。
     * 出参：无
     * 异常：无
     */
    private fun updateStepItemState(titleView: TextView, doneTimeView: TextView, step: StepMock) {
        Log.d(TAG, "updateStepItemState in title=${step.title} done=${step.done}")
        titleView.alpha = if (step.done) 0.5f else 1f
        if (step.doneTime.isNotEmpty()) {
            doneTimeView.text = step.doneTime
            doneTimeView.visibility = View.VISIBLE
        } else {
            doneTimeView.visibility = View.GONE
        }
        Log.d(TAG, "updateStepItemState out doneTimeVisible=${doneTimeView.visibility == View.VISIBLE}")
    }

    /**
     * 功能：生成步骤完成时间文案。
     * 入参：无
     * 出参：返回完成时间字符串。
     * 异常：无
     */
    private fun buildDoneTimeText(): String {
        Log.d(TAG, "buildDoneTimeText in")
        val formattedTime = SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.CHINA).format(Date())
        val result = getString(R.string.task_detail_done_time_format, formattedTime)
        Log.d(TAG, "buildDoneTimeText out result=$result")
        return result
    }

    private data class StepMock(
        val title: String,
        val time: String,
        var done: Boolean,
        var doneTime: String,
    )
}
