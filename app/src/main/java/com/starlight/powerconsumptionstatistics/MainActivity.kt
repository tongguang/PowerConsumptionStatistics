package com.starlight.powerconsumptionstatistics

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.starlight.powerconsumptionstatistics.service.PowerMonitorService
import com.starlight.powerconsumptionstatistics.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var sharedPreferences: SharedPreferences

    // UI 组件
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var tvPermissionHint: TextView
    private lateinit var btnStartService: MaterialButton
    private lateinit var btnStopService: MaterialButton
    private lateinit var btnSettings: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化
        permissionHelper = PermissionHelper(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // 绑定 UI 组件
        initViews()

        // 检查服务状态并更新 UI
        updateServiceStatus()

        // 检查并请求权限
        checkAndRequestPermissions()

        // 设置按钮点击事件
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台时更新状态
        updateServiceStatus()
        updatePermissionHint()
    }

    /**
     * 初始化视图组件
     */
    private fun initViews() {
        tvStatus = findViewById(R.id.tv_status)
        tvStatusHint = findViewById(R.id.tv_status_hint)
        tvPermissionHint = findViewById(R.id.tv_permission_hint)
        btnStartService = findViewById(R.id.btn_start_service)
        btnStopService = findViewById(R.id.btn_stop_service)
        btnSettings = findViewById(R.id.btn_settings)
    }

    /**
     * 设置按钮点击监听
     */
    private fun setupClickListeners() {
        // 启动监控
        btnStartService.setOnClickListener {
            if (permissionHelper.checkAllPermissions()) {
                startMonitorService()
            } else {
                showPermissionDialog()
            }
        }

        // 停止监控
        btnStopService.setOnClickListener {
            stopMonitorService()
        }

        // 进入设置
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 检查并请求必要权限
     */
    private fun checkAndRequestPermissions() {
        // 检查悬浮窗权限
        if (!permissionHelper.checkOverlayPermission()) {
            showPermissionDialog()
        }

        // 检查通知权限（Android 13+）
        if (!permissionHelper.checkNotificationPermission()) {
            permissionHelper.requestNotificationPermission()
        }
    }

    /**
     * 显示权限请求对话框
     */
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("应用需要悬浮窗权限以显示功耗信息\n\n请在系统设置中授予权限")
            .setPositiveButton("去设置") { _, _ ->
                permissionHelper.requestOverlayPermission()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 启动监控服务
     */
    private fun startMonitorService() {
        // 获取更新间隔设置
        val updateInterval = sharedPreferences.getString("update_interval", "1000")?.toLongOrNull() ?: 1000L

        val intent = Intent(this, PowerMonitorService::class.java).apply {
            action = PowerMonitorService.ACTION_START
            putExtra(PowerMonitorService.EXTRA_UPDATE_INTERVAL, updateInterval)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Toast.makeText(this, "功耗监控已启动", Toast.LENGTH_SHORT).show()

            // 延迟检查服务状态，等待服务完成启动（修复时序竞态问题）
            Handler(Looper.getMainLooper()).postDelayed({
                updateServiceStatus()
            }, 200)
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * 停止监控服务
     */
    private fun stopMonitorService() {
        val intent = Intent(this, PowerMonitorService::class.java)
        stopService(intent)

        Toast.makeText(this, "功耗监控已停止", Toast.LENGTH_SHORT).show()

        // 延迟检查服务状态，等待服务完成销毁（修复时序竞态问题）
        Handler(Looper.getMainLooper()).postDelayed({
            updateServiceStatus()
        }, 200)
    }

    /**
     * 更新服务运行状态显示
     */
    private fun updateServiceStatus() {
        val isRunning = isServiceRunning(PowerMonitorService::class.java)

        if (isRunning) {
            // 服务运行中
            tvStatus.text = "运行中"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            tvStatusHint.text = "悬浮窗正在显示功耗信息"

            btnStartService.isEnabled = false
            btnStopService.isEnabled = true
        } else {
            // 服务未运行
            tvStatus.text = "未运行"
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            tvStatusHint.text = "点击下方按钮开始监控"

            btnStartService.isEnabled = true
            btnStopService.isEnabled = false
        }
    }

    /**
     * 更新权限提示
     */
    private fun updatePermissionHint() {
        if (permissionHelper.checkAllPermissions()) {
            tvPermissionHint.text = "✅ 所有权限已授予"
            tvPermissionHint.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvPermissionHint.text = "💡 首次使用需要授予悬浮窗权限"
            tvPermissionHint.setTextColor(getColor(android.R.color.holo_orange_dark))
        }
    }

    /**
     * 检查服务是否正在运行
     * 使用静态标志位替代已弃用的 getRunningServices API
     */
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return PowerMonitorService.isServiceRunning
    }

    /**
     * 处理权限申请结果
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionHelper.REQUEST_OVERLAY_PERMISSION) {
            if (permissionHelper.checkOverlayPermission()) {
                Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
                updatePermissionHint()
            } else {
                Toast.makeText(this, "悬浮窗权限被拒绝，无法启动监控", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 处理运行时权限申请结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionHelper.REQUEST_NOTIFICATION_PERMISSION) {
            if (permissionHelper.checkNotificationPermission()) {
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "通知权限被拒绝，可能影响服务稳定性", Toast.LENGTH_LONG).show()
            }
            updatePermissionHint()
        }
    }
}