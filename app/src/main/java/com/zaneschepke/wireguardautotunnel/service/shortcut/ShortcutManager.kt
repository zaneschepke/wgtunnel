package com.zaneschepke.wireguardautotunnel.service.shortcut

interface ShortcutManager {
	suspend fun addShortcuts()
	suspend fun removeShortcuts()
}
