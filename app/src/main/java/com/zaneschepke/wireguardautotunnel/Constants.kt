package com.zaneschepke.wireguardautotunnel

object Constants {
    const val MANUAL_TUNNEL_CONFIG_ID = "0"
    const val WATCHER_SERVICE_WAKE_LOCK_TIMEOUT = 10*60*1000L /*10 minute*/
    const val VPN_CONNECTIVITY_CHECK_INTERVAL = 3000L
    const val VPN_STATISTIC_CHECK_INTERVAL = 10000L
    const val TOGGLE_TUNNEL_DELAY = 500L
    const val FADE_IN_ANIMATION_DURATION = 1000
    const val SLIDE_IN_ANIMATION_DURATION = 500
    const val SLIDE_IN_TRANSITION_OFFSET = 1000
    const val CONF_FILE_EXTENSION = ".conf"
    const val ZIP_FILE_EXTENSION = ".zip"
    const val URI_CONTENT_SCHEME = "content"
    const val URI_PACKAGE_SCHEME = "package"
    const val ALLOWED_FILE_TYPES = "*/*"
    const val GOOGLE_TV_EXPLORER_STUB = "com.google.android.tv.frameworkpackagestubs"
    const val ANDROID_TV_EXPLORER_STUB = "com.android.tv.frameworkpackagestubs"
}