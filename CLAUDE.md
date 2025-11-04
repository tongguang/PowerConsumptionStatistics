# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Android 原生应用，通过悬浮窗实时显示手机功耗（电流、功率、温度、电压），支持实时/平均电流切换。

- **包名**: `com.starlight.powerconsumptionstatistics`
- **语言**: Kotlin 2.0.21
- **最低支持版本**: Android 8.0 (API 26)
- **目标版本**: Android 14+ (API 36)
- **核心功能**: 前台服务 + 可拖动悬浮窗 + BatteryManager API

## 常用开发命令

### 构建
```bash
./gradlew assembleDebug      # 构建 Debug 版本
./gradlew assembleRelease    # 构建 Release 版本
./gradlew clean              # 清理构建产物
./gradlew build              # 完整构建
```

### 运行
```bash
./gradlew installDebug       # 安装到设备/模拟器
# 启动应用
adb shell am start -n com.starlight.powerconsumptionstatistics/.MainActivity
```

### 代码检查
```bash
./gradlew lint           # 运行 Lint 检查
./gradlew lintDebug      # 生成 Lint 报告
```

## 技术栈

- **构建系统**: Gradle 8.13.0 (Kotlin DSL)
- **JVM**: Java 11
- **核心库**:
  - AndroidX Core KTX 1.10.1
  - AndroidX AppCompat 1.6.1
  - Material Design 1.10.0
  - ConstraintLayout 2.1.4
  - CardView 1.0.0（悬浮窗和卡片布局）
  - Preference KTX 1.2.1（设置页面）

## 代码架构

### 项目结构
```
app/
├── src/main/
│   ├── java/com/starlight/powerconsumptionstatistics/
│   │   ├── MainActivity.kt              # 主界面和服务控制
│   │   ├── SettingsActivity.kt          # 设置页面
│   │   ├── model/
│   │   │   └── BatteryData.kt          # 电池数据模型
│   │   ├── manager/
│   │   │   ├── BatteryInfoManager.kt   # 电池信息管理器
│   │   │   └── FloatingWindowManager.kt # 悬浮窗管理器
│   │   ├── service/
│   │   │   └── PowerMonitorService.kt  # 前台服务
│   │   ├── receiver/
│   │   │   └── BatteryChangeReceiver.kt # 广播接收器
│   │   └── utils/
│   │       └── PermissionHelper.kt      # 权限辅助类
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml          # 主界面布局
│   │   │   ├── activity_settings.xml      # 设置页面布局
│   │   │   └── floating_window_layout.xml # 悬浮窗布局
│   │   ├── values/
│   │   │   └── strings.xml                # 字符串资源
│   │   ├── xml/
│   │   │   └── preferences.xml            # 设置项定义
│   │   └── drawable/
│   │       └── ic_notification.xml        # 通知图标
│   └── AndroidManifest.xml                # 应用清单（权限、服务声明）
```

### 架构说明

典型的分层架构：数据层（BatteryData）→ 管理层（BatteryInfoManager、FloatingWindowManager）→ 服务层（PowerMonitorService）→ 界面层（MainActivity、SettingsActivity）。

**关键点**：
- BatteryInfoManager 封装电池信息获取和实时/平均电流智能选择
- FloatingWindowManager 负责悬浮窗的显示、拖动和数据更新
- PowerMonitorService 作为前台服务协调各组件，定时更新数据
- 使用 SharedPreferences 存储用户设置（更新频率、电流类型）

## 关键设计决策

### 电流获取策略
- 实时电流（`CURRENT_NOW`）：瞬时波动大，部分设备不支持
- 平均电流（`CURRENT_AVERAGE`）：平滑稳定，兼容性好
- 智能回退：优先使用实时电流，不可用时自动回退到平均电流
- 悬浮窗用颜色区分：绿色 `[实时]` / 蓝色 `[平均]`

### 服务初始化
⚠️ **重要**：`BatteryInfoManager` 必须在 `PowerMonitorService.onStartCommand()` 中创建，而不是 `onCreate()`，因为需要读取最新的用户设置（SharedPreferences）。

### 设置生效方式
用户修改设置（更新频率、电流类型）后，需要**停止并重新启动服务**才能生效。服务不会自动重新加载设置。

### 权限要求
- `SYSTEM_ALERT_WINDOW`：悬浮窗（需要引导用户到系统设置）
- `FOREGROUND_SERVICE_SPECIAL_USE`：特殊用途前台服务（Android 14+）
- `POST_NOTIFICATIONS`：通知（Android 13+ 需动态申请）

## 开发注意事项

1. **电流数据兼容性**：部分设备不支持实时电流读取会返回 0，这是正常现象，已实现自动回退到平均电流
2. **BatteryInfoManager 初始化时机**：必须在 `onStartCommand()` 中创建以读取最新设置，而不是 `onCreate()`
3. **设置生效机制**：修改设置后需重启服务才生效，服务运行时不会自动重新加载 SharedPreferences
4. **数据单位转换**：电流(μA→mA ÷1000)、电压(μV→V ÷1000000)、温度(0.1°C 单位，280表示28.0°C)
5. **悬浮窗颜色标识**：实时电流绿色 `#4CAF50`，平均电流蓝色 `#2196F3`
