package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig

object ShortcutsManager {

    private const val SHORT_LABEL_MAX_SIZE = 10;
    private const val LONG_LABEL_MAX_SIZE = 25;
    private const val APPEND_ON = " On";
    private const val APPEND_OFF = " Off"
    const val CLASS_NAME_EXTRA_KEY = "className"

    private fun createAndPushShortcut(context : Context, intent : Intent, id : String, shortLabel : String,
                               longLabel : String, drawable : Int ) {
        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(IconCompat.createWithResource(context, drawable))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    fun createTunnelShortcuts(context : Context, tunnelConfig : TunnelConfig) {
        createAndPushShortcut(context,
            createTunnelOnIntent(context, mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig.toString())),
            tunnelConfig.id.toString() + APPEND_ON,
            tunnelConfig.name.take((SHORT_LABEL_MAX_SIZE - APPEND_ON.length)) + APPEND_ON,
            tunnelConfig.name.take((LONG_LABEL_MAX_SIZE - APPEND_ON.length)) + APPEND_ON,
            R.drawable.vpn_on
            )
        createAndPushShortcut(context,
            createTunnelOffIntent(context, mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig.toString())),
            tunnelConfig.id.toString() + APPEND_OFF,
            tunnelConfig.name.take((SHORT_LABEL_MAX_SIZE - APPEND_OFF.length)) + APPEND_OFF,
            tunnelConfig.name.take((LONG_LABEL_MAX_SIZE - APPEND_OFF.length)) + APPEND_OFF,
            R.drawable.vpn_off
        )
    }

    fun removeTunnelShortcuts(context : Context, tunnelConfig : TunnelConfig) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(tunnelConfig.id.toString() + APPEND_ON,
            tunnelConfig.id.toString() + APPEND_OFF ))
    }

    private fun createTunnelOnIntent(context: Context, extras : Map<String,String>) : Intent {
        return Intent(context, ShortcutsActivity::class.java).also {
            it.action = Action.START.name
            it.putExtra(CLASS_NAME_EXTRA_KEY, WireGuardTunnelService::class.java.name)
            extras.forEach {(k, v) ->
                it.putExtra(k, v)
            }
        }
    }

    private fun createTunnelOffIntent(context : Context, extras : Map<String,String>) : Intent {
        return Intent(context, ShortcutsActivity::class.java).also {
            it.action = Action.STOP.name
            it.putExtra(CLASS_NAME_EXTRA_KEY, WireGuardTunnelService::class.java.name)
            extras.forEach {(k, v) ->
                it.putExtra(k, v)
            }
        }
    }
}