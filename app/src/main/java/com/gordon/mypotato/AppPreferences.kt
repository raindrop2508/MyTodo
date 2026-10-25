package com.gordon.mypotato

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gordon.mypotato.viewmodel.LanguageMode
import com.gordon.mypotato.viewmodel.ThemeMode

object AppPreferences {
    private const val PREFS_NAME = "MyPotatoSettings"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_LANGUAGE_MODE = "language_mode"

    fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readThemeMode(context: Context): ThemeMode {
        return ThemeMode.fromValue(prefs(context).getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.value))
    }

    fun readLanguageMode(context: Context): LanguageMode {
        return LanguageMode.fromValue(prefs(context).getInt(KEY_LANGUAGE_MODE, LanguageMode.SYSTEM.value))
    }

    fun applyThemeMode(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun applyLanguageMode(mode: LanguageMode) {
        val locales = when (mode) {
            LanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            LanguageMode.ZH -> LocaleListCompat.forLanguageTags("zh-CN")
            LanguageMode.EN -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
