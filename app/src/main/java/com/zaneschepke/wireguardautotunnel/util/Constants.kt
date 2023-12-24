package com.zaneschepke.wireguardautotunnel.util

object Constants {
    const val MANUAL_TUNNEL_CONFIG_ID = "0"
    const val BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutes
    const val DEFAULT_WATCHER_WAKE_LOCK_TIMEOUT = 30 * 60 * 1000L // 30 minutes
    const val VPN_STATISTIC_CHECK_INTERVAL = 1000L
    const val VPN_CONNECTED_NOTIFICATION_DELAY = 3000L
    const val TOGGLE_TUNNEL_DELAY = 300L
    const val CONF_FILE_EXTENSION = ".conf"
    const val ZIP_FILE_EXTENSION = ".zip"
    const val URI_CONTENT_SCHEME = "content"
    const val URI_PACKAGE_SCHEME = "package"
    const val ALLOWED_FILE_TYPES = "*/*"
    const val GOOGLE_TV_EXPLORER_STUB = "com.google.android.tv.frameworkpackagestubs"
    const val ANDROID_TV_EXPLORER_STUB = "com.android.tv.frameworkpackagestubs"
    const val EMAIL_MIME_TYPE = "message/rfc822"
    const val SYSTEM_EXEMPT_SERVICE_TYPE_ID = 1024

    const val SUBSCRIPTION_TIMEOUT = 5_000L
    const val FOCUS_REQUEST_DELAY = 500L
}
