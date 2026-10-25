package com.gordon.mypotato.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.DialogNumberStepperBinding
import com.gordon.mypotato.databinding.FragmentSettingsBinding
import com.gordon.mypotato.viewmodel.LanguageMode
import com.gordon.mypotato.viewmodel.SettingsViewModel
import com.gordon.mypotato.viewmodel.ThemeMode
import com.gordon.mypotato.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding
        get() = _binding!!

    private lateinit var viewModel: SettingsViewModel

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            exportToUri(uri)
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            confirmImport(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                getString(R.string.settings_theme_dark),
            )
            val checkedItem = when (viewModel.uiState.value.themeMode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_theme_dialog_title)
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val newMode = when (which) {
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
            val options = arrayOf(
                getString(R.string.settings_language_system),
                getString(R.string.settings_language_zh),
                getString(R.string.settings_language_en),
            )
            val checkedItem = when (viewModel.uiState.value.languageMode) {
                LanguageMode.SYSTEM -> 0
                LanguageMode.ZH -> 1
                LanguageMode.EN -> 2
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_language_dialog_title)
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val newMode = when (which) {
                        1 -> LanguageMode.ZH
                        2 -> LanguageMode.EN
                        else -> LanguageMode.SYSTEM
                    }
                    viewModel.updateLanguageMode(newMode)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupDataSettings() {
        binding.cardExportData.setOnClickListener {
            createDocumentLauncher.launch(getString(R.string.settings_backup_file_name))
        }
        binding.cardImportData.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        }
        binding.cardClearData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_clear_confirm_title)
                .setMessage(R.string.settings_clear_confirm_message)
                .setPositiveButton(R.string.settings_dialog_confirm) { _, _ -> clearData() }
                .setNegativeButton(R.string.settings_dialog_cancel, null)
                .show()
        }
    }

    private fun setupPomodoroSettings() {
        binding.cardPomodoroFocus.setOnClickListener {
            showNumberStepperDialog(
                title = getString(R.string.settings_pomodoro_focus_dialog),
                currentValue = viewModel.uiState.value.focusMinutes,
                min = 1,
                max = 120,
                unit = getString(R.string.settings_unit_minutes),
            ) { viewModel.updateFocusMinutes(it) }
        }
        binding.cardPomodoroBreak.setOnClickListener {
            showNumberStepperDialog(
                title = getString(R.string.settings_pomodoro_break_dialog),
                currentValue = viewModel.uiState.value.shortBreakMinutes,
                min = 1,
                max = 60,
                unit = getString(R.string.settings_unit_minutes),
            ) { viewModel.updateShortBreakMinutes(it) }
        }
        binding.cardPomodoroLongBreak.setOnClickListener {
            showNumberStepperDialog(
                title = getString(R.string.settings_pomodoro_long_break_dialog),
                currentValue = viewModel.uiState.value.longBreakMinutes,
                min = 1,
                max = 120,
                unit = getString(R.string.settings_unit_minutes),
            ) { viewModel.updateLongBreakMinutes(it) }
        }
        binding.cardPomodoroInterval.setOnClickListener {
            showNumberStepperDialog(
                title = getString(R.string.settings_pomodoro_interval_dialog),
                currentValue = viewModel.uiState.value.longBreakInterval,
                min = 1,
                max = 10,
                unit = getString(R.string.settings_unit_pomodoros),
            ) { viewModel.updateLongBreakInterval(it) }
        }
    }

    private fun showNumberStepperDialog(
        title: String,
        currentValue: Int,
        min: Int,
        max: Int,
        unit: String,
        onResult: (Int) -> Unit,
    ) {
        val dialogBinding = DialogNumberStepperBinding.inflate(layoutInflater)
        var value = currentValue.coerceIn(min, max)

        fun render(syncInput: Boolean) {
            dialogBinding.tvValue.text = value.toString()
            dialogBinding.tvUnit.text = unit
            dialogBinding.btnDecrease.isEnabled = value > min
            dialogBinding.btnIncrease.isEnabled = value < max
            dialogBinding.btnDecrease.alpha = if (value > min) 1f else 0.4f
            dialogBinding.btnIncrease.alpha = if (value < max) 1f else 0.4f
            if (syncInput) {
                val text = value.toString()
                if (dialogBinding.etDirectInput.text?.toString() != text) {
                    dialogBinding.etDirectInput.setText(text)
                    dialogBinding.etDirectInput.setSelection(text.length)
                }
            }
        }

        dialogBinding.btnDecrease.setOnClickListener {
            value = (value - 1).coerceAtLeast(min)
            render(syncInput = true)
        }
        dialogBinding.btnIncrease.setOnClickListener {
            value = (value + 1).coerceAtMost(max)
            render(syncInput = true)
        }
        dialogBinding.etDirectInput.doAfterTextChanged { editable ->
            val parsed = editable?.toString()?.toIntOrNull() ?: return@doAfterTextChanged
            value = parsed.coerceIn(min, max)
            render(syncInput = false)
        }

        render(syncInput = true)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.settings_dialog_confirm) { _, _ ->
                val typed = dialogBinding.etDirectInput.text?.toString()?.toIntOrNull()
                onResult((typed ?: value).coerceIn(min, max))
            }
            .setNegativeButton(R.string.settings_dialog_cancel, null)
            .show()
    }

    private fun setupAboutSettings() {
        val versionName = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }

        binding.tvVersionValue.text = versionName

        binding.cardCheckUpdate.setOnClickListener {
            showToast(getString(R.string.settings_check_update_latest))
        }
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.tvThemeValue.text = when (state.themeMode) {
                    ThemeMode.SYSTEM -> getString(R.string.settings_theme_system)
                    ThemeMode.LIGHT -> getString(R.string.settings_theme_light)
                    ThemeMode.DARK -> getString(R.string.settings_theme_dark)
                }
                binding.tvLanguageValue.text = when (state.languageMode) {
                    LanguageMode.SYSTEM -> getString(R.string.settings_item_language_value_system)
                    LanguageMode.ZH -> getString(R.string.settings_item_language_value_zh)
                    LanguageMode.EN -> getString(R.string.settings_item_language_value_en)
                }

                binding.tvPomodoroFocusValue.text = getString(R.string.settings_minutes_format, state.focusMinutes)
                binding.tvPomodoroBreakValue.text = getString(R.string.settings_minutes_format, state.shortBreakMinutes)
                binding.tvPomodoroLongBreakValue.text = getString(R.string.settings_minutes_format, state.longBreakMinutes)
                binding.tvPomodoroIntervalValue.text = getString(R.string.settings_interval_format, state.longBreakInterval)

                binding.switchPomodoroSound.setOnCheckedChangeListener(null)
                binding.switchPomodoroSound.isChecked = state.isSoundEnabled
                binding.switchPomodoroSound.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setSoundEnabled(isChecked)
                }
            }
        }
    }

    private fun exportToUri(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                val json = viewModel.exportJson()
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(StandardCharsets.UTF_8))
                    } ?: error("openOutputStream failed")
                }
            }.onSuccess {
                showToast(getString(R.string.settings_export_success))
            }.onFailure {
                showToast(getString(R.string.settings_export_failed))
            }
        }
    }

    private fun confirmImport(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_import_confirm_title)
            .setMessage(R.string.settings_import_confirm_message)
            .setPositiveButton(R.string.settings_dialog_confirm) { _, _ -> importFromUri(uri) }
            .setNegativeButton(R.string.settings_dialog_cancel, null)
            .show()
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                    } ?: error("openInputStream failed")
                }
                viewModel.importJson(json)
            }.onSuccess {
                showToast(getString(R.string.settings_import_success))
            }.onFailure {
                showToast(getString(R.string.settings_import_failed))
            }
        }
    }

    private fun clearData() {
        lifecycleScope.launch {
            runCatching {
                viewModel.clearAllData()
            }.onSuccess {
                showToast(getString(R.string.settings_clear_success))
            }.onFailure {
                showToast(getString(R.string.settings_clear_failed))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
