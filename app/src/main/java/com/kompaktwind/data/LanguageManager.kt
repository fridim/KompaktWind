package com.kompaktwind.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

data class AppLanguage(val tag: String?, val label: String)

class LanguageManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentTag(): String? = prefs.getString(KEY, null)

    fun setTag(tag: String?) {
        val editor = prefs.edit()
        if (tag.isNullOrBlank()) editor.remove(KEY) else editor.putString(KEY, tag)
        editor.apply()
    }

    fun wrap(context: Context): Context {
        val tag = currentTag()
        if (tag.isNullOrBlank()) {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return context
        }

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }

    companion object {
        private const val PREFS = "kompaktwind_language"
        private const val KEY = "language_tag"

        val languages: List<AppLanguage> = listOf(
            AppLanguage(null, ""),
            AppLanguage("en", "English"),
            AppLanguage("fr", "Français"),
            AppLanguage("de", "Deutsch"),
            AppLanguage("es", "Español"),
            AppLanguage("it", "Italiano"),
            AppLanguage("pt", "Português"),
            AppLanguage("nl", "Nederlands"),
            AppLanguage("pl", "Polski"),
            AppLanguage("cs", "Čeština"),
            AppLanguage("sv", "Svenska"),
        )
    }
}
