package com.gordon.mypotato.ui.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.gordon.mypotato.R
import com.gordon.mypotato.databinding.FragmentSettingsBinding
import com.google.android.material.button.MaterialButton

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding
        get() = _binding!!

    private var selectedThemeMode: ThemeMode = ThemeMode.LIGHT
    private var focusMinutes: Int = DEFAULT_FOCUS_MINUTES
    private var breakMinutes: Int = DEFAULT_BREAK_MINUTES

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
        setupThemeButtons()
        setupPomodoroInputs()
        setupDataActions()
        setupVersion()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader() {
        binding.tvSettingsTitle.text = getString(R.string.settings_title)
        binding.tvSettingsSubtitle.text = getString(R.string.settings_subtitle)
    }

    private fun setupThemeButtons() {
        binding.btnThemeSystem.apply {
            alpha = DISABLED_THEME_ALPHA
            setOnClickListener { showToast(getString(R.string.settings_theme_locked_hint)) }
        }
        binding.btnThemeLight.setOnClickListener { selectThemeMode(ThemeMode.LIGHT) }
        binding.btnThemeDark.apply {
            alpha = DISABLED_THEME_ALPHA
            setOnClickListener { showToast(getString(R.string.settings_theme_locked_hint)) }
        }
        renderThemeButtons()
    }

    private fun selectThemeMode(mode: ThemeMode) {
        selectedThemeMode = mode
        renderThemeButtons()
        if (mode == ThemeMode.LIGHT) {
            showToast(getString(R.string.settings_theme_selected_hint, getString(mode.labelRes)))
        } else {
            showToast(getString(R.string.settings_theme_locked_hint))
        }
    }

    private fun renderThemeButtons() {
        updateThemeButton(binding.btnThemeSystem, selectedThemeMode == ThemeMode.SYSTEM)
        updateThemeButton(binding.btnThemeLight, selectedThemeMode == ThemeMode.LIGHT)
        updateThemeButton(binding.btnThemeDark, selectedThemeMode == ThemeMode.DARK)
    }

    private fun updateThemeButton(button: MaterialButton, selected: Boolean) {
        val selectedBackground = resolveThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
        val selectedForeground = resolveThemeColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
        val defaultBackground = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        val defaultForeground = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val defaultStroke = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)

        button.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBackground else defaultBackground)
        button.strokeColor = ColorStateList.valueOf(if (selected) selectedForeground else defaultStroke)
        button.setTextColor(if (selected) selectedForeground else defaultForeground)
        button.iconTint = ColorStateList.valueOf(if (selected) selectedForeground else defaultForeground)
    }

    private fun setupPomodoroInputs() {
        binding.etFocusMinutes.setText(focusMinutes.toString())
        binding.etBreakMinutes.setText(breakMinutes.toString())

        bindMinuteInput(
            editText = binding.etFocusMinutes,
            onMinuteChanged = { focusMinutes = it }
        )
        bindMinuteInput(
            editText = binding.etBreakMinutes,
            onMinuteChanged = { breakMinutes = it }
        )
    }

    private fun bindMinuteInput(
        editText: com.google.android.material.textfield.TextInputEditText,
        onMinuteChanged: (Int) -> Unit
    ) {
        editText.doAfterTextChanged { editable ->
            val text = editable?.toString()?.trim().orEmpty()
            val parsed = text.toIntOrNull() ?: return@doAfterTextChanged
            val sanitized = parsed.coerceIn(MIN_MINUTES, MAX_MINUTES)
            if (sanitized != parsed) {
                editText.setText(sanitized.toString())
                editText.setSelection(editText.text?.length ?: 0)
                return@doAfterTextChanged
            }
            onMinuteChanged(sanitized)
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) return@setOnFocusChangeListener
            val sanitized = sanitizeMinutes(editText.text?.toString())
            editText.setText(sanitized.toString())
            editText.setSelection(editText.text?.length ?: 0)
            onMinuteChanged(sanitized)
        }
    }

    private fun setupDataActions() {
        binding.btnExportJson.setOnClickListener {
            showToast(getString(R.string.settings_export_json_placeholder))
        }
        binding.btnExportCsv.setOnClickListener {
            showToast(getString(R.string.settings_export_csv_placeholder))
        }
        binding.btnImportJson.setOnClickListener {
            showToast(getString(R.string.settings_import_json_placeholder))
        }
    }

    private fun setupVersion() {
        val versionName = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { DEFAULT_VERSION_NAME }

        binding.tvSettingsVersion.text = getString(
            R.string.settings_version_format,
            getString(R.string.app_name),
            versionName
        )
    }

    private fun sanitizeMinutes(rawValue: String?): Int {
        val parsed = rawValue?.trim()?.toIntOrNull() ?: MIN_MINUTES
        return parsed.coerceIn(MIN_MINUTES, MAX_MINUTES)
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val outValue = TypedValue()
        if (requireContext().theme.resolveAttribute(attrRes, outValue, true)) {
            return outValue.data
        }
        return requireContext().getColor(R.color.purple_500)
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
        private const val DEFAULT_VERSION_NAME = "1.0"
        private const val DISABLED_THEME_ALPHA = 0.45f
    }
}
