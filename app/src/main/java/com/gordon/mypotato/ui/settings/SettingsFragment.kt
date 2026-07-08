package com.gordon.mypotato.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentSettingsBinding
import com.gordon.mypotato.viewmodel.SettingsViewModel
import com.gordon.mypotato.viewmodel.ThemeMode
import com.gordon.mypotato.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding
        get() = _binding!!

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(requireContext()))[SettingsViewModel::class.java]
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
        collectUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvSettingsTitle.text = getString(R.string.settings_title)
    }

    private fun setupAppearanceSettings() {
        binding.cardTheme.setOnClickListener {
            val options = arrayOf(
                getString(R.string.settings_theme_system),
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark)
            )
            val currentMode = viewModel.uiState.value.themeMode
            val checkedItem = when (currentMode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
                else -> 0
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择主题模式")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val newMode = when (which) {
                        0 -> ThemeMode.SYSTEM
                        1 -> ThemeMode.LIGHT
                        2 -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                    viewModel.updateThemeMode(newMode)
                    dialog.dismiss()
                }
                .show()
        }
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
        binding.cardPomodoroFocus.setOnClickListener {
            showNumberInputDialog("设置工作时长（分钟）", viewModel.uiState.value.focusMinutes) { newValue ->
                viewModel.updateFocusMinutes(newValue)
            }
        }

        binding.cardPomodoroBreak.setOnClickListener {
            showNumberInputDialog("设置休息时长（分钟）", viewModel.uiState.value.shortBreakMinutes) { newValue ->
                viewModel.updateShortBreakMinutes(newValue)
            }
        }

        binding.switchPomodoroSound.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleSound()
        }
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
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }

        binding.tvVersionValue.text = versionName

        binding.cardCheckUpdate.setOnClickListener {
            showToast("已经是最新版本")
        }
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.tvThemeValue.text = when (state.themeMode) {
                    ThemeMode.SYSTEM -> getString(R.string.settings_theme_system)
                    ThemeMode.LIGHT -> getString(R.string.settings_theme_light)
                    ThemeMode.DARK -> getString(R.string.settings_theme_dark)
                    else -> getString(R.string.settings_theme_system)
                }

                binding.tvPomodoroFocusValue.text = "${state.focusMinutes}分钟"
                binding.tvPomodoroBreakValue.text = "${state.shortBreakMinutes}分钟"

                binding.switchPomodoroSound.setOnCheckedChangeListener(null)
                binding.switchPomodoroSound.isChecked = state.isSoundEnabled
                binding.switchPomodoroSound.setOnCheckedChangeListener { _, _ ->
                    viewModel.toggleSound()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}