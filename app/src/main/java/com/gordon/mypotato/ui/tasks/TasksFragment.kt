package com.gordon.mypotato.ui.tasks

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.BottomSheetAddTaskPlaceholderBinding
import com.gordon.mypotato.databinding.FragmentTasksBinding
import com.gordon.mypotato.databinding.ItemTodayTaskBinding

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private var _binding: FragmentTasksBinding? = null
    private val binding: FragmentTasksBinding
        get() = _binding!!

    private val allTasks: MutableList<TaskUiModel> = buildMockTasks().toMutableList()
    private val adapter = TasksAdapter(::onTaskClicked, ::onTaskCheckChanged)
    private var keyword: String = ""

    private enum class FilterDimension {
        CATEGORY,
        QUADRANT,
        STATUS,
    }

    private var currentDimension: FilterDimension = FilterDimension.CATEGORY
    private var currentCategory: String = "全部"
    private var currentQuadrant: String = "全部"
    private var currentStatus: String = "全部"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupTaskList()
        setupSearch()
        setupDimensionTabs()
        setupChips()
        setupFab()
        renderTasks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 功能：初始化 Tasks 页标题与总数文案。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupHeader() {
        val total = allTasks.size
        Log.d(TAG, "setupHeader in total=$total")
        binding.tvTasksTitle.text = getString(R.string.tasks_title)
        Log.d(TAG, "setupHeader out title=${binding.tvTasksTitle.text}")
    }

    /**
     * 功能：初始化任务列表与适配器。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupTaskList() {
        Log.d(TAG, "setupTaskList in")
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter
        Log.d(TAG, "setupTaskList out adapterReady=${binding.rvTasks.adapter != null}")
    }

    /**
     * 功能：绑定搜索输入，实时触发标题模糊过滤。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupSearch() {
        Log.d(TAG, "setupSearch in")
        binding.etTasksSearch.doAfterTextChanged { editable ->
            keyword = editable?.toString()?.trim().orEmpty()
            Log.d(TAG, "setupSearch changed keyword=$keyword")
            renderTasks()
        }
        Log.d(TAG, "setupSearch out")
    }

    /**
     * 功能：绑定维度分段控制器监听。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupDimensionTabs() {
        Log.d(TAG, "setupDimensionTabs in")
        binding.rgDimension.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_dim_category -> switchDimension(FilterDimension.CATEGORY)
                R.id.rb_dim_quadrant -> switchDimension(FilterDimension.QUADRANT)
                R.id.rb_dim_status -> switchDimension(FilterDimension.STATUS)
            }
        }
    }

    private fun switchDimension(dimension: FilterDimension) {
        currentDimension = dimension
        binding.chipGroupCategory.visibility = if (dimension == FilterDimension.CATEGORY) View.VISIBLE else View.GONE
        binding.chipGroupQuadrant.visibility = if (dimension == FilterDimension.QUADRANT) View.VISIBLE else View.GONE
        binding.chipGroupStatus.visibility = if (dimension == FilterDimension.STATUS) View.VISIBLE else View.GONE
        renderTasks()
    }

    /**
     * 功能：绑定分类筛选 Chip 监听。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupChips() {
        Log.d(TAG, "setupChips in")
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_cat_all
            currentCategory =
                when (checkedId) {
                    R.id.chip_cat_study -> "学习"
                    R.id.chip_cat_work -> "工作"
                    R.id.chip_cat_life -> "生活"
                    R.id.chip_cat_health -> "健康"
                    else -> "全部"
                }
            if (currentDimension == FilterDimension.CATEGORY) renderTasks()
        }

        binding.chipGroupQuadrant.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_quadrant_all
            currentQuadrant =
                when (checkedId) {
                    R.id.chip_quadrant_ui -> "紧急且重要"
                    R.id.chip_quadrant_i -> "重要"
                    R.id.chip_quadrant_u -> "紧急"
                    R.id.chip_quadrant_other -> "其他"
                    else -> "全部"
                }
            if (currentDimension == FilterDimension.QUADRANT) renderTasks()
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_status_all
            currentStatus =
                when (checkedId) {
                    R.id.chip_status_done -> "已完成"
                    R.id.chip_status_doing -> "进行中"
                    R.id.chip_status_todo -> "未完成"
                    else -> "全部"
                }
            if (currentDimension == FilterDimension.STATUS) renderTasks()
        }
    }

    /**
     * 功能：绑定 FAB 点击并展示新任务 BottomSheet。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupFab() {
        Log.d(TAG, "setupFab in")
        binding.fabAddTask.setOnClickListener {
            Log.d(TAG, "setupFab click showAddTaskBottomSheet")
            showAddTaskBottomSheet()
        }
        Log.d(TAG, "setupFab out")
    }

    /**
     * 功能：根据搜索与多条件筛选渲染任务列表。
     * 入参：无（使用当前筛选状态字段）。
     * 出参：无。
     * 异常：无。
     */
    private fun renderTasks() {
        Log.d(TAG, "renderTasks in dimension=$currentDimension keyword=$keyword")
        val filtered =
            allTasks
                .filter { task ->
                    if (keyword.isNotBlank() && !task.title.contains(keyword, ignoreCase = true)) return@filter false

                    when (currentDimension) {
                        FilterDimension.CATEGORY -> {
                            if (currentCategory != "全部" && task.category != currentCategory) return@filter false
                        }

                        FilterDimension.QUADRANT -> {
                            if (currentQuadrant != "全部") {
                                val isUI = task.urgent && task.important
                                val isI = !task.urgent && task.important
                                val isU = task.urgent && !task.important
                                val isOther = !task.urgent && !task.important

                                val match =
                                    when (currentQuadrant) {
                                        "紧急且重要" -> isUI
                                        "重要" -> isI
                                        "紧急" -> isU
                                        "其他" -> isOther
                                        else -> true
                                    }
                                if (!match) return@filter false
                            }
                        }

                        FilterDimension.STATUS -> {
                            if (currentStatus != "全部") {
                                val match =
                                    when (currentStatus) {
                                        "已完成" -> task.done
                                        "进行中" -> !task.done
                                        "未完成" -> !task.done
                                        else -> true
                                    }
                                if (!match) return@filter false
                            }
                        }
                    }
                    true
                }.sortedBy { it.done }
        adapter.submitData(filtered)
        binding.layoutTasksEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        Log.d(TAG, "renderTasks out filteredCount=${filtered.size}")
    }

    /**
     * 功能：处理任务卡片点击并跳转到任务详情占位页。
     * 入参：task 当前点击任务。
     * 出参：无。
     * 异常：无。
     */
    private fun onTaskClicked(task: TaskUiModel) {
        Log.d(TAG, "onTaskClicked in taskId=${task.id} title=${task.title}")
        val intent =
            android.content.Intent(requireContext(), TaskDetailActivity::class.java).apply {
                putExtra("taskId", task.id)
                putExtra("taskTitle", task.title)
                putExtra("isLongTask", task.isLongTask)
                putExtra("category", task.category)
                putExtra("urgent", task.urgent)
                putExtra("important", task.important)
            }
        startActivity(intent)
        Log.d(TAG, "onTaskClicked out navigated=true")
    }

    /**
     * 功能：处理任务勾选状态变更，更新数据并重新排序列表。
     * 入参：task 当前任务，isChecked 勾选状态。
     * 出参：无。
     * 异常：无。
     */
    private fun onTaskCheckChanged(
        task: TaskUiModel,
        isChecked: Boolean,
    ) {
        Log.d(TAG, "onTaskCheckChanged id=${task.id} isChecked=$isChecked")
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index] = allTasks[index].copy(done = isChecked)
            renderTasks()
        }
    }

    /**
     * 功能：展示“新增任务”BottomSheet 最小实现内容，支持字段输入、选中态切换与标题必填校验。
     * 入参：无。
     * 出参：无。
     * 异常：无，控件缺失时将直接返回，避免崩溃。
     */
    private fun showAddTaskBottomSheet() {
        Log.d(TAG, "showAddTaskBottomSheet in")
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.bottom_sheet_add_task_placeholder, null)
        val sheetBinding = BottomSheetAddTaskPlaceholderBinding.bind(content)

        var selectedType = "one-time"
        var selectedCategory = "none"
        var urgent = false
        var important = false

        sheetBinding.groupTaskType.check(R.id.btn_type_one_time)
        sheetBinding.groupTaskType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedType = if (checkedId == R.id.btn_type_long_task) "long" else "one-time"
            Log.d(TAG, "showAddTaskBottomSheet typeChanged selectedType=$selectedType")
        }

        sheetBinding.groupTaskCategory.check(R.id.chip_category_none)
        sheetBinding.groupTaskCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(R.id.chip_category_none)
                return@setOnCheckedStateChangeListener
            }
            selectedCategory =
                when (checkedIds.first()) {
                    R.id.chip_category_work -> "work"
                    R.id.chip_category_life -> "life"
                    R.id.chip_category_study -> "study"
                    R.id.chip_category_health -> "health"
                    else -> "none"
                }
            Log.d(TAG, "showAddTaskBottomSheet categoryChanged selectedCategory=$selectedCategory")
        }

        sheetBinding.switchTaskUrgent.setOnCheckedChangeListener { _, isChecked ->
            urgent = isChecked
            Log.d(TAG, "showAddTaskBottomSheet urgentChanged urgent=$urgent")
        }

        sheetBinding.switchTaskImportant.setOnCheckedChangeListener { _, isChecked ->
            important = isChecked
            Log.d(TAG, "showAddTaskBottomSheet importantChanged important=$important")
        }

        sheetBinding.btnBottomSheetClose.setOnClickListener {
            Log.d(TAG, "showAddTaskBottomSheet clickClose")
            dialog.dismiss()
        }

        sheetBinding.btnBottomSheetCancel.setOnClickListener {
            Log.d(TAG, "showAddTaskBottomSheet clickCancel")
            dialog.dismiss()
        }

        sheetBinding.btnBottomSheetCreate.setOnClickListener {
            val title =
                sheetBinding.etTaskTitle.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            Log.d(
                TAG,
                "showAddTaskBottomSheet clickSave title=$title type=$selectedType category=$selectedCategory urgent=$urgent important=$important",
            )
            if (title.isBlank()) {
                sheetBinding.tilTaskTitle.error = getString(R.string.today_bottom_sheet_title_required)
                Log.d(TAG, "showAddTaskBottomSheet saveBlocked reason=blankTitle")
                return@setOnClickListener
            }
            sheetBinding.tilTaskTitle.error = null
            val description =
                sheetBinding.etTaskContent.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            val note =
                sheetBinding.etTaskNote.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            Log.d(
                TAG,
                "showAddTaskBottomSheet savePayload title=$title description=$description note=$note type=$selectedType category=$selectedCategory urgent=$urgent important=$important",
            )
            // TODO: 接入新建任务表单保存逻辑（Room/Repository）。
            dialog.dismiss()
        }

        dialog.setContentView(content)
        dialog.show()
        Log.d(TAG, "showAddTaskBottomSheet out shown=true")
    }

    /**
     * 功能：解析主题属性对应的颜色值。
     * 入参：attrRes 主题属性资源 id。
     * 出参：返回解析后的颜色值。
     * 异常：无，解析失败返回主题主色。
     */
    private fun resolveThemeColor(attrRes: Int): Int {
        Log.d(TAG, "resolveThemeColor in attrRes=$attrRes")
        val outValue = TypedValue()
        val resolved = requireContext().theme.resolveAttribute(attrRes, outValue, true)
        if (resolved) {
            Log.d(TAG, "resolveThemeColor out color=${outValue.data}")
            return outValue.data
        }
        val fallback = ContextCompat.getColor(requireContext(), R.color.purple_500)
        Log.d(TAG, "resolveThemeColor out fallback=$fallback")
        return fallback
    }

    /**
     * 功能：根据任务紧急与重要字段计算优先级。
     * 入参：task 任务数据。
     * 出参：优先级枚举。
     * 异常：无。
     */
    private fun getPriorityOf(task: TaskUiModel): PriorityFilter {
        Log.d(TAG, "getPriorityOf in taskId=${task.id}")
        if (task.urgent && task.important) return PriorityFilter.UI
        if (task.important) return PriorityFilter.I
        if (task.urgent) return PriorityFilter.U
        return PriorityFilter.N
    }

    /**
     * 功能：构建 Tasks 页写死任务数据（与 Today 同源字段）。
     * 入参：无。
     * 出参：返回任务列表。
     * 异常：无。
     */
    private fun buildMockTasks(): List<TaskUiModel> {
        Log.d(TAG, "buildMockTasks in")
        val result =
            listOf(
                TaskUiModel(
                    id = "task_001",
                    title = "完成项目原型设计",
                    content = "同步项目进度和风险事项",
                    done = false,
                    isLongTask = true,
                    urgent = true,
                    important = true,
                    category = "工作",
                    minutes = 60,
                    steps = 5,
                ),
                TaskUiModel(
                    id = "task_002",
                    title = "购买生活用品",
                    content = "准备评审议程与需求变更说明",
                    done = true,
                    isLongTask = false,
                    urgent = false,
                    important = true,
                    category = "购物",
                ),
                TaskUiModel(
                    id = "task_003",
                    title = "回复客户邮件",
                    content = "联系医院并确认可预约时段",
                    done = false,
                    isLongTask = false,
                    urgent = true,
                    important = false,
                    category = "工作",
                ),
                TaskUiModel(
                    id = "task_004",
                    title = "学习React新特性",
                    content = "输入输出模型相关文章",
                    done = true,
                    isLongTask = true,
                    urgent = false,
                    important = false,
                    category = "学习",
                    minutes = 120,
                    steps = 3,
                ),
            )
        Log.d(TAG, "buildMockTasks out count=${result.size}")
        return result
    }

    private data class TaskUiModel(
        val id: String,
        val title: String,
        val content: String?,
        val done: Boolean,
        val isLongTask: Boolean,
        val urgent: Boolean,
        val important: Boolean,
        val category: String = "工作",
        val minutes: Int = 0,
        val steps: Int = 0,
    )

    private enum class StatusFilter {
        ALL,
        TODO,
        DONE,
    }

    private enum class TypeFilter {
        ALL,
        ONE_TIME,
        LONG,
    }

    private enum class PriorityFilter {
        ALL,
        UI,
        I,
        U,
        N,
    }

    private class TasksAdapter(
        private val onItemClick: (TaskUiModel) -> Unit,
        private val onCheckChanged: (TaskUiModel, Boolean) -> Unit,
    ) : RecyclerView.Adapter<TasksAdapter.TasksViewHolder>() {
        private val data = mutableListOf<TaskUiModel>()

        fun submitData(newData: List<TaskUiModel>) {
            data.clear()
            data.addAll(newData)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): TasksViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemTodayTaskBinding.inflate(inflater, parent, false)
            return TasksViewHolder(binding, onItemClick, onCheckChanged)
        }

        override fun onBindViewHolder(
            holder: TasksViewHolder,
            position: Int,
        ) {
            holder.bind(data[position])
        }

        override fun getItemCount(): Int = data.size

        class TasksViewHolder(
            private val binding: ItemTodayTaskBinding,
            private val onItemClick: (TaskUiModel) -> Unit,
            private val onCheckChanged: (TaskUiModel, Boolean) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: TaskUiModel) {
                binding.tvTaskTitle.text = item.title
                if (item.done) {
                    binding.tvTaskTitle.alpha = 0.5f
                } else {
                    binding.tvTaskTitle.alpha = 1.0f
                }

                binding.tvCategoryTag.text = item.category
                bindCategoryColor(item.category, binding.tvCategoryTag)

                binding.tvTypeTag.text =
                    if (item.isLongTask) {
                        binding.root.context.getString(R.string.today_task_type_long)
                    } else {
                        binding.root.context.getString(R.string.today_task_type_once)
                    }

                if (item.minutes > 0) {
                    binding.tvTimeTag.visibility = View.VISIBLE
                    binding.tvTimeTag.text = "${item.minutes}分钟"
                } else {
                    binding.tvTimeTag.visibility = View.GONE
                }

                if (item.steps > 0) {
                    binding.tvStepTag.visibility = View.VISIBLE
                    binding.tvStepTag.text = "${item.steps}个步骤"
                } else {
                    binding.tvStepTag.visibility = View.GONE
                }

                binding.cbTaskDone.setOnCheckedChangeListener(null)
                binding.cbTaskDone.isChecked = item.done
                binding.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
                    onCheckChanged(item, isChecked)
                }

                binding.root.setOnClickListener { onItemClick(item) }
            }

            private fun bindCategoryColor(
                category: String,
                textView: android.widget.TextView,
            ) {
                val context = textView.context
                val bgRes: Int
                val textRes: Int
                when (category) {
                    "工作" -> {
                        bgRes = R.color.tag_work_bg
                        textRes = R.color.tag_work_text
                    }

                    "购物" -> {
                        bgRes = R.color.tag_shopping_bg
                        textRes = R.color.tag_shopping_text
                    }

                    "学习" -> {
                        bgRes = R.color.tag_study_bg
                        textRes = R.color.tag_study_text
                    }

                    "健康" -> {
                        bgRes = R.color.tag_health_bg
                        textRes = R.color.tag_health_text
                    }

                    else -> {
                        bgRes = R.color.tag_default_bg
                        textRes = R.color.tag_default_text
                    }
                }
                textView.backgroundTintList = ContextCompat.getColorStateList(context, bgRes)
                textView.setTextColor(ContextCompat.getColor(context, textRes))
            }
        }
    }

    private companion object {
        private const val TAG = "TasksFragment"
    }
}
