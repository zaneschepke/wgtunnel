package com.zaneschepke.wireguardautotunnel.util.extensions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.core.location.LocationManagerCompat
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.tile.AutoTunnelControlTile
import com.zaneschepke.wireguardautotunnel.core.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.Constants
import java.io.InputStream

private const val BASELINE_HEIGHT = 2201
private const val BASELINE_WIDTH = 1080
private const val BASELINE_DENSITY = 2.625
private const val ANDROID_TV_SIZE_MULTIPLIER = 1.5f

fun Context.openWebUrl(url: String): Result<Unit> {
    return kotlin
        .runCatching {
            val webpage: Uri = Uri.parse(url)
            val intent =
                Intent(Intent.ACTION_VIEW, webpage).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            startActivity(intent)
        }
        .onFailure { showToast(R.string.no_browser_detected) }
}

fun Context.isBatteryOptimizationsDisabled(): Boolean {
    val pm = getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

val Context.actionBarSize
    get() =
        theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize)).let { attrs ->
            attrs.getDimension(0, 0F).toInt().also { attrs.recycle() }
        }

fun Context.resizeHeight(dp: Dp): Dp {
    val displayMetrics = resources.displayMetrics
    val density = displayMetrics.density
    val height =
        (displayMetrics.heightPixels - this.actionBarSize) *
            (if (isRunningOnTv()) ANDROID_TV_SIZE_MULTIPLIER else 1f)
    val resizeHeightPercentage =
        (height.toFloat() / BASELINE_HEIGHT) * (BASELINE_DENSITY.toFloat() / density)
    return dp * resizeHeightPercentage
}

fun Context.resizeHeight(textUnit: TextUnit): TextUnit {
    val displayMetrics = resources.displayMetrics
    val density = displayMetrics.density
    val height =
        (displayMetrics.heightPixels - actionBarSize) *
            (if (isRunningOnTv()) ANDROID_TV_SIZE_MULTIPLIER else 1f)
    val resizeHeightPercentage =
        (height.toFloat() / BASELINE_HEIGHT) * (BASELINE_DENSITY.toFloat() / density)
    return textUnit * resizeHeightPercentage * 1.1
}

fun Context.resizeWidth(dp: Dp): Dp {
    val displayMetrics = resources.displayMetrics
    val density = displayMetrics.density
    val width = displayMetrics.widthPixels
    val resizeWidthPercentage =
        (width.toFloat() / BASELINE_WIDTH) * (BASELINE_DENSITY.toFloat() / density)
    return dp * resizeWidthPercentage
}

fun Context.launchNotificationSettings() {
    if (isRunningOnTv()) return launchAppSettings()
    val settingsIntent: Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    this.startActivity(settingsIntent)
}

fun Context.launchShareFile(file: Uri) {
    val shareIntent =
        Intent().apply {
            action = Intent.ACTION_SEND
            type = Constants.ALL_FILE_TYPES
            putExtra(Intent.EXTRA_STREAM, file)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val chooserIntent =
        Intent.createChooser(shareIntent, "").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    this.startActivity(chooserIntent)
}

fun Context.isLocationServicesEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

fun Context.showToast(resId: Int) {
    Toast.makeText(this, this.getString(resId), Toast.LENGTH_LONG).show()
}

fun Context.launchSupportEmail() {
    val intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.my_email)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(
            Intent.createChooser(intent, getString(R.string.email_chooser)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } else {
        showToast(R.string.no_email_detected)
    }
}

fun Context.isRunningOnTv(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

fun Context.launchVpnSettings(): Result<Unit> {
    return kotlin.runCatching {
        val intent =
            Intent(Constants.VPN_SETTINGS_PACKAGE).apply { setFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        startActivity(intent)
    }
}

fun Context.getInputStreamFromUri(uri: Uri): InputStream? {
    return this.applicationContext.contentResolver.openInputStream(uri)
}

fun Context.launchLocationServicesSettings(): Result<Unit> {
    return kotlin.runCatching {
        val intent =
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(intent)
    }
}

fun Context.launchSettings(): Result<Unit> {
    return kotlin.runCatching {
        val intent =
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        startActivity(intent)
    }
}

fun Context.launchAppSettings() {
    kotlin.runCatching {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        startActivity(intent)
    }
}

fun Context.requestTunnelTileServiceStateUpdate() {
    TileService.requestListeningState(this, ComponentName(this, TunnelControlTile::class.java))
}

fun Context.requestAutoTunnelTileServiceUpdate() {
    TileService.requestListeningState(this, ComponentName(this, AutoTunnelControlTile::class.java))
}

fun Context.getAllInternetCapablePackages(): List<PackageInfo> {
    val permissions = arrayOf(Manifest.permission.INTERNET)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackagesHoldingPermissions(
            permissions,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        packageManager.getPackagesHoldingPermissions(permissions, 0)
    }
}
