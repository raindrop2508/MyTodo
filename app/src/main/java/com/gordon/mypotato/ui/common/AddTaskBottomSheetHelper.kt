package com.gordon.mypotato.ui.common

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gordon.mypotato.R
import com.gordon.mypotato.data.repository.CategoryRepository
import com.gordon.mypotato.databinding.BottomSheetAddTaskPlaceholderBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Collections

/**
 * 功能：添加任务 BottomSheet 辅助类，封装完整的添加任务逻辑。
 * 入参：无。
 * 出参：无。
 * 异常：无。
 */
class AddTaskBottomSheetHelper(
    private val fragment: Fragment,
    private val callback: Callback,
    private val categoryRepository: CategoryRepository
) {

    /**
     * 功能：回调接口，用于通知外部 BottomSheet 的操作结果。
     */
    interface Callback {
        /**
         * 功能：当用户点击创建任务时调用。
         * 入参：title 标题，description 描述，note 备注，type 类型，categoryId 分类ID，urgent 是否紧急，important 是否重要，steps 子任务列表。
         * 出参：无。
         * 异常：无。
         */
        fun onTaskCreate(
            title: String,
            description: String,
            note: String,
            type: String,
            categoryId: Long,
            urgent: Boolean,
            important: Boolean,
            steps: List<EditableStep>
        )

        /**
         * 功能：当 BottomSheet 关闭时调用。
         * 入参：无。
         * 出参：无。
         * 异常：无。
         */
        fun onDismiss() {}
    }

    private var dialog: BottomSheetDialog? = null
    private var stepAdapter: EditableStepAdapter? = null
    private val stepList = mutableListOf<EditableStep>()
    private var selectedType = "one-time"
    private var selectedCategoryId = 0L
    private var urgent = false
    private var important = false

    companion object {
        private const val TAG = "AddTaskBottomSheet"
    }

    /**
     * 功能：显示添加任务 BottomSheet。
     * 入参：无。
     * 出参：无。
     * 异常：无。
     */
    fun show() {
        Log.d(TAG, "show in")
        stepList.clear()
        selectedType = "one-time"
        selectedCategoryId = 0L
        urgent = false
        important = false

        dialog = BottomSheetDialog(fragment.requireContext())
        val content = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.bottom_sheet_add_task_placeholder, null)
        val sheetBinding = BottomSheetAddTaskPlaceholderBinding.bind(content)

        setupStepsRecyclerView(sheetBinding)
        setupTypeButtons(sheetBinding)
        setupCategoryChips(sheetBinding)
        setupSwitches(sheetBinding)
        setupButtons(sheetBinding)

        dialog?.setContentView(content)
        dialog?.setOnDismissListener { callback.onDismiss() }
        dialog?.show()
        Log.d(TAG, "show out")
    }

    /**
     * 功能：设置子任务列表。
     * 入参：sheetBinding BottomSheet 绑定对象。
     * 出参：无。
     * 异常：无。
     */
    private fun setupStepsRecyclerView(sheetBinding: BottomSheetAddTaskPlaceholderBinding) {
        stepAdapter = EditableStepAdapter(
            steps = stepList,
            onDeleteClick = { position ->
                stepList.removeAt(position)
                stepAdapter?.notifyItemRemoved(position)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        sheetBinding.rvSteps.layoutManager = LinearLayoutManager(fragment.requireContext())
        sheetBinding.rvSteps.adapter = stepAdapter

        itemTouchHelper.attachToRecyclerView(sheetBinding.rvSteps)

        sheetBinding.tvAddStep.setOnClickListener {
            stepList.add(EditableStep(""))
            stepAdapter?.notifyItemInserted(stepList.size - 1)
        }
    }

    /**
     * 功能：ItemTouchHelper 用于处理拖拽排序。
     */
    private val itemTouchHelper = ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false
                }
                if (fromPos < toPos) {
                    for (i in fromPos until toPos) {
                        Collections.swap(stepList, i, i + 1)
                    }
                } else {
                    for (i in fromPos downTo toPos + 1) {
                        Collections.swap(stepList, i, i - 1)
                    }
                }
                recyclerView.adapter?.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 不处理滑动删除
            }
        }
    )

    /**
     * 功能：设置任务类型按钮。
     * 入参：sheetBinding BottomSheet 绑定对象。
     * 出参：无。
     * 异常：无。
     */
    private fun setupTypeButtons(sheetBinding: BottomSheetAddTaskPlaceholderBinding) {
        sheetBinding.groupTaskType.check(R.id.btn_type_one_time)
        sheetBinding.groupTaskType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedType = if (checkedId == R.id.btn_type_long_task) "long" else "one-time"
            Log.d(TAG, "typeChanged selectedType=$selectedType")
            sheetBinding.cardSteps.visibility =
                if (selectedType == "long") View.VISIBLE else View.GONE
        }
    }

    /**
     * 功能：设置分类芯片。
     * 入参：sheetBinding BottomSheet 绑定对象。
     * 出参：无。
     * 异常：无。
     */
    private fun setupCategoryChips(sheetBinding: BottomSheetAddTaskPlaceholderBinding) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val categories = categoryRepository.getCategories().first()
            CategoryChipHelper.populateCategoryChips(
                chipGroup = sheetBinding.groupTaskCategory,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { id ->
                    selectedCategoryId = id
                    Log.d(TAG, "categoryChanged selectedCategoryId=$selectedCategoryId")
                }
            )
        }
    }

    /**
     * 功能：设置紧急和重要开关。
     * 入参：sheetBinding BottomSheet 绑定对象。
     * 出参：无。
     * 异常：无。
     */
    private fun setupSwitches(sheetBinding: BottomSheetAddTaskPlaceholderBinding) {
        sheetBinding.switchTaskUrgent.setOnCheckedChangeListener { _, isChecked ->
            urgent = isChecked
            Log.d(TAG, "urgentChanged urgent=$urgent")
        }

        sheetBinding.switchTaskImportant.setOnCheckedChangeListener { _, isChecked ->
            important = isChecked
            Log.d(TAG, "importantChanged important=$important")
        }
    }

    /**
     * 功能：设置关闭、取消和创建按钮。
     * 入参：sheetBinding BottomSheet 绑定对象。
     * 出参：无。
     * 异常：无。
     */
    private fun setupButtons(sheetBinding: BottomSheetAddTaskPlaceholderBinding) {
        sheetBinding.btnBottomSheetClose.setOnClickListener {
            Log.d(TAG, "clickClose")
            dialog?.dismiss()
        }

        sheetBinding.btnBottomSheetCancel.setOnClickListener {
            Log.d(TAG, "clickCancel")
            dialog?.dismiss()
        }

        sheetBinding.btnBottomSheetCreate.setOnClickListener {
            sheetBinding.root.findFocus()?.clearFocus()
            stepAdapter?.syncVisibleStepInputs(sheetBinding.rvSteps)
            val title = sheetBinding.etTaskTitle.text?.toString()?.trim().orEmpty()
            Log.d(
                TAG,
                "clickSave title=$title type=$selectedType categoryId=$selectedCategoryId urgent=$urgent important=$important"
            )
            if (title.isBlank()) {
                sheetBinding.tilTaskTitle.error =
                    fragment.getString(R.string.today_bottom_sheet_title_required)
                Log.d(TAG, "saveBlocked reason=blankTitle")
                return@setOnClickListener
            }
            sheetBinding.tilTaskTitle.error = null
            val description = sheetBinding.etTaskContent.text?.toString()?.trim().orEmpty()
            val note = sheetBinding.etTaskNote.text?.toString()?.trim().orEmpty()
            Log.d(
                TAG,
                "savePayload title=$title description=$description note=$note type=$selectedType categoryId=$selectedCategoryId urgent=$urgent important=$important stepsCount=${stepList.size}"
            )
            callback.onTaskCreate(
                title = title,
                description = description,
                note = note,
                type = selectedType,
                categoryId = selectedCategoryId,
                urgent = urgent,
                important = important,
                steps = stepList.toList()
            )
            dialog?.dismiss()
        }
    }
}
