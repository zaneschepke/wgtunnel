package com.zaneschepke.wireguardautotunnel.core.shortcut

interface ShortcutManager {
    suspend fun addShortcuts()

    suspend fun removeShortcuts()
}
