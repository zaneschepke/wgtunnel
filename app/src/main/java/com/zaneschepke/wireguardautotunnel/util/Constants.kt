package com.zaneschepke.wireguardautotunnel.util

object Constants {

    const val BASE_LOG_FILE_NAME = "wgtunnel-logs"
    const val LOG_BUFFER_SIZE = 3_000L

    const val MANUAL_TUNNEL_CONFIG_ID = "0"
    const val BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT = 10 * 60 * 1_000L // 10 minutes
    const val VPN_STATISTIC_CHECK_INTERVAL = 1_000L
    const val VPN_CONNECTED_NOTIFICATION_DELAY = 3_000L
    const val TOGGLE_TUNNEL_DELAY = 300L
    const val WATCHER_COLLECTION_DELAY = 1_000L
    const val CONF_FILE_EXTENSION = ".conf"
    const val ZIP_FILE_EXTENSION = ".zip"
    const val URI_CONTENT_SCHEME = "content"
    const val ALLOWED_FILE_TYPES = "*/*"
    const val TEXT_MIME_TYPE = "text/plain"
    const val GOOGLE_TV_EXPLORER_STUB = "com.google.android.tv.frameworkpackagestubs"
    const val ANDROID_TV_EXPLORER_STUB = "com.android.tv.frameworkpackagestubs"
    const val ALWAYS_ON_VPN_ACTION = "android.net.VpnService"
    const val EMAIL_MIME_TYPE = "message/rfc822"
    const val SYSTEM_EXEMPT_SERVICE_TYPE_ID = 1024

    const val SUBSCRIPTION_TIMEOUT = 5_000L
    const val FOCUS_REQUEST_DELAY = 500L

    const val BACKUP_PING_HOST = "1.1.1.1"
    const val PING_TIMEOUT = 5_000L
    const val VPN_RESTART_DELAY = 1_000L
    const val PING_INTERVAL = 60_000L
    const val PING_COOLDOWN = PING_INTERVAL * 60 //one hour

    const val ALLOWED_DISPLAY_NAME_LENGTH = 20

    const val TUNNEL_EXTRA_KEY = "tunnelId"

    const val UNREADABLE_SSID = "<unknown ssid>"

}
