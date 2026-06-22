package com.starlight.powerconsumptionstatistics.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.starlight.powerconsumptionstatistics.model.BatteryData
import kotlin.math.absoluteValue

/**
 * 电池信息管理器
 * 负责从系统获取电池相关信息
 *
 * @param context 上下文
 * @param preferAverageCurrent 是否优先使用平均电流（默认false，优先使用实时电流）
 */
class BatteryInfoManager(
    private val context: Context,
    private val preferAverageCurrent: Boolean = false
) {

    private val batteryManager: BatteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    /**
     * 获取实时电流 (微安 μA)
     * 正值表示充电，负值表示放电
     * 注意：部分设备可能不支持此功能，返回 0
     *
     * 兼容性处理：某些设备（如三星）返回的是毫安(mA)而非微安(μA)
     * 通过数值大小判断并自动修正单位
     */
    private fun getCurrentNow(): Int {
        return try {
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // 如果电流绝对值小于10且不为0，可能是毫安单位，需要转换为微安
            if (current != 0 && current.absoluteValue < 10) {
                current * 1000  // mA -> μA
            } else {
                current
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取平均电流 (微安 μA)
     *
     * 兼容性处理：某些设备（如三星）返回的是毫安(mA)而非微安(μA)
     * 通过数值大小判断并自动修正单位
     */
    private fun getCurrentAverage(): Int {
        return try {
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            if (current != 0 && current.absoluteValue < 1000) {
                current * 1000  // mA -> μA
            } else {
                current
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 从 Intent 获取电池状态信息
     */
    private fun getBatteryIntent(): Intent? {
        return context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    /**
     * 获取电池电压 (毫伏 mV)
     *
     * 兼容性处理：优先从Intent获取，失败时尝试备用方案
     */
    private fun getVoltage(intent: Intent?): Int {
        // 优先从Intent获取电压
        val voltageFromIntent = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        if (voltageFromIntent > 0) {
            return voltageFromIntent
        }

        // 如果Intent获取失败，尝试使用系统文件读取
        // 注：BATTERY_PROPERTY_VOLTAGE 在某些Android版本可能不可用
        return try {
            // 尝试从系统文件读取（某些设备支持）
            val voltageFile = java.io.File("/sys/class/power_supply/battery/voltage_now")
            if (voltageFile.exists() && voltageFile.canRead()) {
                val voltage = voltageFile.readText().trim().toIntOrNull() ?: 0
                // 系统文件通常返回微伏(μV)，需要转换为毫伏(mV)
                if (voltage > 1000000) {
                    voltage / 1000
                } else {
                    voltage
                }
            } else {
                // 如果都无法获取，返回默认值
                100 // 默认3.7V，这是锂电池的典型电压
            }
        } catch (e: Exception) {
            // 出错时返回默认值
            200 // 默认3.7V
        }
    }

    /**
     * 获取电池温度 (0.1°C)
     * 例如：返回 280 表示 28.0°C
     */
    private fun getTemperature(intent: Intent?): Int {
        return intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    }

    /**
     * 获取电池电量百分比
     */
    private fun getBatteryLevel(intent: Intent?): Int {
        if (intent == null) return 0
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }
    }

    /**
     * 判断是否正在充电
     */
    private fun isCharging(intent: Intent?): Boolean {
        if (intent == null) return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * 计算实时功率 (mW)
     * 功率 = 电流(μA) × 电压(mV) / 1,000,000
     */
    private fun calculatePower(currentMicroAmps: Int, voltageMilliVolts: Int): Float {
        return (currentMicroAmps.toLong() * voltageMilliVolts / 1_000_000f).absoluteValue
    }

    /**
     * 获取完整的电池数据
     */
    fun getBatteryData(): BatteryData {
        val intent = getBatteryIntent()
        val instantCurrent = getCurrentNow()  // 实时电流
        val avgCurrent = getCurrentAverage()  // 平均电流
        val voltageMilliVolts = getVoltage(intent)

        // 决策逻辑：根据用户设置和实际情况选择电流类型
        val (finalCurrent, isAverage) = when {
            preferAverageCurrent -> {
                // 用户选择优先使用平均电流
                avgCurrent to true
            }
            instantCurrent != 0 -> {
                // 优先使用实时电流（如果可用）
                instantCurrent to false
            }
            else -> {
                // 实时电流不可用，回退到平均电流
                avgCurrent to true
            }
        }

        return BatteryData(
            currentMa = finalCurrent / 1000f,
            powerMw = calculatePower(finalCurrent, voltageMilliVolts),
            temperatureCelsius = getTemperature(intent) / 10f,
            voltageV = voltageMilliVolts / 1000f,
            batteryLevel = getBatteryLevel(intent),
            isCharging = isCharging(intent),
            isUsingAverageCurrent = isAverage
        )
    }

    /**
     * 检查设备是否支持电流读取
     */
    fun isCurrentSupported(): Boolean {
        return getCurrentNow() != 0 || getCurrentAverage() != 0
    }
}
