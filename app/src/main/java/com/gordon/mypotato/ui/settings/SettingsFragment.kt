package com.gordon.mypotato.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding
        get() = _binding!!

    private var selectedThemeMode: ThemeMode = ThemeMode.SYSTEM
    private var focusMinutes: Int = DEFAULT_FOCUS_MINUTES
    private var breakMinutes: Int = DEFAULT_BREAK_MINUTES
    private var isSoundEnabled: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupAppearanceSettings()
        setupLanguageSettings()
        setupDataSettings()
        setupPomodoroSettings()
        setupAboutSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvSettingsTitle.text = getString(R.string.settings_title)
    }

    private fun setupAppearanceSettings() {
        updateThemeText()
        binding.cardTheme.setOnClickListener {
            val options = ThemeMode.values().map { getString(it.labelRes) }.toTypedArray()
            val checkedItem = ThemeMode.values().indexOf(selectedThemeMode)
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择主题模式")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    selectedThemeMode = ThemeMode.values()[which]
                    updateThemeText()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updateThemeText() {
        binding.tvThemeValue.text = getString(selectedThemeMode.labelRes)
    }

    private fun setupLanguageSettings() {
        binding.cardLanguage.setOnClickListener {
            showToast("语言设置即将上线")
        }
    }

    private fun setupDataSettings() {
        binding.cardExportData.setOnClickListener {
            showToast(getString(R.string.settings_export_json_placeholder))
        }
    }

    private fun setupPomodoroSettings() {
        updatePomodoroText()
        
        binding.cardPomodoroFocus.setOnClickListener {
            showNumberInputDialog("设置工作时长（分钟）", focusMinutes) { newValue ->
                focusMinutes = newValue.coerceIn(MIN_MINUTES, MAX_MINUTES)
                updatePomodoroText()
            }
        }
        
        binding.cardPomodoroBreak.setOnClickListener {
            showNumberInputDialog("设置休息时长（分钟）", breakMinutes) { newValue ->
                breakMinutes = newValue.coerceIn(MIN_MINUTES, MAX_MINUTES)
                updatePomodoroText()
            }
        }
        
        binding.switchPomodoroSound.isChecked = isSoundEnabled
        binding.switchPomodoroSound.setOnCheckedChangeListener { _, isChecked ->
            isSoundEnabled = isChecked
        }
    }

    private fun updatePomodoroText() {
        binding.tvPomodoroFocusValue.text = "${focusMinutes}分钟"
        binding.tvPomodoroBreakValue.text = "${breakMinutes}分钟"
    }

    private fun showNumberInputDialog(title: String, currentValue: Int, onResult: (Int) -> Unit) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentValue.toString())
            setSelection(text.length)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val newValue = input.text.toString().toIntOrNull()
                if (newValue != null) {
                    onResult(newValue)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupAboutSettings() {
        val versionName = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { DEFAULT_VERSION_NAME }

        binding.tvVersionValue.text = versionName
        
        binding.cardCheckUpdate.setOnClickListener {
            showToast("已经是最新版本")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private enum class ThemeMode(val labelRes: Int) {
        SYSTEM(R.string.settings_theme_system),
        LIGHT(R.string.settings_theme_light),
        DARK(R.string.settings_theme_dark)
    }

    private companion object {
        private const val DEFAULT_FOCUS_MINUTES = 25
        private const val DEFAULT_BREAK_MINUTES = 5
        private const val MIN_MINUTES = 1
        private const val MAX_MINUTES = 120
        private const val DEFAULT_VERSION_NAME = "1.0.0"
    }
}
