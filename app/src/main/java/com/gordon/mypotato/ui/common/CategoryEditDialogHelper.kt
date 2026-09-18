package com.gordon.mypotato.ui.common

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gordon.mypotato.R
import com.gordon.mypotato.domain.Category

/**
 * 分类新建 / 删除确认对话框辅助类
 */
object CategoryEditDialogHelper {

    val PRESET_COLORS = listOf(
        "#FF6B6B",
        "#4ECDC4",
        "#FFE66D",
        "#95E1D3",
        "#F38181"
    )

    /**
     * 按已有分类轮选下一个预设颜色
     */
    fun nextColor(existingCategories: List<Category>): String {
        val used = existingCategories.map { it.colorHex }.toSet()
        return PRESET_COLORS.firstOrNull { it !in used }
            ?: PRESET_COLORS[existingCategories.size % PRESET_COLORS.size]
    }

    /**
     * 显示新建分类对话框
     * @param existingNames 已有分类名称（用于前端去重提示）
     * @param onConfirm 名称校验通过后回调（已 trim）
     */
    fun showCreateDialog(
        context: Context,
        existingNames: Collection<String>,
        onConfirm: (name: String) -> Unit
    ) {
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = context.getString(R.string.category_create_hint)
            setSingleLine()
        }
        val container = FrameLayout(context).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.category_create_title)
            .setView(container)
            .setPositiveButton(R.string.category_create_confirm, null)
            .setNegativeButton(R.string.category_dialog_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text?.toString()?.trim().orEmpty()
                when {
                    name.isEmpty() -> {
                        Toast.makeText(
                            context,
                            R.string.category_name_empty,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    existingNames.any { it.equals(name, ignoreCase = true) } -> {
                        Toast.makeText(
                            context,
                            R.string.category_name_duplicate,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        dialog.dismiss()
                        onConfirm(name)
                    }
                }
            }
        }
        dialog.show()
    }

    /**
     * 显示删除分类确认对话框
     */
    fun showDeleteConfirmDialog(
        context: Context,
        category: Category,
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.category_delete_title)
            .setMessage(
                context.getString(R.string.category_delete_message, category.name)
            )
            .setPositiveButton(R.string.category_delete_confirm) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.category_dialog_cancel, null)
            .show()
    }
}
