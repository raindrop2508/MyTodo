package com.gordon.mypotato.ui.tasks

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentTasksBinding
import com.gordon.mypotato.databinding.ItemTodayTaskBinding
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.Task
import com.gordon.mypotato.domain.TaskStatus
import com.gordon.mypotato.domain.TaskType
import com.gordon.mypotato.ui.common.AddTaskBottomSheetHelper
import com.gordon.mypotato.ui.common.EditableStep
import com.gordon.mypotato.viewmodel.FilterDimension
import com.gordon.mypotato.viewmodel.TasksViewModel
import kotlinx.coroutines.launch

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private var _binding: FragmentTasksBinding? = null
    private val binding: FragmentTasksBinding
        get() = _binding!!

    private lateinit var viewModel: TasksViewModel
    private val adapter = TasksAdapter(::onTaskClicked, ::onTaskCheckChanged)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TasksViewModel::class.java]
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
        collectTasks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvTasksTitle.text = getString(R.string.tasks_title)
    }

    private fun setupTaskList() {
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter
    }

    private fun setupSearch() {
        binding.etTasksSearch.doAfterTextChanged { editable ->
            val keyword = editable?.toString()?.trim().orEmpty()
            viewModel.setKeyword(keyword)
        }
    }

    private fun setupDimensionTabs() {
        binding.rgDimension.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_dim_category -> viewModel.setDimension(FilterDimension.CATEGORY)
                R.id.rb_dim_quadrant -> viewModel.setDimension(FilterDimension.QUADRANT)
                R.id.rb_dim_status -> viewModel.setDimension(FilterDimension.STATUS)
            }
        }
    }

    private fun setupChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_cat_all
            val categoryId = when (checkedId) {
                R.id.chip_cat_study -> 1L
                R.id.chip_cat_work -> 2L
                R.id.chip_cat_life -> 3L
                R.id.chip_cat_health -> 4L
                R.id.chip_cat_shopping -> 5L
                else -> null // Includes chip_cat_all
            }
            viewModel.setCategoryFilter(categoryId)
        }

        binding.chipGroupQuadrant.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_quadrant_all
            val (urgent, important) = when (checkedId) {
                R.id.chip_quadrant_ui -> true to true
                R.id.chip_quadrant_i -> false to true
                R.id.chip_quadrant_u -> true to false
                R.id.chip_quadrant_other -> false to false
                else -> null to null
            }
            viewModel.setQuadrantFilter(urgent, important)
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_status_all
            val status = when (checkedId) {
                R.id.chip_status_done -> TaskStatus.COMPLETED
                R.id.chip_status_doing -> TaskStatus.IN_PROGRESS
                R.id.chip_status_todo -> TaskStatus.TODO
                else -> null
            }
            viewModel.setStatusFilter(status)
        }
    }

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
                        lifecycleScope.launch {
                            val taskType = if (type == "long") TaskType.LONG.value else TaskType.ONCE.value
                            val task = Task(
                                id = 0,
                                title = title,
                                content = if (description.isBlank()) null else description,
                                note = if (note.isBlank()) null else note,
                                taskType = taskType,
                                status = TaskStatus.TODO.value,
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

    private fun collectTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                binding.chipGroupCategory.visibility = if (uiState.dimension == FilterDimension.CATEGORY) View.VISIBLE else View.GONE
                binding.chipGroupQuadrant.visibility = if (uiState.dimension == FilterDimension.QUADRANT) View.VISIBLE else View.GONE
                binding.chipGroupStatus.visibility = if (uiState.dimension == FilterDimension.STATUS) View.VISIBLE else View.GONE

                adapter.submitData(uiState.tasks, uiState.categories)
                binding.layoutTasksEmptyState.visibility = if (uiState.tasks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onTaskClicked(task: Task) {
        val intent = android.content.Intent(requireContext(), TaskDetailActivity::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("isLongTask", task.isLongTask())
            putExtra("category", viewModel.getCategoryName(task.categoryId))
            putExtra("urgent", task.isUrgent)
            putExtra("important", task.isImportant)
        }
        startActivity(intent)
    }

    private fun onTaskCheckChanged(task: Task, isChecked: Boolean) {
        lifecycleScope.launch {
            viewModel.toggleTaskStatus(task.id)
        }
    }

    private class TasksAdapter(
        private val onItemClick: (Task) -> Unit,
        private val onCheckChanged: (Task, Boolean) -> Unit,
    ) : RecyclerView.Adapter<TasksAdapter.TasksViewHolder>() {
        private val data = mutableListOf<Task>()
        private var categories: Map<Long, Category> = emptyMap()

        fun submitData(newData: List<Task>, categoryMap: Map<Long, Category>) {
            data.clear()
            data.addAll(newData)
            categories = categoryMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemTodayTaskBinding.inflate(inflater, parent, false)
            return TasksViewHolder(binding, onItemClick, onCheckChanged)
        }

        override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
            holder.bind(data[position], categories)
        }

        override fun getItemCount(): Int = data.size

        class TasksViewHolder(
            private val binding: ItemTodayTaskBinding,
            private val onItemClick: (Task) -> Unit,
            private val onCheckChanged: (Task, Boolean) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: Task, categories: Map<Long, Category>) {
                binding.tvTaskTitle.text = item.title
                binding.tvTaskTitle.alpha = if (item.isCompleted()) 0.5f else 1.0f

                val category = categories[item.categoryId]
                binding.tvCategoryTag.text = category?.name ?: "未分类"
                bindCategoryColor(category, binding.tvCategoryTag)

                binding.tvTypeTag.text = if (item.isLongTask()) {
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

            private fun bindCategoryColor(category: Category?, textView: android.widget.TextView) {
                if (category != null) {
                    try {
                        val bgColor = android.graphics.Color.parseColor(category.colorHex)
                        textView.backgroundTintList = ColorStateList.valueOf(bgColor)
                        val luminance = android.graphics.Color.luminance(bgColor)
                        val textColor = if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                        textView.setTextColor(textColor)
                    } catch (e: Exception) {
                        applyDefaultCategoryColor(textView)
                    }
                } else {
                    applyDefaultCategoryColor(textView)
                }
            }

            private fun applyDefaultCategoryColor(textView: android.widget.TextView) {
                val context = textView.context
                textView.backgroundTintList = ContextCompat.getColorStateList(context, R.color.tag_default_bg)
                textView.setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
            }
        }
    }

    private companion object {
        private const val TAG = "TasksFragment"
    }
}
