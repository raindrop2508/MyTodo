package com.gordon.mypotato.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gordon.mypotato.AppPreferences
import com.gordon.mypotato.data.backup.RoomBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

enum class LanguageMode(val value: Int) {
    SYSTEM(0),
    ZH(1),
    EN(2);

    companion object {
        fun fromValue(value: Int): LanguageMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

data class SettingsUiState(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val isSoundEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
)

class SettingsViewModel(
    private val context: Context,
    private val backupRepository: RoomBackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val sharedPreferences: SharedPreferences = AppPreferences.prefs(context)

    companion object {
        const val KEY_FOCUS_MINUTES = "focus_minutes"
        const val KEY_SHORT_BREAK_MINUTES = "short_break_minutes"
        const val KEY_LONG_BREAK_MINUTES = "long_break_minutes"
        const val KEY_LONG_BREAK_INTERVAL = "long_break_interval"
        private const val KEY_IS_SOUND_ENABLED = "is_sound_enabled"

        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_LONG_BREAK_INTERVAL = 4
        private const val DEFAULT_IS_SOUND_ENABLED = true
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                focusMinutes = sharedPreferences.getInt(KEY_FOCUS_MINUTES, DEFAULT_FOCUS_MINUTES),
                shortBreakMinutes = sharedPreferences.getInt(KEY_SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES),
                longBreakMinutes = sharedPreferences.getInt(KEY_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES),
                longBreakInterval = sharedPreferences.getInt(KEY_LONG_BREAK_INTERVAL, DEFAULT_LONG_BREAK_INTERVAL),
                isSoundEnabled = sharedPreferences.getBoolean(KEY_IS_SOUND_ENABLED, DEFAULT_IS_SOUND_ENABLED),
                themeMode = AppPreferences.readThemeMode(context),
                languageMode = AppPreferences.readLanguageMode(context),
            )
        }
    }

    fun updateFocusMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 120)
        _uiState.value = _uiState.value.copy(focusMinutes = clamped)
        saveSetting(KEY_FOCUS_MINUTES, clamped)
    }

    fun updateShortBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        _uiState.value = _uiState.value.copy(shortBreakMinutes = clamped)
        saveSetting(KEY_SHORT_BREAK_MINUTES, clamped)
    }

    fun updateLongBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 120)
        _uiState.value = _uiState.value.copy(longBreakMinutes = clamped)
        saveSetting(KEY_LONG_BREAK_MINUTES, clamped)
    }

    fun updateLongBreakInterval(interval: Int) {
        val clamped = interval.coerceIn(1, 10)
        _uiState.value = _uiState.value.copy(longBreakInterval = clamped)
        saveSetting(KEY_LONG_BREAK_INTERVAL, clamped)
    }

    fun toggleSound() {
        setSoundEnabled(!_uiState.value.isSoundEnabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        if (_uiState.value.isSoundEnabled == enabled) return
        _uiState.value = _uiState.value.copy(isSoundEnabled = enabled)
        saveSetting(KEY_IS_SOUND_ENABLED, enabled)
    }

    fun updateThemeMode(mode: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        sharedPreferences.edit().putInt(AppPreferences.KEY_THEME_MODE, mode.value).apply()
        AppPreferences.applyThemeMode(mode)
    }

    fun updateLanguageMode(mode: LanguageMode) {
        _uiState.value = _uiState.value.copy(languageMode = mode)
        sharedPreferences.edit().putInt(AppPreferences.KEY_LANGUAGE_MODE, mode.value).apply()
        AppPreferences.applyLanguageMode(mode)
    }

    suspend fun exportJson(): String = backupRepository.exportJson()

    suspend fun importJson(json: String) = backupRepository.importJson(json)

    suspend fun clearAllData() = backupRepository.clearAllAndReseed()

    private fun saveSetting(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    private fun saveSetting(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getPomodoroSettings(): PomodoroSettings {
        val state = _uiState.value
        return PomodoroSettings(
            focusMinutes = state.focusMinutes,
            shortBreakMinutes = state.shortBreakMinutes,
            longBreakMinutes = state.longBreakMinutes,
            longBreakInterval = state.longBreakInterval,
        )
    }
}

data class PomodoroSettings(
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakInterval: Int,
)
