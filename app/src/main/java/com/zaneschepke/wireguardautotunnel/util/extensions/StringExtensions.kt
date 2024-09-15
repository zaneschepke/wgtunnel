package com.zaneschepke.wireguardautotunnel.util.extensions

import timber.log.Timber
import java.util.regex.Pattern

fun String.isValidIpv4orIpv6Address(): Boolean {
	val ipv4Pattern = Pattern.compile(
		"^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\$",
	)
	val ipv6Pattern = Pattern.compile(
		"^([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}\$",
	)
	return ipv4Pattern.matcher(this).matches() || ipv6Pattern.matcher(this).matches()
}

fun List<String>.isMatchingToWildcardList(value: String): Boolean {
	val excludeValues = this.filter { it.startsWith("!") }.map { it.removePrefix("!").toRegexWithWildcards() }
	Timber.d("Excluded values: $excludeValues")
	val includedValues = this.filter { !it.startsWith("!") }.map { it.toRegexWithWildcards() }
	Timber.d("Included values: $includedValues")
	val matches = includedValues.filter { it.matches(value) }
	val excludedMatches = excludeValues.filter { it.matches(value) }
	Timber.d("Excluded matches: $excludedMatches")
	Timber.d("Matches: $matches")
	return matches.isNotEmpty() && excludedMatches.isEmpty()
}

fun String.toRegexWithWildcards(): Regex {
	return this.replace("*", ".*").replace("?", ".").toRegex()
}
