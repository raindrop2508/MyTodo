package com.gordon.mypotato.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gordon.mypotato.R
import com.gordon.mypotato.domain.Category

/**
 * 分类 Chip 辅助类
 * 用于在 ChipGroup 中动态生成和管理分类标签（Chip）
 * */
object CategoryChipHelper {

    /** 「新建分类」Chip 的 tag，不可选中 */
    private const val ADD_CHIP_TAG = -2L

    private fun dpToPx(context: Context, dp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        )
    }

    /**
     * 填充分类 Chip（用于任务编辑/添加场景）
     * @param chipGroup 容器控件
     * @param categories 分类列表数据
     * @param selectedCategoryId 默认选中的分类 ID，默认为 0（无分类）
     * @param onCategorySelected 选中状态变化时的回调
     * @param onAddCategoryClick 点击「+」新建分类时的回调；为 null 时不显示「+」
     * @param onCategoryLongClick 长按分类 Chip 时的回调；为 null 时不启用长按
     */
    fun populateCategoryChips(
        chipGroup: ChipGroup,
        categories: List<Category>,
        selectedCategoryId: Long = 0L,
        onCategorySelected: ((Long) -> Unit)? = null,
        onAddCategoryClick: (() -> Unit)? = null,
        onCategoryLongClick: ((Category) -> Unit)? = null
    ) {
        chipGroup.removeAllViews()

        val noneChip = createNoneChip(chipGroup.context)
        chipGroup.addView(noneChip)

        categories.forEach { category ->
            val chip = createCategoryChip(chipGroup.context, category)
            if (onCategoryLongClick != null) {
                chip.setOnLongClickListener {
                    onCategoryLongClick(category)
                    true
                }
            }
            chipGroup.addView(chip)
        }

        if (onAddCategoryClick != null) {
            chipGroup.addView(createAddChip(chipGroup.context, onAddCategoryClick))
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                chipGroup.check(noneChip.id)
                return@setOnCheckedStateChangeListener
            }

            val checkedChip = chipGroup.findViewById<Chip>(checkedIds.first())
            val categoryId = checkedChip.tag as? Long ?: return@setOnCheckedStateChangeListener
            if (categoryId == ADD_CHIP_TAG) {
                chipGroup.check(noneChip.id)
                return@setOnCheckedStateChangeListener
            }
            onCategorySelected?.invoke(categoryId)
        }

        selectCategory(chipGroup, selectedCategoryId)
    }

    /**
     * 填充分类过滤 Chip（用于任务列表筛选场景）
     * 逻辑与 populateCategoryChips 类似，但包含“全部”选项
     * @param selectedCategoryId 为 null 时表示选中“全部”
     */
    fun populateCategoryFilterChips(
        chipGroup: ChipGroup,
        categories: List<Category>,
        selectedCategoryId: Long? = null,
        onCategoryFilterChanged: ((Long?) -> Unit)? = null
    ) {
        chipGroup.removeAllViews()

        val allChip = createAllChip(chipGroup.context)
        chipGroup.addView(allChip)

        categories.forEach { category ->
            val chip = createCategoryChip(chipGroup.context, category)
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                chipGroup.check(allChip.id)
                return@setOnCheckedStateChangeListener
            }
            val checkedChip = chipGroup.findViewById<Chip>(checkedIds.first())
            val categoryId = checkedChip.tag as? Long
            onCategoryFilterChanged?.invoke(if (categoryId == -1L) null else categoryId)
        }

        val idToSelect = selectedCategoryId ?: -1L
        selectCategory(chipGroup, idToSelect)
    }

    /**
     * 获取当前 ChipGroup 中选中项对应的分类 ID
     * @return 选中的分类 ID，无选中或选中"全部"时返回 0L
     */
    fun getSelectedCategoryId(chipGroup: ChipGroup): Long {
        val checkedId = chipGroup.checkedChipId
        if (checkedId == View.NO_ID) return 0L
        val chip = chipGroup.findViewById<Chip>(checkedId)
        val tag = chip.tag as? Long ?: return 0L
        return if (tag == ADD_CHIP_TAG || tag == -1L) 0L else tag
    }

    /**
     * 根据分类 ID 在 ChipGroup 中查找并选中对应的 Chip
     */
    fun selectCategory(chipGroup: ChipGroup, categoryId: Long) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.tag == categoryId) {
                chipGroup.check(chip.id)
                return
            }
        }
        if (categoryId == 0L || categoryId == -1L) {
            if (chipGroup.childCount > 0) {
                val firstChip = chipGroup.getChildAt(0) as Chip
                chipGroup.check(firstChip.id)
            }
        }
    }

    private fun createNoneChip(context: Context): Chip {
        val defaultBgColor = ContextCompat.getColor(context, R.color.tag_default_bg)
        val defaultTextColor = ContextCompat.getColor(context, R.color.tag_default_text)
        val uncheckedTextColor = ContextCompat.getColor(context, R.color.text_primary)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        return Chip(context).apply {
            id = View.generateViewId()
            tag = 0L
            text = context.getString(R.string.today_bottom_sheet_category_none)
            isCheckable = true
            isChecked = true
            chipBackgroundColor = ColorStateList(
                states,
                intArrayOf(defaultBgColor, Color.TRANSPARENT)
            )
            chipStrokeColor = ColorStateList(
                states,
                intArrayOf(defaultBgColor, defaultBgColor)
            )
            chipStrokeWidth = dpToPx(context, 2)
            setTextColor(ColorStateList(
                states,
                intArrayOf(defaultTextColor, uncheckedTextColor)
            ))
        }
    }

    private fun createAllChip(context: Context): Chip {
        val defaultBgColor = ContextCompat.getColor(context, R.color.tag_default_bg)
        val defaultTextColor = ContextCompat.getColor(context, R.color.tag_default_text)
        val uncheckedTextColor = ContextCompat.getColor(context, R.color.text_primary)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        return Chip(context).apply {
            id = View.generateViewId()
            tag = -1L
            text = context.getString(R.string.tasks_filter_chip_all)
            isCheckable = true
            isChecked = true
            chipBackgroundColor = ColorStateList(
                states,
                intArrayOf(defaultBgColor, Color.TRANSPARENT)
            )
            chipStrokeColor = ColorStateList(
                states,
                intArrayOf(defaultBgColor, defaultBgColor)
            )
            chipStrokeWidth = dpToPx(context, 2)
            setTextColor(ColorStateList(
                states,
                intArrayOf(defaultTextColor, uncheckedTextColor)
            ))
        }
    }

    private fun createAddChip(context: Context, onClick: () -> Unit): Chip {
        val strokeColor = ContextCompat.getColor(context, R.color.tag_default_bg)
        val textColor = ContextCompat.getColor(context, R.color.text_primary)

        return Chip(context).apply {
            id = View.generateViewId()
            tag = ADD_CHIP_TAG
            text = context.getString(R.string.category_chip_add)
            isCheckable = false
            isClickable = true
            chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
            chipStrokeColor = ColorStateList.valueOf(strokeColor)
            chipStrokeWidth = dpToPx(context, 2)
            setTextColor(textColor)
            setOnClickListener { onClick() }
        }
    }

    private fun createCategoryChip(context: Context, category: Category): Chip {
        return Chip(context).apply {
            id = View.generateViewId()
            tag = category.id
            text = category.name
            isCheckable = true
            isChecked = false
            try {
                val bgColor = Color.parseColor(category.colorHex)
                val luminance = Color.luminance(bgColor)
                val checkedTextColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                val uncheckedTextColor = ContextCompat.getColor(context, R.color.text_primary)

                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                )

                chipBackgroundColor = ColorStateList(
                    states,
                    intArrayOf(bgColor, Color.TRANSPARENT)
                )

                chipStrokeColor = ColorStateList(
                    states,
                    intArrayOf(bgColor, bgColor)
                )

                chipStrokeWidth = dpToPx(context, 2)

                setTextColor(ColorStateList(
                    states,
                    intArrayOf(checkedTextColor, uncheckedTextColor)
                ))
            } catch (e: Exception) {
                setChipBackgroundColorResource(R.color.tag_default_bg)
                setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
            }
        }
    }
}
