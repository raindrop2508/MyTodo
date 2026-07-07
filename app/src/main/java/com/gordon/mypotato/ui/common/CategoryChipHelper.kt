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
     */
    fun populateCategoryChips(
        chipGroup: ChipGroup,
        categories: List<Category>,
        selectedCategoryId: Long = 0L,
        onCategorySelected: ((Long) -> Unit)? = null
    ) {
        // 清空现有视图
        chipGroup.removeAllViews()

        // 1. 添加“无分类”选项
        val noneChip = createNoneChip(chipGroup.context)
        chipGroup.addView(noneChip)

        // 2. 遍历列表，添加具体的分类 Chip
        categories.forEach { category ->
            val chip = createCategoryChip(chipGroup.context, category)
            chipGroup.addView(chip)
        }

        // 3. 设置选中监听
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            // 如果没有任何选中项（点击了已选中的项），默认重新选中“无分类”
            if (checkedIds.isEmpty()) {
                chipGroup.check(noneChip.id)
                return@setOnCheckedStateChangeListener
            }
            
            // 获取当前选中的 Chip 绑定的分类 ID 并触发回调
            val checkedChip = chipGroup.findViewById<Chip>(checkedIds.first())
            val categoryId = checkedChip.tag as Long
            onCategorySelected?.invoke(categoryId)
        }

        // 4. 执行初始选中逻辑
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

        // 添加“全部”选项（Tag 为 null）
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

        // 如果传入 ID 为空，则选中“全部”（对应 Tag -1L 的逻辑在 selectCategory 中处理）
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
        return chip.tag as? Long ?: 0L
    }

    /**
     * 根据分类 ID 在 ChipGroup 中查找并选中对应的 Chip
     */
    fun selectCategory(chipGroup: ChipGroup, categoryId: Long) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            // 通过比较 tag 来匹配分类
            if (chip.tag == categoryId) {
                chipGroup.check(chip.id)
                return
            }
        }
        // 如果没找到或 ID 为特殊值（0 或 -1），默认选中第一个（通常是“无”或“全部”）
        if (categoryId == 0L || categoryId == -1L) {
            val firstChip = chipGroup.getChildAt(0) as Chip
            chipGroup.check(firstChip.id)
        }
    }

    /**
     * 创建“无分类” Chip
     * 支持选中/未选中状态切换
     */
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

    /**
     * 创建“全部”过滤 Chip
     * 支持选中/未选中状态切换
     */
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

    /**
     * 根据分类对象创建具体的 Chip
     * 包含动态颜色处理，支持选中/未选中状态切换
     */
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
