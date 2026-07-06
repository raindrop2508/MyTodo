package com.gordon.mypotato.ui.today

import android.graphics.Color
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentTodayBinding
import com.gordon.mypotato.databinding.ItemTodayTaskBinding
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskType
import com.gordon.mypotato.ui.common.AddTaskBottomSheetHelper
import com.gordon.mypotato.ui.common.EditableStep
import com.gordon.mypotato.ui.tasks.TaskDetailActivity
import com.gordon.mypotato.viewmodel.PriorityFilter
import com.gordon.mypotato.viewmodel.TodayViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayFragment : Fragment(R.layout.fragment_today) {
    private var _binding: FragmentTodayBinding? = null
    private val binding: FragmentTodayBinding
        get() = _binding!!

    private lateinit var viewModel: TodayViewModel
    private val adapter = TodayTaskAdapter(::onTaskClicked, ::onTaskCheckChanged)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TodayViewModel::class.java]  // 绑定ViewModel
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupHeaderDate()
        setupTaskList()
        setupPriorityFilter()
        setupFab()
        collectTasks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeaderDate() {
        val dateText = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
        Log.d(TAG, "setupHeaderDate dateText=$dateText")
        binding.tvTodayDate.text = dateText
    }

    private fun setupTaskList() {
        binding.rvTodayTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodayTasks.adapter = adapter
    }

    private fun setupPriorityFilter() {
        binding.chipGroupPriority.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            val filter =
                when (checkedId) {
                    R.id.chip_ui -> PriorityFilter.UI
                    R.id.chip_i -> PriorityFilter.I
                    R.id.chip_u -> PriorityFilter.U
                    R.id.chip_n -> PriorityFilter.N
                    else -> PriorityFilter.ALL
                }
            Log.d(TAG, "setupPriorityFilter checkedId=$checkedId filter=$filter")
            viewModel.setFilter(filter)   // 更新根据类别过滤数据
        }
    }

    /**
    * 添加任务 弹窗
    * */
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            AddTaskBottomSheetHelper(
                fragment = this,
                callback = object : AddTaskBottomSheetHelper.Callback {
                    override fun onTaskCreate(
                        title: String,
                        description: String,
                        note: String,
                        type: String,
                        categoryId: Long,
                        urgent: Boolean,
                        important: Boolean,
                        steps: List<EditableStep>
                    ) {
                        Log.d(
                            TAG,
                            "onTaskCreate title=$title type=$type categoryId=$categoryId urgent=$urgent important=$important stepsCount=${steps.size}"
                        )
                        lifecycleScope.launch {
                            val taskType = if (type == "long") TaskType.LONG.value else TaskType.ONCE.value
                            val task = Task(
                                id = 0,
                                title = title,
                                content = if (description.isBlank()) null else description,
                                note = if (note.isBlank()) null else note,
                                taskType = taskType,
                                status = com.gordon.mypotato.domain.TaskStatus.TODO.value,
                                isUrgent = urgent,
                                isImportant = important,
                                categoryId = categoryId,
                                createdAt = System.currentTimeMillis() / 1000,
                                plannedStartAt = null,
                                finishedAt = null,
                                totalDurationSec = 0
                            )
                            val stepTitles = steps.map { it.title }.filter { it.isNotBlank() }
                            viewModel.addTask(task, stepTitles)
                        }
                    }
                }
            ).show()
        }
    }

    /**
    * 建立“观察者”连接 ,主动“监听” ViewModel 中的数据变化。
    * */
    private fun collectTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                Log.d(TAG, "collectTasks tasksCount=${uiState.tasks.size}")
                // 更新任务 Adapter 展示的数据
                adapter.submitData(uiState.tasks, uiState.categories)
                binding.layoutEmptyState.visibility = if (uiState.tasks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onTaskClicked(task: Task) {
        Log.d(TAG, "onTaskClicked id=${task.id} title=${task.title}")
        val intent =
            android.content
                .Intent(
                    requireContext(),
                    TaskDetailActivity::class.java,
                ).apply {
                    putExtra("taskId", task.id)
                    putExtra("taskTitle", task.title)
                    putExtra("isLongTask", task.isLongTask())
                    putExtra("category", viewModel.getCategoryName(task.categoryId))
                    putExtra("urgent", task.isUrgent)
                    putExtra("important", task.isImportant)
                }
        startActivity(intent)
    }

    private fun onTaskCheckChanged(
        task: Task,
        isChecked: Boolean
    ) {
        Log.d(TAG, "onTaskCheckChanged id=${task.id} isChecked=$isChecked")
        lifecycleScope.launch {
            viewModel.toggleTaskStatus(task.id)
        }
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        Log.d(TAG, "resolveThemeColor attrRes=$attrRes")
        val outValue = TypedValue()
        val resolved = requireContext().theme.resolveAttribute(attrRes, outValue, true)
        if (resolved) return outValue.data
        return ContextCompat.getColor(requireContext(), R.color.purple_500)
    }

    private class TodayTaskAdapter(
        private val onItemClick: (Task) -> Unit,
        private val onCheckChanged: (Task, Boolean) -> Unit,
    ) : RecyclerView.Adapter<TodayTaskAdapter.TodayTaskViewHolder>() {
        private val data = mutableListOf<Task>()
        private var categories: Map<Long, com.gordon.mypotato.domain.Category> = emptyMap()

        fun submitData(newData: List<Task>, categoryMap: Map<Long, com.gordon.mypotato.domain.Category>) {
            data.clear()
            data.addAll(newData)
            categories = categoryMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): TodayTaskViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemTodayTaskBinding.inflate(inflater, parent, false)
            return TodayTaskViewHolder(binding, onItemClick, onCheckChanged)
        }

        override fun onBindViewHolder(
            holder: TodayTaskViewHolder,
            position: Int,
        ) {
            holder.bind(data[position], categories)
        }

        override fun getItemCount(): Int = data.size

        class TodayTaskViewHolder(
            private val binding: ItemTodayTaskBinding,
            private val onItemClick: (Task) -> Unit,
            private val onCheckChanged: (Task, Boolean) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Task, categories: Map<Long, com.gordon.mypotato.domain.Category>) {
                binding.tvTaskTitle.text = item.title
                binding.tvTaskTitle.alpha = if (item.isCompleted()) 0.5f else 1.0f

                val category = categories[item.categoryId]
                binding.tvCategoryTag.text = category?.name ?: "未分类"
                bindCategoryColor(category, binding.tvCategoryTag)

                binding.tvTypeTag.text =
                    if (item.isLongTask()) {
                        binding.root.context.getString(R.string.today_task_type_long)
                    } else {
                        binding.root.context.getString(R.string.today_task_type_once)
                    }

                if (item.totalDurationSec > 0) {
                    binding.tvTimeTag.visibility = View.VISIBLE
                    binding.tvTimeTag.text = "${item.totalDurationSec / 60}分钟"
                } else {
                    binding.tvTimeTag.visibility = View.GONE
                }

                binding.cbTaskDone.setOnCheckedChangeListener(null)
                binding.cbTaskDone.isChecked = item.isCompleted()
                binding.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
                    onCheckChanged(item, isChecked)
                }

                binding.root.setOnClickListener { onItemClick(item) }
            }

            private fun bindCategoryColor(
                category: Category?,
                textView: TextView,
            ) {
                if (category != null) {
                    try {
                        val bgColor = Color.parseColor(category.colorHex)
                        textView.backgroundTintList = ColorStateList.valueOf(bgColor)
                        val luminance = Color.luminance(bgColor)
                        val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                        textView.setTextColor(textColor)
                    } catch (e: IllegalArgumentException) {
                        applyDefaultCategoryColor(textView)
                    }
                } else {
                    applyDefaultCategoryColor(textView)
                }
            }

            private fun applyDefaultCategoryColor(textView: TextView) {
                val context = textView.context
                textView.backgroundTintList = ContextCompat.getColorStateList(context, R.color.tag_default_bg)
                textView.setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
            }
        }
    }

    private companion object {
        private const val TAG = "TodayFragment"
    }
}