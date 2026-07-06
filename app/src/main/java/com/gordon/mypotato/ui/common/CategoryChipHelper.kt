package com.gordon.mypotato.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gordon.mypotato.R
import com.gordon.mypotato.domain.Category

object CategoryChipHelper {

    fun populateCategoryChips(
        chipGroup: ChipGroup,
        categories: List<Category>,
        selectedCategoryId: Long = 0L,
        onCategorySelected: ((Long) -> Unit)? = null
    ) {
        chipGroup.removeAllViews()

        val noneChip = createNoneChip(chipGroup.context)
        chipGroup.addView(noneChip)

        categories.forEach { category ->
            val chip = createCategoryChip(chipGroup.context, category)
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                chipGroup.check(noneChip.id)
                return@setOnCheckedStateChangeListener
            }
            val checkedChip = chipGroup.findViewById<Chip>(checkedIds.first())
            val categoryId = checkedChip.tag as Long
            onCategorySelected?.invoke(categoryId)
        }

        selectCategory(chipGroup, selectedCategoryId)
    }

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
            onCategoryFilterChanged?.invoke(categoryId)
        }

        val idToSelect = selectedCategoryId ?: -1L
        selectCategory(chipGroup, idToSelect)
    }

    fun getSelectedCategoryId(chipGroup: ChipGroup): Long {
        val checkedId = chipGroup.checkedChipId
        if (checkedId == View.NO_ID) return 0L
        val chip = chipGroup.findViewById<Chip>(checkedId)
        return chip.tag as Long
    }

    fun selectCategory(chipGroup: ChipGroup, categoryId: Long) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.tag == categoryId) {
                chipGroup.check(chip.id)
                return
            }
        }
        if (categoryId == 0L || categoryId == -1L) {
            val firstChip = chipGroup.getChildAt(0) as Chip
            chipGroup.check(firstChip.id)
        }
    }

    private fun createNoneChip(context: Context): Chip {
        return Chip(context).apply {
            id = View.generateViewId()
            tag = 0L
            text = context.getString(R.string.today_bottom_sheet_category_none)
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.tag_default_bg)
            setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
        }
    }

    private fun createAllChip(context: Context): Chip {
        return Chip(context).apply {
            id = View.generateViewId()
            tag = null
            text = context.getString(R.string.tasks_filter_chip_all)
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.tag_default_bg)
            setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
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
                chipBackgroundColor = ColorStateList.valueOf(bgColor)
                val luminance = Color.luminance(bgColor)
                val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                setTextColor(textColor)
            } catch (e: Exception) {
                setChipBackgroundColorResource(R.color.tag_default_bg)
                setTextColor(ContextCompat.getColor(context, R.color.tag_default_text))
            }
        }
    }
}