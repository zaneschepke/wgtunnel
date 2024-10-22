package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import com.zaneschepke.wireguardautotunnel.BuildConfig
import java.util.Locale

object LocaleUtil {
	private const val DEFAULT_LANG = "en"
	val supportedLocales: Array<String> = BuildConfig.LANGUAGES
	const val OPTION_PHONE_LANGUAGE = "sys_def"

	/**
	 * returns the locale to use depending on the preference value
	 * when preference value = "sys_def" returns the locale of current system
	 * else it returns the locale code e.g. "en", "bn" etc.
	 */
	fun getLocaleFromPrefCode(prefCode: String): Locale {
		val localeCode = if (prefCode != OPTION_PHONE_LANGUAGE) {
			prefCode
		} else {
			val systemLang = ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)?.language ?: DEFAULT_LANG
			if (systemLang in supportedLocales) {
				systemLang
			} else {
				DEFAULT_LANG
			}
		}
		return Locale.forLanguageTag(localeCode)
	}

	fun getLocalizedConfiguration(prefLocaleCode: String): Configuration {
		val locale = getLocaleFromPrefCode(prefLocaleCode)
		return getLocalizedConfiguration(locale)
	}

	private fun getLocalizedConfiguration(locale: Locale): Configuration {
		val config = Configuration()
		return config.apply {
			config.setLayoutDirection(locale)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				config.setLocale(locale)
				val localeList = LocaleList(locale)
				LocaleList.setDefault(localeList)
				config.setLocales(localeList)
			} else {
				config.setLocale(locale)
			}
		}
	}

	fun getLocalizedContext(baseContext: Context, prefLocaleCode: String?): Context {
		if (prefLocaleCode == null) return baseContext
		val currentLocale = getLocaleFromPrefCode(prefLocaleCode)
		val baseLocale = getLocaleFromConfiguration(baseContext.resources.configuration)
		Locale.setDefault(currentLocale)
		return if (!baseLocale.toString().equals(currentLocale.toString(), ignoreCase = true)) {
			val config = getLocalizedConfiguration(currentLocale)
			baseContext.createConfigurationContext(config)
			baseContext
		} else {
			baseContext
		}
	}

	fun applyLocalizedContext(baseContext: Context, prefLocaleCode: String) {
		val currentLocale = getLocaleFromPrefCode(prefLocaleCode)
		val baseLocale = getLocaleFromConfiguration(baseContext.resources.configuration)
		Locale.setDefault(currentLocale)
		if (!baseLocale.toString().equals(currentLocale.toString(), ignoreCase = true)) {
			val config = getLocalizedConfiguration(currentLocale)
			baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
		}
	}

	@Suppress("DEPRECATION")
	private fun getLocaleFromConfiguration(configuration: Configuration): Locale {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			configuration.locales.get(0)
		} else {
			configuration.locale
		}
	}

	fun getLocalizedResources(resources: Resources, prefLocaleCode: String): Resources {
		val locale = getLocaleFromPrefCode(prefLocaleCode)
		val config = resources.configuration
		@Suppress("DEPRECATION")
		config.locale = locale
		config.setLayoutDirection(locale)

		@Suppress("DEPRECATION")
		resources.updateConfiguration(config, resources.displayMetrics)
		return resources
	}
}
