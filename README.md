# PowerConsumptionStatistics - 功耗统计

Android 原生应用，通过悬浮窗实时显示手机功耗信息（电流、功率、温度、电压）。

## 功能特点

- **实时监控**：实时显示电池电流、功率、温度和电压数据
- **悬浮窗显示**：可拖动悬浮窗，在使用其他应用时持续监控
- **智能电流切换**：自动检测设备支持情况，在实时电流和平均电流间智能切换
- **自定义设置**：可调整更新频率（0.5s/1s/2s），选择电流类型
- **颜色标识**：绿色标识实时电流，蓝色标识平均电流，一目了然

## 系统要求

- **Android 版本**：8.0 (API 26) 及以上
- **目标版本**：Android 14 (API 36)
- **所需权限**：
  - `悬浮窗权限`：显示悬浮窗（需在系统设置中授权）
  - `通知权限`：发送前台服务通知（Android 13+ 需要）
  - `前台服务权限`：保持后台运行

## 安装使用

### 安装步骤

1. 下载 APK 文件
2. 允许安装未知来源应用
3. 安装完成后打开应用

### 权限设置

1. **悬浮窗权限**（必需）
   - 首次启动时会提示授权
   - 点击"去设置"跳转到系统设置
   - 找到本应用并开启"允许显示在其他应用上层"

2. **通知权限**（Android 13+）
   - 应用会自动请求
   - 允许发送通知以保持服务运行

### 基本使用

1. 打开应用，点击"启动监控"按钮
2. 授予必要权限
3. 悬浮窗会显示在屏幕上，显示实时功耗数据
4. 长按拖动悬浮窗到合适位置
5. 点击"设置"可调整更新频率和电流类型
6. 点击"停止监控"关闭悬浮窗

## 技术信息

### 开发环境

- **开发语言**：Kotlin 2.0.21
- **构建工具**：Gradle 8.13.0 (Kotlin DSL)
- **JVM 版本**：Java 11
- **主要依赖**：
  - AndroidX Core KTX 1.10.1
  - Material Design 1.10.0
  - ConstraintLayout 2.1.4

### 项目结构

```
app/src/main/java/com/starlight/powerconsumptionstatistics/
├── MainActivity.kt              # 主界面
├── SettingsActivity.kt         # 设置页面
├── model/
│   └── BatteryData.kt         # 电池数据模型
├── manager/
│   ├── BatteryInfoManager.kt  # 电池信息管理
│   └── FloatingWindowManager.kt # 悬浮窗管理
├── service/
│   └── PowerMonitorService.kt # 前台服务
└── utils/
    └── PermissionHelper.kt    # 权限辅助类
```

## 开发构建

### 环境要求

- Android Studio Ladybug 或更高版本
- Android SDK 36
- Kotlin 2.0+

### 构建命令

```bash
# 构建调试版本
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 安装到设备
./gradlew installDebug
```

## 注意事项

1. **设备兼容性**：部分设备不支持实时电流（CURRENT_NOW），会自动切换到平均电流（CURRENT_AVERAGE）
2. **设置生效**：修改设置后需要重启服务才能生效
3. **电池数据单位**：
   - 电流：微安(μA) → 毫安(mA)
   - 电压：毫伏(mV) → 伏特(V)
   - 温度：0.1°C 单位（如 280 = 28.0°C）
