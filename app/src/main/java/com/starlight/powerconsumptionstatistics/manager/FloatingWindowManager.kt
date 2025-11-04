package com.starlight.powerconsumptionstatistics.manager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.starlight.powerconsumptionstatistics.R
import com.starlight.powerconsumptionstatistics.model.BatteryData

/**
 * 悬浮窗管理器
 * 负责悬浮窗的显示、隐藏、拖动和数据更新
 */
class FloatingWindowManager(context: Context) {

    // 使用 applicationContext 避免持有 Service 引用导致内存泄漏
    private val appContext: Context = context.applicationContext

    private val windowManager: WindowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // 拖动相关变量
    private var isDragging = false  // 拖动状态标志
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // UI 组件
    private var tvCurrent: TextView? = null
    private var tvCurrentType: TextView? = null
    private var tvPower: TextView? = null
    private var tvTemperature: TextView? = null
    private var tvVoltage: TextView? = null

    /**
     * 显示悬浮窗
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (floatingView != null) {
            return // 已经显示，避免重复创建
        }

        // 创建悬浮窗视图
        floatingView = LayoutInflater.from(appContext).inflate(
            R.layout.floating_window_layout,
            null
        )

        // 初始化 UI 组件引用
        floatingView?.let { view ->
            tvCurrent = view.findViewById(R.id.tv_current)
            tvCurrentType = view.findViewById(R.id.tv_current_type)
            tvPower = view.findViewById(R.id.tv_power)
            tvTemperature = view.findViewById(R.id.tv_temperature)
            tvVoltage = view.findViewById(R.id.tv_voltage)
        }

        // 配置窗口参数
        layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            // Android 8.0+ 必须使用 TYPE_APPLICATION_OVERLAY
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 窗口标志
            // FLAG_NOT_FOCUSABLE: 不获取焦点，避免影响其他应用
            // FLAG_LAYOUT_NO_LIMITS: 允许窗口超出屏幕边界
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // 透明背景
            format = PixelFormat.TRANSLUCENT

            // 初始位置（屏幕右上角）
            gravity = Gravity.TOP or Gravity.START
            x = getScreenWidth() - 250  // 距离右边约 50dp
            y = 100  // 距离顶部 100px
        }

        // 添加触摸监听实现拖动
        setupTouchListener()

        // 添加到窗口
        try {
            windowManager.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置触摸监听，实现拖动功能
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 开始拖动，设置标志位
                    isDragging = true
                    // 记录初始位置
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // 更新悬浮窗位置
                    layoutParams?.let { params ->
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        try {
                            windowManager.updateViewLayout(floatingView, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 拖动结束，重置标志位
                    isDragging = false
                    false
                }

                else -> false
            }
        }
    }

    /**
     * 更新显示的电池数据
     */
    fun updateData(data: BatteryData) {
        // 拖动时跳过更新，避免与拖动操作冲突导致卡顿
        if (isDragging) return

        floatingView?.post {
            tvCurrent?.text = data.getFormattedCurrent()
            tvCurrentType?.text = "[${data.getCurrentTypeLabel()}]"
            // 根据电流类型设置不同的颜色
            tvCurrentType?.setTextColor(
                if (data.isUsingAverageCurrent) {
                    0xFF2196F3.toInt() // 蓝色 - 平均电流
                } else {
                    0xFF4CAF50.toInt() // 绿色 - 实时电流
                }
            )
            tvPower?.text = data.getFormattedPower()
            tvTemperature?.text = data.getFormattedTemperature()
            tvVoltage?.text = data.getFormattedVoltage()
        }
    }

    /**
     * 隐藏悬浮窗
     */
    fun hide() {
        floatingView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
            tvCurrent = null
            tvCurrentType = null
            tvPower = null
            tvTemperature = null
            tvVoltage = null
        }
    }

    /**
     * 检查悬浮窗是否正在显示
     */
    fun isShowing(): Boolean = floatingView != null

    /**
     * 获取屏幕宽度
     */
    private fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = appContext.resources.displayMetrics
            displayMetrics.widthPixels
        }
    }
}
