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
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentTasksBinding
import com.gordon.mypotato.databinding.ItemTodayTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private var _binding: FragmentTasksBinding? = null
    private val binding: FragmentTasksBinding
        get() = _binding!!

    private val allTasks: List<TaskUiModel> = buildMockTasks()
    private val adapter = TasksAdapter(::onTaskClicked)
    private var statusFilter: StatusFilter = StatusFilter.ALL
    private var typeFilter: TypeFilter = TypeFilter.ALL
    private var priorityFilter: PriorityFilter = PriorityFilter.ALL
    private var keyword: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupTaskList()
        setupSearch()
        setupStatusFilter()
        setupTypeFilter()
        setupPriorityFilter()
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
        binding.tvTasksTotal.text = getString(R.string.tasks_total_format, total)
        Log.d(TAG, "setupHeader out title=${binding.tvTasksTitle.text} total=${binding.tvTasksTotal.text}")
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
     * 功能：绑定完成状态筛选 Chip 监听。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupStatusFilter() {
        Log.d(TAG, "setupStatusFilter in")
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_status_all
            statusFilter = when (checkedId) {
                R.id.chip_status_todo -> StatusFilter.TODO
                R.id.chip_status_done -> StatusFilter.DONE
                else -> StatusFilter.ALL
            }
            Log.d(TAG, "setupStatusFilter out checkedId=$checkedId statusFilter=$statusFilter")
            renderTasks()
        }
    }

    /**
     * 功能：绑定任务类型筛选 Chip 监听。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupTypeFilter() {
        Log.d(TAG, "setupTypeFilter in")
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_type_all
            typeFilter = when (checkedId) {
                R.id.chip_type_one_time -> TypeFilter.ONE_TIME
                R.id.chip_type_long -> TypeFilter.LONG
                else -> TypeFilter.ALL
            }
            Log.d(TAG, "setupTypeFilter out checkedId=$checkedId typeFilter=$typeFilter")
            renderTasks()
        }
    }

    /**
     * 功能：绑定优先级筛选 Chip 监听。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupPriorityFilter() {
        Log.d(TAG, "setupPriorityFilter in")
        binding.chipGroupPriority.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_priority_all
            priorityFilter = when (checkedId) {
                R.id.chip_priority_ui -> PriorityFilter.UI
                R.id.chip_priority_i -> PriorityFilter.I
                R.id.chip_priority_u -> PriorityFilter.U
                R.id.chip_priority_n -> PriorityFilter.N
                else -> PriorityFilter.ALL
            }
            Log.d(TAG, "setupPriorityFilter out checkedId=$checkedId priorityFilter=$priorityFilter")
            renderTasks()
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
        Log.d(
            TAG,
            "renderTasks in statusFilter=$statusFilter typeFilter=$typeFilter priorityFilter=$priorityFilter keyword=$keyword"
        )
        val filtered = allTasks.filter { task ->
            if (statusFilter == StatusFilter.TODO && task.done) return@filter false
            if (statusFilter == StatusFilter.DONE && !task.done) return@filter false
            if (typeFilter == TypeFilter.ONE_TIME && task.isLongTask) return@filter false
            if (typeFilter == TypeFilter.LONG && !task.isLongTask) return@filter false
            if (priorityFilter != PriorityFilter.ALL && getPriorityOf(task) != priorityFilter) return@filter false
            if (keyword.isNotBlank() && !task.title.contains(keyword, ignoreCase = true)) return@filter false
            true
        }
        adapter.submitData(filtered)
        binding.layoutTasksEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        Log.d(TAG, "renderTasks out filteredCount=${filtered.size}")
    }

    /**
     * 功能：处理任务点击并跳转到任务详情占位页。
     * 入参：task 当前点击任务。
     * 出参：无。
     * 异常：无。
     */
    private fun onTaskClicked(task: TaskUiModel) {
        Log.d(TAG, "onTaskClicked in taskId=${task.id} title=${task.title}")
        val bundle = Bundle().apply {
            putString("taskId", task.id)
            putString("taskTitle", task.title)
        }
        findNavController().navigate(R.id.taskDetail, bundle)
        Log.d(TAG, "onTaskClicked out navigated=true")
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

        val titleLayout = content.findViewById<TextInputLayout>(R.id.til_task_title)
        val titleInput = content.findViewById<TextInputEditText>(R.id.et_task_title)
        val descriptionInput = content.findViewById<TextInputEditText>(R.id.et_task_content)
        val noteInput = content.findViewById<TextInputEditText>(R.id.et_task_note)
        val typeButtonGroup = content.findViewById<MaterialButtonToggleGroup>(R.id.group_task_type)
        val categoryChipGroup = content.findViewById<ChipGroup>(R.id.group_task_category)
        val urgentButton = content.findViewById<MaterialButton>(R.id.btn_task_urgent)
        val importantButton = content.findViewById<MaterialButton>(R.id.btn_task_important)
        val closeButton = content.findViewById<View>(R.id.btn_bottom_sheet_close)
        val cancelButton = content.findViewById<View>(R.id.btn_bottom_sheet_cancel)
        val saveButton = content.findViewById<View>(R.id.btn_bottom_sheet_create)

        if (titleLayout == null || titleInput == null || descriptionInput == null || noteInput == null ||
            typeButtonGroup == null || categoryChipGroup == null || urgentButton == null ||
            importantButton == null || closeButton == null || cancelButton == null || saveButton == null
        ) {
            Log.e(TAG, "showAddTaskBottomSheet requiredViewMissing")
            return
        }

        var selectedType = "one-time"
        var selectedCategory = "none"
        var urgent = false
        var important = false

        typeButtonGroup.check(R.id.btn_type_one_time)
        typeButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedType = if (checkedId == R.id.btn_type_long_task) "long" else "one-time"
            Log.d(TAG, "showAddTaskBottomSheet typeChanged selectedType=$selectedType")
        }

        categoryChipGroup.check(R.id.chip_category_none)
        categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                group.check(R.id.chip_category_none)
                return@setOnCheckedStateChangeListener
            }
            selectedCategory = when (checkedIds.first()) {
                R.id.chip_category_work -> "work"
                R.id.chip_category_life -> "life"
                R.id.chip_category_study -> "study"
                R.id.chip_category_health -> "health"
                else -> "none"
            }
            Log.d(TAG, "showAddTaskBottomSheet categoryChanged selectedCategory=$selectedCategory")
        }

        urgentButton.setOnClickListener {
            urgent = !urgent
            updateToggleButtonStyle(urgentButton, urgent, R.color.task_urgent_highlight)
            Log.d(TAG, "showAddTaskBottomSheet urgentChanged urgent=$urgent")
        }

        importantButton.setOnClickListener {
            important = !important
            updateToggleButtonStyle(importantButton, important, R.color.task_important_highlight)
            Log.d(TAG, "showAddTaskBottomSheet importantChanged important=$important")
        }

        closeButton.setOnClickListener {
            Log.d(TAG, "showAddTaskBottomSheet clickClose")
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            Log.d(TAG, "showAddTaskBottomSheet clickCancel")
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            Log.d(
                TAG,
                "showAddTaskBottomSheet clickSave title=$title type=$selectedType category=$selectedCategory urgent=$urgent important=$important"
            )
            if (title.isBlank()) {
                titleLayout.error = getString(R.string.today_bottom_sheet_title_required)
                Log.d(TAG, "showAddTaskBottomSheet saveBlocked reason=blankTitle")
                return@setOnClickListener
            }
            titleLayout.error = null
            val description = descriptionInput.text?.toString()?.trim().orEmpty()
            val note = noteInput.text?.toString()?.trim().orEmpty()
            Log.d(
                TAG,
                "showAddTaskBottomSheet savePayload title=$title description=$description note=$note type=$selectedType category=$selectedCategory urgent=$urgent important=$important"
            )
            // TODO: 接入新建任务表单保存逻辑（Room/Repository）。
            dialog.dismiss()
        }

        dialog.setContentView(content)
        dialog.show()
        Log.d(TAG, "showAddTaskBottomSheet out shown=true")
    }

    /**
     * 功能：根据选中状态刷新“紧急/重要”按钮视觉。
     * 入参：button 目标按钮；selected 是否选中；selectedColorRes 选中时边框与文字颜色资源。
     * 出参：无。
     * 异常：无，颜色解析失败时使用主题主色回退。
     */
    private fun updateToggleButtonStyle(button: MaterialButton, selected: Boolean, selectedColorRes: Int) {
        Log.d(TAG, "updateToggleButtonStyle in selected=$selected selectedColorRes=$selectedColorRes")
        val selectedColor = ContextCompat.getColor(requireContext(), selectedColorRes)
        val defaultStrokeColor = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
        val defaultTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        button.strokeColor = ColorStateList.valueOf(if (selected) selectedColor else defaultStrokeColor)
        button.setTextColor(if (selected) selectedColor else defaultTextColor)
        Log.d(TAG, "updateToggleButtonStyle out applied=true")
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
        val result = listOf(
            TaskUiModel(
                id = "task_001",
                title = "完成周报并发送团队邮件",
                content = "同步项目进度和风险事项",
                done = false,
                isLongTask = true,
                urgent = true,
                important = true
            ),
            TaskUiModel(
                id = "task_002",
                title = "整理下周需求评审材料",
                content = "准备评审议程与需求变更说明",
                done = true,
                isLongTask = false,
                urgent = false,
                important = true
            ),
            TaskUiModel(
                id = "task_003",
                title = "预约体检时间",
                content = "联系医院并确认可预约时段",
                done = false,
                isLongTask = false,
                urgent = true,
                important = false
            ),
            TaskUiModel(
                id = "task_004",
                title = "阅读技术文章并记录摘要",
                content = "输入输出模型相关文章",
                done = true,
                isLongTask = true,
                urgent = false,
                important = false
            )
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
        val important: Boolean
    )

    private enum class StatusFilter {
        ALL, TODO, DONE
    }

    private enum class TypeFilter {
        ALL, ONE_TIME, LONG
    }

    private enum class PriorityFilter {
        ALL, UI, I, U, N
    }

    private class TasksAdapter(
        private val onItemClick: (TaskUiModel) -> Unit
    ) : RecyclerView.Adapter<TasksAdapter.TasksViewHolder>() {

        private val data = mutableListOf<TaskUiModel>()

        fun submitData(newData: List<TaskUiModel>) {
            data.clear()
            data.addAll(newData)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemTodayTaskBinding.inflate(inflater, parent, false)
            return TasksViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
            holder.bind(data[position])
        }

        override fun getItemCount(): Int = data.size

        class TasksViewHolder(
            private val binding: ItemTodayTaskBinding,
            private val onItemClick: (TaskUiModel) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: TaskUiModel) {
                binding.tvTaskTitle.text = item.title
                binding.tvTaskContent.text = item.content.orEmpty()
                binding.tvTaskContent.visibility = if (item.content.isNullOrBlank()) View.GONE else View.VISIBLE
                binding.tvTypeTag.text = if (item.isLongTask) {
                    binding.root.context.getString(R.string.tasks_type_long_task)
                } else {
                    binding.root.context.getString(R.string.tasks_type_one_time)
                }
                binding.tvPriorityTag.text = getPriorityText(item)
                binding.tvTaskTitle.paint.isStrikeThruText = item.done
                binding.root.alpha = if (item.done) 0.72f else 1f
                binding.root.setOnClickListener { onItemClick(item) }
            }

            private fun getPriorityText(item: TaskUiModel): String {
                val context = binding.root.context
                if (item.urgent && item.important) return context.getString(R.string.tasks_filter_ui)
                if (item.important) return context.getString(R.string.tasks_filter_i)
                if (item.urgent) return context.getString(R.string.tasks_filter_u)
                return context.getString(R.string.tasks_filter_n)
            }
        }
    }

    private companion object {
        private const val TAG = "TasksFragment"
    }
}
