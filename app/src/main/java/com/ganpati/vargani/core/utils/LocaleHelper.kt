package com.ganpati.vargani.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ganpati.vargani.domain.model.AppLanguage
import java.util.Locale

/**
 * Per-app language switching.
 *
 * Uses SharedPreferences for a synchronous read in [attachBaseContext],
 * plus [AppCompatDelegate.setApplicationLocales] and an activity [Activity.recreate]
 * so Compose `stringResource` values refresh immediately.
 */
object LocaleHelper {

    private const val PREFS_NAME = "vargani_locale_prefs"
    private const val KEY_LANGUAGE = "language_code"

    fun storedLanguage(context: Context): AppLanguage {
        val code = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code)
        return AppLanguage.fromCode(code)
    }

    /** Keeps SharedPreferences in sync without recreating the UI. */
    fun persist(context: Context, language: AppLanguage) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    fun wrap(context: Context): Context {
        return wrap(context, storedLanguage(context))
    }

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = localeFor(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    /**
     * Persists language and recreates the host activity so the whole UI reloads
     * with the new string resources.
     */
    fun changeLanguage(context: Context, language: AppLanguage) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .commit()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code),
        )

        context.findActivity()?.recreate()
    }

    fun current(): AppLanguage {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isBlank()) return AppLanguage.ENGLISH
        return AppLanguage.fromCode(tags.substringBefore(',').substringBefore('-'))
    }

    private fun localeFor(language: AppLanguage): Locale = when (language) {
        AppLanguage.MARATHI -> Locale.forLanguageTag("mr-IN")
        AppLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
