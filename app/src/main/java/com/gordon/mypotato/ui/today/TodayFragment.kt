package com.gordon.mypotato.ui.today

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.BottomSheetAddTaskPlaceholderBinding
import com.gordon.mypotato.databinding.FragmentTodayBinding
import com.gordon.mypotato.databinding.ItemTodayTaskBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayFragment : Fragment(R.layout.fragment_today) {

    private var _binding: FragmentTodayBinding? = null
    private val binding: FragmentTodayBinding
        get() = _binding!!

    private val allTasks: MutableList<TodayTask> = buildMockTasks().toMutableList()
    private val adapter = TodayTaskAdapter(::onTaskClicked, ::onTaskCheckChanged)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeaderDate()
        setupTaskList()
        setupPriorityFilter()
        setupFab()
        renderTasks(PriorityFilter.ALL)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 功能：设置 Today 页日期文案（中文）。
     * 入参：无。
     * 出参：无。
     * 异常：无，若格式化失败将使用默认当前时间字符串。
     */
    private fun setupHeaderDate() {
        val dateText = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
        Log.d(TAG, "setupHeaderDate dateText=$dateText")
        binding.tvTodayDate.text = dateText
    }

    /**
     * 功能：初始化任务列表与点击事件回调。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupTaskList() {
        binding.rvTodayTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayTasks.adapter = adapter
    }

    /**
     * 功能：绑定优先级筛选 Chip 逻辑并刷新列表。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupPriorityFilter() {
        binding.chipGroupPriority.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            val filter = when (checkedId) {
                R.id.chip_ui -> PriorityFilter.UI
                R.id.chip_i -> PriorityFilter.I
                R.id.chip_u -> PriorityFilter.U
                R.id.chip_n -> PriorityFilter.N
                else -> PriorityFilter.ALL
            }
            Log.d(TAG, "setupPriorityFilter checkedId=$checkedId filter=$filter")
            renderTasks(filter)
        }
    }

    /**
     * 功能：绑定 FAB 并展示“新任务”BottomSheet 占位弹窗。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskBottomSheet()
        }
    }

    /**
     * 功能：按筛选条件过滤写死任务并刷新列表与空状态，已完成的任务自动排在未完成的后边。
     * 入参：filter 当前优先级筛选项。
     * 出参：无。
     * 异常：无。
     */
    private fun renderTasks(filter: PriorityFilter) {
        val filtered = allTasks.filter { task -> filter == PriorityFilter.ALL || task.priority == filter }
            .sortedBy { it.done }
        Log.d(TAG, "renderTasks filter=$filter count=${filtered.size}")
        adapter.submitData(filtered)
        binding.layoutEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * 功能：处理任务卡片点击并跳转到详情占位页。
     * 入参：task 当前点击任务。
     * 出参：无。
     * 异常：无。
     */
    private fun onTaskClicked(task: TodayTask) {
        Log.d(TAG, "onTaskClicked id=${task.id} title=${task.title}")
        val bundle = Bundle().apply {
            putString("taskId", task.id)
            putString("taskTitle", task.title)
        }
        findNavController().navigate(R.id.action_today_to_taskDetail, bundle)
    }

    /**
     * 功能：处理任务勾选状态变更，更新数据并重新排序列表。
     * 入参：task 当前任务，isChecked 勾选状态。
     * 出参：无。
     * 异常：无。
     */
    private fun onTaskCheckChanged(task: TodayTask, isChecked: Boolean) {
        Log.d(TAG, "onTaskCheckChanged id=${task.id} isChecked=$isChecked")
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index] = allTasks[index].copy(done = isChecked)
            
            // 获取当前筛选条件并刷新列表
            val checkedIds = binding.chipGroupPriority.checkedChipIds
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            val filter = when (checkedId) {
                R.id.chip_ui -> PriorityFilter.UI
                R.id.chip_i -> PriorityFilter.I
                R.id.chip_u -> PriorityFilter.U
                R.id.chip_n -> PriorityFilter.N
                else -> PriorityFilter.ALL
            }
            renderTasks(filter)
        }
    }

    /**
     * 功能：展示“新增任务”BottomSheet 最小实现内容，支持字段输入、选中态切换与标题必填校验。
     * 入参：无。
     * 出参：无。
     * 异常：无，控件缺失时将直接返回，避免崩溃。
     */
    private fun showAddTaskBottomSheet() {
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
            selectedCategory = when (checkedIds.first()) {
                R.id.chip_category_work -> "work"
                R.id.chip_category_life -> "life"
                R.id.chip_category_study -> "study"
                R.id.chip_category_health -> "health"
                else -> "none"
            }
            Log.d(TAG, "showAddTaskBottomSheet categoryChanged selectedCategory=$selectedCategory")
        }

        sheetBinding.btnTaskUrgent.setOnClickListener {
            urgent = !urgent
            updateToggleButtonStyle(sheetBinding.btnTaskUrgent, urgent, R.color.state_error_red)
            Log.d(TAG, "showAddTaskBottomSheet urgentChanged urgent=$urgent")
        }

        sheetBinding.btnTaskImportant.setOnClickListener {
            important = !important
            updateToggleButtonStyle(sheetBinding.btnTaskImportant, important, R.color.state_chart_orange)
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
            val title = sheetBinding.etTaskTitle.text?.toString()?.trim().orEmpty()
            Log.d(TAG, "showAddTaskBottomSheet clickSave title=$title type=$selectedType category=$selectedCategory urgent=$urgent important=$important")
            if (title.isBlank()) {
                sheetBinding.tilTaskTitle.error = getString(R.string.today_bottom_sheet_title_required)
                Log.d(TAG, "showAddTaskBottomSheet saveBlocked reason=blankTitle")
                return@setOnClickListener
            }
            sheetBinding.tilTaskTitle.error = null
            val description = sheetBinding.etTaskContent.text?.toString()?.trim().orEmpty()
            val note = sheetBinding.etTaskNote.text?.toString()?.trim().orEmpty()
            Log.d(TAG, "showAddTaskBottomSheet savePayload title=$title description=$description note=$note type=$selectedType category=$selectedCategory urgent=$urgent important=$important")
            // TODO: 接入新建任务表单保存逻辑（Room/Repository）。
            dialog.dismiss()
        }

        dialog.setContentView(content)
        dialog.show()
    }

    /**
     * 功能：根据选中状态刷新“紧急/重要”按钮视觉。
     * 入参：button 目标按钮；selected 是否选中；selectedColorRes 选中时边框与文字颜色资源。
     * 出参：无。
     * 异常：无，颜色解析失败时使用主题主色回退。
     */
    private fun updateToggleButtonStyle(button: MaterialButton, selected: Boolean, selectedColorRes: Int) {
        Log.d(TAG, "updateToggleButtonStyle selected=$selected selectedColorRes=$selectedColorRes")
        val selectedColor = ContextCompat.getColor(requireContext(), selectedColorRes)
        val defaultStrokeColor = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
        val defaultTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val activeTextColor = selectedColor
        button.strokeColor = android.content.res.ColorStateList.valueOf(if (selected) selectedColor else defaultStrokeColor)
        button.setTextColor(if (selected) activeTextColor else defaultTextColor)
    }

    /**
     * 功能：解析主题属性对应的颜色值。
     * 入参：attrRes 主题属性资源 id。
     * 出参：返回解析后的颜色值。
     * 异常：无，解析失败返回主题主色。
     */
    private fun resolveThemeColor(attrRes: Int): Int {
        Log.d(TAG, "resolveThemeColor attrRes=$attrRes")
        val outValue = TypedValue()
        val resolved = requireContext().theme.resolveAttribute(attrRes, outValue, true)
        if (resolved) return outValue.data
        return ContextCompat.getColor(requireContext(), R.color.purple_500)
    }

    /**
     * 功能：构建 Today 页写死任务数据。
     * 入参：无。
     * 出参：返回用于展示的任务列表。
     * 异常：无。
     */
    private fun buildMockTasks(): List<TodayTask> {
        // TODO: 接入 Room/Repository 真实任务数据并替换当前写死数据源。
        return listOf(
            TodayTask("task_001", "完成项目原型设计", "同步项目进度和风险事项", true, false, true, PriorityFilter.UI, category = "工作", minutes = 60, steps = 5),
            TodayTask("task_002", "购买生活用品", "准备评审议程与需求变更说明", false, true, true, PriorityFilter.I, category = "购物", minutes = 0, steps = 0),
            TodayTask("task_003", "回复客户邮件", "联系医院并确认可预约时段", false, false, true, PriorityFilter.U, category = "工作", minutes = 0, steps = 0, done = true),
            TodayTask("task_004", "学习React新特性", "输入输出模型相关文章", true, false, false, PriorityFilter.N, category = "学习", minutes = 120, steps = 3),
            TodayTask("task_005", "晚间跑步", "输入输出模型相关文章", true, false, false, PriorityFilter.N, category = "健康", minutes = 30, steps = 0)
        )
    }

    private data class TodayTask(
        val id: String,
        val title: String,
        val content: String?,
        val isLongTask: Boolean,
        val urgent: Boolean,
        val important: Boolean,
        val priority: PriorityFilter,
        val category: String = "工作",
        val minutes: Int = 0,
        val steps: Int = 0,
        val done: Boolean = false
    )

    private enum class PriorityFilter {
        ALL, UI, I, U, N
    }

    private class TodayTaskAdapter(
        private val onItemClick: (TodayTask) -> Unit,
        private val onCheckChanged: (TodayTask, Boolean) -> Unit
    ) : RecyclerView.Adapter<TodayTaskAdapter.TodayTaskViewHolder>() {

        private val data = mutableListOf<TodayTask>()

        fun submitData(newData: List<TodayTask>) {
            data.clear()
            data.addAll(newData)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodayTaskViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemTodayTaskBinding.inflate(inflater, parent, false)
            return TodayTaskViewHolder(binding, onItemClick, onCheckChanged)
        }

        override fun onBindViewHolder(holder: TodayTaskViewHolder, position: Int) {
            holder.bind(data[position])
        }

        override fun getItemCount(): Int = data.size

        class TodayTaskViewHolder(
            private val binding: ItemTodayTaskBinding,
            private val onItemClick: (TodayTask) -> Unit,
            private val onCheckChanged: (TodayTask, Boolean) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: TodayTask) {
                binding.tvTaskTitle.text = item.title
                if (item.done) {
                    binding.tvTaskTitle.alpha = 0.5f
                } else {
                    binding.tvTaskTitle.alpha = 1.0f
                }

                binding.tvCategoryTag.text = item.category
                bindCategoryColor(item.category, binding.tvCategoryTag)

                binding.tvTypeTag.text = if (item.isLongTask) {
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

            private fun bindCategoryColor(category: String, textView: TextView) {
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
        private const val TAG = "TodayFragment"
    }
}
