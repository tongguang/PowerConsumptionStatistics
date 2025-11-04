package com.starlight.powerconsumptionstatistics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.starlight.powerconsumptionstatistics.MainActivity
import com.starlight.powerconsumptionstatistics.R
import com.starlight.powerconsumptionstatistics.manager.BatteryInfoManager
import com.starlight.powerconsumptionstatistics.manager.FloatingWindowManager

/**
 * 功耗监控前台服务
 * 负责后台运行、显示悬浮窗和定时更新数据
 */
class PowerMonitorService : Service() {

    private var batteryInfoManager: BatteryInfoManager? = null
    private lateinit var floatingWindowManager: FloatingWindowManager

    private val handler = Handler(Looper.getMainLooper())
    private var updateInterval = 1000L // 默认 1 秒更新一次

    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "power_monitor_channel"
        private const val CHANNEL_NAME = "功耗监控服务"

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_UPDATE_INTERVAL = "EXTRA_UPDATE_INTERVAL"

        // 服务运行状态标志位（用于替代已弃用的 getRunningServices）
        @Volatile
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        floatingWindowManager = FloatingWindowManager(this)
        // BatteryInfoManager 会在 onStartCommand 中根据设置创建
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // 获取更新间隔（如果有）
                updateInterval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, 1000L)

                // 读取电流类型设置
                val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                val preferAverageCurrent = sharedPreferences.getBoolean("use_average_current", false)

                // 根据设置创建 BatteryInfoManager
                batteryInfoManager = BatteryInfoManager(this, preferAverageCurrent)

                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }
            else -> {
                // 默认情况也需要初始化 BatteryInfoManager
                val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                val preferAverageCurrent = sharedPreferences.getBoolean("use_average_current", false)
                batteryInfoManager = BatteryInfoManager(this, preferAverageCurrent)

                startMonitoring()
            }
        }

        return START_STICKY
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        if (isRunning) return

        // 创建通知渠道
        createNotificationChannel()

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        // 显示悬浮窗
        floatingWindowManager.show()

        // 开始定时更新
        isRunning = true
        isServiceRunning = true
        startPeriodicUpdate()
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        isRunning = false
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
        floatingWindowManager.hide()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // 低重要性，不发出声音
            ).apply {
                description = "显示功耗监控悬浮窗"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("功耗监控运行中")
            .setContentText("点击打开应用")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 不可滑动删除
            .setSilent(true)  // 静默通知
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * 开始定时更新
     */
    private fun startPeriodicUpdate() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    updateBatteryInfo()
                    handler.postDelayed(this, updateInterval)
                }
            }
        }
        handler.post(updateRunnable)
    }

    /**
     * 更新电池信息
     */
    private fun updateBatteryInfo() {
        try {
            val batteryData = batteryInfoManager?.getBatteryData() ?: return
            floatingWindowManager.updateData(batteryData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
