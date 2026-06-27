package com.gordon.mypotato.ui.tasks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityTaskEditBinding
import com.gordon.mypotato.ui.common.EditableStep
import com.gordon.mypotato.ui.common.EditableStepAdapter
import java.util.Collections

/*
* TODO: 当前页面中没有将单次任务转换为长时任务的功能（转换后应该支持增加任务步骤）
* */
class TaskEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskEditBinding
    private lateinit var stepAdapter: EditableStepAdapter
    private val stepList = mutableListOf<EditableStep>()

    private var taskId: String = ""
    private var taskTitle: String = ""
    private var isLongTask: Boolean = false
    private var category: String = ""
    private var urgent: Boolean = false
    private var important: Boolean = false

    companion object {
        private const val TAG = "TaskEditActivity"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
        const val EXTRA_IS_LONG_TASK = "isLongTask"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_URGENT = "urgent"
        const val EXTRA_IMPORTANT = "important"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate in")
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readIntentExtras()
        setupToolbar()
        setupStepList()
        setupTypeButtons()
        setupCategoryChips()
        populateMockData()

        Log.d(TAG, "onCreate out")
    }

    /**
     * 功能：读取编辑页初始化参数。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun readIntentExtras() {
        Log.d(TAG, "readIntentExtras in")
        taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "设计任务编辑页面"
        isLongTask = intent.getBooleanExtra(EXTRA_IS_LONG_TASK, false)
        category = intent.getStringExtra(EXTRA_CATEGORY) ?: "工作"
        urgent = intent.getBooleanExtra(EXTRA_URGENT, false)
        important = intent.getBooleanExtra(EXTRA_IMPORTANT, false)
        Log.d(
            TAG,
            "readIntentExtras out taskId=$taskId taskTitle=$taskTitle isLongTask=$isLongTask category=$category urgent=$urgent important=$important",
        )
    }

    /**
     * 功能：初始化顶部 Toolbar 与占位菜单行为。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupToolbar() {
        Log.d(TAG, "setupToolbar in")
        binding.toolbar.setNavigationOnClickListener {
            Log.d(TAG, "setupToolbar clickNavigation")
            finish()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    Log.d(TAG, "setupToolbar clickSave")
                    // TODO: 后续接入任务保存逻辑。
                    Toast.makeText(this, R.string.task_edit_save_placeholder, Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        Log.d(TAG, "setupToolbar out")
    }

    /**
     * 功能：初始化步骤编辑列表与拖拽能力。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupStepList() {
        Log.d(TAG, "setupStepList in")
        stepAdapter =
            EditableStepAdapter(
                steps = stepList,
                onDeleteClick = { position ->
                    if (position !in stepList.indices) {
                        Log.d(TAG, "setupStepList skipDelete invalidPosition=$position")
                    } else {
                        stepList.removeAt(position)
                        stepAdapter.notifyItemRemoved(position)
                        updateStepSectionSummary()
                    }
                },
                onStartDrag = { viewHolder ->
                    itemTouchHelper.startDrag(viewHolder)
                },
            )
        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = stepAdapter
        itemTouchHelper.attachToRecyclerView(binding.rvSteps)
        binding.tvAddStep.setOnClickListener {
            Log.d(TAG, "setupStepList clickAddStep")
            stepList.add(EditableStep(""))
            stepAdapter.notifyItemInserted(stepList.size - 1)
            updateStepSectionSummary()
        }
        Log.d(TAG, "setupStepList out")
    }

    /**
     * 功能：初始化任务类型切换交互。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupTypeButtons() {
        Log.d(TAG, "setupTypeButtons in")
        binding.groupTaskType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isLongTask = checkedId == R.id.btn_type_long_task
            Log.d(TAG, "setupTypeButtons changed isLongTask=$isLongTask")
            renderStepSection()
        }
        Log.d(TAG, "setupTypeButtons out")
    }

    /**
     * 功能：初始化分类选择组。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupCategoryChips() {
        Log.d(TAG, "setupCategoryChips in")
        binding.groupTaskCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(R.id.chip_category_none)
                category = getString(R.string.today_bottom_sheet_category_none)
                return@setOnCheckedStateChangeListener
            }
            category =
                when (checkedIds.first()) {
                    R.id.chip_category_work -> getString(R.string.today_bottom_sheet_category_work)
                    R.id.chip_category_life -> getString(R.string.today_bottom_sheet_category_life)
                    R.id.chip_category_study -> getString(R.string.today_bottom_sheet_category_study)
                    R.id.chip_category_health -> getString(R.string.today_bottom_sheet_category_health)
                    else -> getString(R.string.today_bottom_sheet_category_none)
                }
            Log.d(TAG, "setupCategoryChips changed category=$category")
        }
        Log.d(TAG, "setupCategoryChips out")
    }

    /**
     * 功能：填充编辑页 Mock 数据并渲染当前 UI。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun populateMockData() {
        Log.d(TAG, "populateMockData in")
        binding.tvTaskId.text = getString(R.string.task_edit_task_id_format, taskId.ifBlank { "mock-task-001" })
        binding.tvMockHint.text = getString(R.string.task_edit_mock_banner)
        binding.etTaskTitle.setText(taskTitle)
        binding.etTaskContent.setText(buildMockDescription())
        binding.etTaskNote.setText(buildMockNote())
        binding.switchTaskUrgent.isChecked = urgent
        binding.switchTaskImportant.isChecked = important

        if (isLongTask) {
            binding.groupTaskType.check(R.id.btn_type_long_task)
        } else {
            binding.groupTaskType.check(R.id.btn_type_one_time)
        }
        binding.groupTaskCategory.check(resolveCategoryChipId(category))

        stepList.clear()
        stepList.addAll(buildMockSteps())
        stepAdapter.notifyDataSetChanged()
        renderStepSection()
        updateStepSectionSummary()
        Log.d(TAG, "populateMockData out stepCount=${stepList.size}")
    }

    /**
     * 功能：根据任务类型渲染步骤编辑区显隐。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun renderStepSection() {
        Log.d(TAG, "renderStepSection in isLongTask=$isLongTask")
        binding.cardSteps.visibility = if (isLongTask) View.VISIBLE else View.GONE
        Log.d(TAG, "renderStepSection out visibility=${binding.cardSteps.visibility}")
    }

    /**
     * 功能：刷新步骤区摘要文案。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun updateStepSectionSummary() {
        Log.d(TAG, "updateStepSectionSummary in")
        binding.tvStepSummary.text = getString(R.string.task_edit_step_count_format, stepList.size)
        Log.d(TAG, "updateStepSectionSummary out summary=${binding.tvStepSummary.text}")
    }

    /**
     * 功能：构建编辑页描述 Mock 数据。
     * 入参：无。
     * 出参：返回任务描述文本。
     * 异常：无。
     */
    private fun buildMockDescription(): String {
        Log.d(TAG, "buildMockDescription in")
        // TODO: 后续改为真实任务数据源。
        val description =
            if (isLongTask) {
                "梳理任务编辑页的卡片层级、交互区块和视觉风格，保持与详情页一致。"
            } else {
                "补充任务编辑页的基础信息展示和表单占位，便于后续接入真实数据。"
            }
        Log.d(TAG, "buildMockDescription out")
        return description
    }

    /**
     * 功能：构建编辑页备注 Mock 数据。
     * 入参：无。
     * 出参：返回备注文本。
     * 异常：无。
     */
    private fun buildMockNote(): String {
        Log.d(TAG, "buildMockNote in")
        // TODO: 后续改为真实任务数据源。
        val note = "当前页面仅完成 UI 骨架与占位交互，保存、回传与数据同步将在后续阶段接入。"
        Log.d(TAG, "buildMockNote out")
        return note
    }

    /**
     * 功能：根据当前任务类型生成步骤 Mock 数据。
     * 入参：无。
     * 出参：返回步骤列表。
     * 异常：无。
     */
    private fun buildMockSteps(): List<EditableStep> {
        Log.d(TAG, "buildMockSteps in isLongTask=$isLongTask")
        // TODO: 后续接入步骤增删改与排序逻辑。
        val steps =
            if (isLongTask) {
                listOf(
                    EditableStep("整理编辑页信息结构"),
                    EditableStep("补齐表单输入区域"),
                    EditableStep("统一按钮与卡片风格"),
                )
            } else {
                emptyList()
            }
        Log.d(TAG, "buildMockSteps out stepCount=${steps.size}")
        return steps
    }

    /**
     * 功能：根据分类名称映射选中的 Chip。
     * 入参：categoryName 分类名称。
     * 出参：返回对应分类 Chip 的资源 id。
     * 异常：无。
     */
    private fun resolveCategoryChipId(categoryName: String): Int {
        Log.d(TAG, "resolveCategoryChipId in categoryName=$categoryName")
        val chipId =
            when (categoryName) {
                getString(R.string.today_bottom_sheet_category_work) -> R.id.chip_category_work
                getString(R.string.today_bottom_sheet_category_life) -> R.id.chip_category_life
                getString(R.string.today_bottom_sheet_category_study) -> R.id.chip_category_study
                getString(R.string.today_bottom_sheet_category_health) -> R.id.chip_category_health
                else -> R.id.chip_category_none
            }
        Log.d(TAG, "resolveCategoryChipId out chipId=$chipId")
        return chipId
    }

    /**
     * 功能：配置步骤列表拖拽排序。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private val itemTouchHelper =
        ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition
                    if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                        return false
                    }
                    if (fromPos < toPos) {
                        for (index in fromPos until toPos) {
                            Collections.swap(stepList, index, index + 1)
                        }
                    } else {
                        for (index in fromPos downTo toPos + 1) {
                            Collections.swap(stepList, index, index - 1)
                        }
                    }
                    recyclerView.adapter?.notifyItemMoved(fromPos, toPos)
                    return true
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int,
                ) {
                    Log.d(TAG, "onSwiped ignored")
                }
            },
        )
}
