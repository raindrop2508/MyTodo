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

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getStringExtra("taskId") ?: ""
        val isLongTask = intent.getBooleanExtra("isLongTask", false)
        val taskTitle = intent.getStringExtra("taskTitle") ?: "未命名任务"
        val category = intent.getStringExtra("category") ?: "工作"
        val urgent = intent.getBooleanExtra("urgent", false)
        val important = intent.getBooleanExtra("important", false)

        setupToolbar()
        renderTaskInfo(taskTitle, category, urgent, important)

        if (isLongTask) {
            renderLongTask()
        } else {
            renderSingleTask()
        }
    }

    /**
     * 功能：配置顶部 Toolbar。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    Log.d("TaskDetailActivity", "Click edit")
                    true
                }
                R.id.action_delete -> {
                    Log.d("TaskDetailActivity", "Click delete")
                    true
                }
                else -> false
            }
        }
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

        renderSteps()
    }

    /**
     * 功能：渲染写死的任务步骤列表（仅展示用）。
     * 入参：无
     * 出参：无
     * 异常：无
     */
    private fun renderSteps() {
        val steps = listOf(
            StepMock("设计Today页面", "30分钟", true, "完成于 2026/5/21 14:18:09"),
            StepMock("设计Tasks页面", "20分钟", true, "完成于 2026/5/21 14:48:09"),
            StepMock("设计Statistics页面", "", false, ""),
            StepMock("设计Task Detail页面", "", false, ""),
            StepMock("设计Settings页面", "", false, "")
        )

        binding.layoutStepList.removeAllViews()
        for (step in steps) {
            val stepView = layoutInflater.inflate(R.layout.item_step, binding.layoutStepList, false)
            val titleView = stepView.findViewById<TextView>(R.id.tv_step_title)
            titleView.text = step.title
            
            val cb = stepView.findViewById<CheckBox>(R.id.cb_step)
            cb.isChecked = step.done
            
            val timeView = stepView.findViewById<TextView>(R.id.tv_step_time)
            if (step.time.isNotEmpty()) {
                timeView.text = step.time
                timeView.visibility = View.VISIBLE
            } else {
                timeView.visibility = View.GONE
            }

            val doneTimeView = stepView.findViewById<TextView>(R.id.tv_step_done_time)
            if (step.doneTime.isNotEmpty()) {
                doneTimeView.text = step.doneTime
                doneTimeView.visibility = View.VISIBLE
            } else {
                doneTimeView.visibility = View.GONE
            }

            if (step.done) {
                titleView.alpha = 0.5f
            }
            binding.layoutStepList.addView(stepView)
        }
    }

    private data class StepMock(val title: String, val time: String, val done: Boolean, val doneTime: String)
}
