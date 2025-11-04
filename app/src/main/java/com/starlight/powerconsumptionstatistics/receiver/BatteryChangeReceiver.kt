package com.starlight.powerconsumptionstatistics.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.starlight.powerconsumptionstatistics.model.BatteryData

/**
 * 电池状态变化广播接收器
 * 监听 ACTION_BATTERY_CHANGED 广播
 */
class BatteryChangeReceiver(
    private val onBatteryChanged: (BatteryData) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED && context != null) {
            // 注意：这里不直接解析 Intent，而是通过 BatteryInfoManager 获取最新数据
            // 因为某些信息（如实时电流）需要通过 BatteryManager API 获取
        }
    }
}
