package com.starlight.powerconsumptionstatistics.model

/**
 * 电池数据模型
 *
 * @property currentMa 电流 (mA)，正值表示充电，负值表示放电
 * @property powerMw 功率 (mW)
 * @property temperatureCelsius 温度 (°C)
 * @property voltageV 电压 (V)
 * @property batteryLevel 电量百分比 (0-100)
 * @property isCharging 是否正在充电
 * @property isUsingAverageCurrent 是否使用平均电流（false表示实时电流）
 */
data class BatteryData(
    val currentMa: Float = 0f,
    val powerMw: Float = 0f,
    val temperatureCelsius: Float = 0f,
    val voltageV: Float = 0f,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val isUsingAverageCurrent: Boolean = false
) {
    /**
     * 格式化显示文本
     */
    fun getFormattedCurrent(): String = String.format("%.1f mA", currentMa)
    fun getFormattedPower(): String = String.format("%.1f mW", powerMw)
    fun getFormattedTemperature(): String = String.format("%.1f°C", temperatureCelsius)
    fun getFormattedVoltage(): String = String.format("%.2f V", voltageV)
    fun getFormattedBatteryLevel(): String = "$batteryLevel%"

    /**
     * 获取电流类型标签
     */
    fun getCurrentTypeLabel(): String = if (isUsingAverageCurrent) "平均" else "实时"

    /**
     * 判断是否有效数据
     */
    fun isValid(): Boolean = voltageV > 0
}
