package com.gordon.mypotato.ui.common

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gordon.mypotato.databinding.ItemStepEditableBinding

/**
 * 功能：可编辑步骤列表适配器。
 * 入参：steps 步骤列表，onDeleteClick 删除回调，onStartDrag 开始拖拽回调。
 * 出参：无。
 * 异常：无。
 */
class EditableStepAdapter(
    private val steps: MutableList<EditableStep>,
    private val onDeleteClick: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<EditableStepAdapter.StepViewHolder>() {

    inner class StepViewHolder(
        private val binding: ItemStepEditableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: EditableStep, position: Int) {
            binding.etStepTitle.setText(step.title)
            binding.etStepTitle.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    step.title = binding.etStepTitle.text?.toString()?.trim().orEmpty()
                }
            }
            binding.btnDeleteStep.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                onDeleteClick(currentPosition)
            }
            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemStepEditableBinding.inflate(inflater, parent, false)
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(steps[position], position)
    }

    override fun getItemCount(): Int = steps.size
}
