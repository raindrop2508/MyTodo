package com.gordon.mypotato.ui.tasks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.ActivityTaskEditBinding
import com.gordon.mypotato.domain.Category
import com.gordon.mypotato.domain.TaskStep
import com.gordon.mypotato.domain.TaskType
import com.gordon.mypotato.ui.common.CategoryChipHelper
import com.gordon.mypotato.ui.common.CategoryEditDialogHelper
import com.gordon.mypotato.viewmodel.EditableStepItem
import com.gordon.mypotato.viewmodel.TaskEditViewModel
import com.gordon.mypotato.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.util.Collections

class TaskEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskEditBinding
    private lateinit var viewModel: TaskEditViewModel
    private lateinit var stepAdapter: com.gordon.mypotato.ui.common.EditableStepAdapter
    private val stepList = mutableListOf<com.gordon.mypotato.ui.common.EditableStep>()
    private val deletedStepIds = mutableListOf<Long>()

    private var taskId: Long = -1
    private var selectedCategoryId = 0L
    private var hasRenderedTask = false
    private var lastCategoryIds: List<Long> = emptyList()

    companion object {
        private const val TAG = "TaskEditActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate in")
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(this))[TaskEditViewModel::class.java]
        readIntentExtras()
        setupToolbar()
        setupStepList()
        setupTypeButtons()
        collectUiState()

        if (taskId != -1L) {
            viewModel.loadTask(taskId)
        } else {
            Log.e(TAG, "Invalid taskId provided")
            finish()
        }
        Log.d(TAG, "onCreate out")
    }

    private fun readIntentExtras() {
        Log.d(TAG, "readIntentExtras in")
        val args = TaskEditActivityArgs.fromBundle(intent.extras ?: Bundle())
        taskId = args.taskId
        Log.d(TAG, "readIntentExtras out taskId=$taskId")
    }

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
                    saveTask()
                    true
                }
                else -> false
            }
        }
        Log.d(TAG, "setupToolbar out")
    }

    private fun saveTask() {
        Log.d(TAG, "saveTask in")

        stepAdapter.syncVisibleStepInputs(binding.rvSteps)

        val title = binding.etTaskTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etTaskContent.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val note = binding.etTaskNote.text?.toString()?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val isLongTask = binding.groupTaskType.checkedButtonId == R.id.btn_type_long_task
        val taskType = if (isLongTask) TaskType.LONG.value else TaskType.ONCE.value
        val isUrgent = binding.switchTaskUrgent.isChecked
        val isImportant = binding.switchTaskImportant.isChecked

        val categoryId = CategoryChipHelper.getSelectedCategoryId(binding.groupTaskCategory)

        val stepEdits = stepList
            .filter { it.title.isNotBlank() }
            .mapIndexed { index, step ->
                EditableStepItem(
                    id = stepList.indexOfFirst { it == step }.let { pos ->
                        if (pos >= 0 && pos < viewModel.uiState.value.steps.size) {
                            viewModel.uiState.value.steps[pos].id
                        } else {
                            0L
                        }
                    },
                    title = step.title
                )
            }

        val updatedTask = viewModel.buildUpdatedTask(title, content, note, taskType, categoryId, isUrgent, isImportant)
        updatedTask?.let {
            viewModel.updateTask(it)
            if (isLongTask) {
                viewModel.saveSteps(stepEdits, deletedStepIds)
            } else {
                viewModel.uiState.value.steps.forEach { step ->
                    viewModel.saveSteps(emptyList(), listOf(step.id))
                }
            }
        }

        finish()
        Log.d(TAG, "saveTask out")
    }

    private fun setupStepList() {
        Log.d(TAG, "setupStepList in")
        stepAdapter =
            com.gordon.mypotato.ui.common.EditableStepAdapter(
                steps = stepList,
                onDeleteClick = { position ->
                    if (position !in stepList.indices) {
                        Log.d(TAG, "setupStepList skipDelete invalidPosition=$position")
                    } else {
                        if (position < viewModel.uiState.value.steps.size) {
                            deletedStepIds.add(viewModel.uiState.value.steps[position].id)
                        }
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
            stepList.add(com.gordon.mypotato.ui.common.EditableStep(""))
            stepAdapter.notifyItemInserted(stepList.size - 1)
            updateStepSectionSummary()
        }
        Log.d(TAG, "setupStepList out")
    }

    private fun setupTypeButtons() {
        Log.d(TAG, "setupTypeButtons in")
        binding.groupTaskType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val isLongTask = checkedId == R.id.btn_type_long_task
            Log.d(TAG, "setupTypeButtons changed isLongTask=$isLongTask")
            renderStepSection(isLongTask)
        }
        Log.d(TAG, "setupTypeButtons out")
    }

    private fun collectUiState() {
        Log.d(TAG, "collectUiState in")
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading) {
                    return@collect
                }

                val categoryIds = state.categories.map { it.id }
                if (categoryIds != lastCategoryIds) {
                    lastCategoryIds = categoryIds
                    bindCategoryChips(state.categories)
                }

                state.task?.let { task ->
                    if (!hasRenderedTask) {
                        selectedCategoryId = task.categoryId
                        renderTask(task, state.category, state.steps)
                        bindCategoryChips(state.categories)
                        hasRenderedTask = true
                    }
                }
            }
        }
        Log.d(TAG, "collectUiState out")
    }

    private fun bindCategoryChips(categories: List<Category>) {
        CategoryChipHelper.populateCategoryChips(
            chipGroup = binding.groupTaskCategory,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { id ->
                selectedCategoryId = id
            },
            onAddCategoryClick = {
                CategoryEditDialogHelper.showCreateDialog(
                    context = this,
                    existingNames = categories.map { it.name }
                ) { name ->
                    viewModel.addCategory(
                        name = name,
                        colorHex = CategoryEditDialogHelper.nextColor(categories)
                    ) { result ->
                        result.onSuccess { newId ->
                            selectedCategoryId = newId
                            val current = viewModel.uiState.value.categories
                            if (current.any { it.id == newId }) {
                                lastCategoryIds = current.map { it.id }
                                bindCategoryChips(current)
                            } else {
                                lastCategoryIds = emptyList()
                            }
                            Log.d(TAG, "categoryCreated id=$newId")
                        }.onFailure { e ->
                            Log.e(TAG, "categoryCreateFailed", e)
                            Toast.makeText(this, R.string.category_create_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCategoryLongClick = { category ->
                CategoryEditDialogHelper.showDeleteConfirmDialog(
                    context = this,
                    category = category
                ) {
                    viewModel.deleteCategory(category.id) { result ->
                        result.onSuccess {
                            if (selectedCategoryId == category.id) {
                                selectedCategoryId = 0L
                            }
                            val current = viewModel.uiState.value.categories
                            if (current.none { it.id == category.id }) {
                                lastCategoryIds = current.map { it.id }
                                bindCategoryChips(current)
                            } else {
                                lastCategoryIds = emptyList()
                            }
                            Log.d(TAG, "categoryDeleted id=${category.id}")
                        }.onFailure { e ->
                            Log.e(TAG, "categoryDeleteFailed", e)
                            Toast.makeText(this, R.string.category_delete_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun renderTask(
        task: com.gordon.mypotato.domain.Task,
        category: com.gordon.mypotato.domain.Category?,
        steps: List<TaskStep>
    ) {
        Log.d(TAG, "renderTask in")

        binding.tvTaskId.text = getString(R.string.task_edit_task_id_format, task.id)
        binding.tvMockHint.visibility = View.GONE

        binding.etTaskTitle.setText(task.title)
        binding.etTaskContent.setText(task.content ?: "")
        binding.etTaskNote.setText(task.note ?: "")
        binding.switchTaskUrgent.isChecked = task.isUrgent
        binding.switchTaskImportant.isChecked = task.isImportant

        if (task.isLongTask()) {
            binding.groupTaskType.check(R.id.btn_type_long_task)
        } else {
            binding.groupTaskType.check(R.id.btn_type_one_time)
        }

        CategoryChipHelper.selectCategory(binding.groupTaskCategory, task.categoryId)

        stepList.clear()
        stepList.addAll(steps.map { com.gordon.mypotato.ui.common.EditableStep(it.title) })
        stepAdapter.notifyDataSetChanged()

        renderStepSection(task.isLongTask())
        updateStepSectionSummary()

        Log.d(TAG, "renderTask out stepCount=${stepList.size}")
    }

    private fun renderStepSection(isLongTask: Boolean) {
        Log.d(TAG, "renderStepSection in isLongTask=$isLongTask")
        binding.cardSteps.visibility = if (isLongTask) View.VISIBLE else View.GONE
        Log.d(TAG, "renderStepSection out visibility=${binding.cardSteps.visibility}")
    }

    private fun updateStepSectionSummary() {
        Log.d(TAG, "updateStepSectionSummary in")
        binding.tvStepSummary.text = getString(R.string.task_edit_step_count_format, stepList.size)
        Log.d(TAG, "updateStepSectionSummary out summary=${binding.tvStepSummary.text}")
    }

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
