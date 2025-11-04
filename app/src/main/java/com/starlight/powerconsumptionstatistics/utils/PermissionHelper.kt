package com.starlight.powerconsumptionstatistics.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限辅助类
 * 负责检查和请求各种权限
 */
class PermissionHelper(private val activity: Activity) {

    companion object {
        const val REQUEST_OVERLAY_PERMISSION = 1001
        const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }

    /**
     * 检查悬浮窗权限
     */
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(activity)
        } else {
            true // Android 6.0 以下不需要此权限
        }
    }

    /**
     * 请求悬浮窗权限
     * 会跳转到系统设置页面
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    /**
     * 检查通知权限（Android 13+）
     */
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 以下不需要运行时权限
        }
    }

    /**
     * 请求通知权限（Android 13+）
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    /**
     * 检查所有必需权限
     */
    fun checkAllPermissions(): Boolean {
        return checkOverlayPermission() && checkNotificationPermission()
    }

    /**
     * 打开应用详情设置页面
     * 用于引导用户手动授权权限
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
