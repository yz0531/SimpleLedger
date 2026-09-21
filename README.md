# 简账

一款面向个人使用的简洁 Android 记账应用。项目使用 Kotlin 与 Jetpack Compose 构建，最低支持 Android 8.0（API 26）。

## 功能

- 纯支出模式，金额以正数展示，可填写分类、日期与备注
- 首页月份卡片支持左右滑动切换月份，点击可直接选择年份和月份
- 浏览、编辑和删除历史账单
- 每日、每周、每两周、每月周期记账，可暂停或恢复规则
- 年度月度支出柱状图统计
- 多套图片皮肤与适配的文字、图标配色
- 通过系统文件选择器导出和导入 JSON 备份
- 导出带 UTF-8 BOM 的 CSV，方便使用 Excel 查看
- 坚果云 WebDAV 手动备份与恢复
- 进入应用时按数据变化自动备份，云端自动备份仅保留最近 3 份
- 坚果云应用密码通过 Android Keystore 加密保存，不写入源码
- Room 本地数据库，离线记账不依赖网络
- Material 3 界面及深色模式支持

## 技术栈

- Kotlin / Compose Compiler plugin 2.1.21
- Jetpack Compose + Material 3（Compose BOM 2025.05.01）
- Navigation Compose 2.9.0
- Lifecycle 2.9.0
- Room 2.7.1 + KSP 2.1.21-2.0.1
- kotlinx.serialization 1.8.1
- Android Gradle Plugin 8.10.1 / Gradle 8.14.3
- compileSdk / targetSdk 35，minSdk 26，Java 字节码目标 17

## 使用 Android Studio 构建

1. 安装 Android Studio，并在 SDK Manager 中安装 Android SDK 35。
2. 使用 Android Studio 打开本目录，使用其内置 JDK，或选择 JDK 17–24 作为 Gradle JDK。
3. 等待 Gradle Sync 完成。
4. 连接 Android 8.0 以上设备或创建模拟器，然后运行 `app` 配置。

也可以在命令行构建调试包：

```bash
./gradlew assembleDebug
```

调试 APK 会生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 使用说明

- 首页可左右滑动月份卡片切换月份，也可点击月份卡片选择指定年月。
- 点击“记一笔”新增记录；点击已有流水可编辑或删除。
- “周期”中可创建、暂停和恢复自动记账规则。
- “统计”中可查看指定年份每个月的支出柱状图。
- “导入 / 导出”中，JSON 用于完整备份与恢复，CSV 仅用于查看和分析。
- JSON 导入按记录 ID 合并：新 ID 新增，相同 ID 使用备份内容更新；校验失败时不会写入部分数据。
- “设置”中可切换图片皮肤，并配置坚果云账号和第三方应用密码。
- 单个 JSON 备份最多 10 MB、50,000 条记录，金额统一按人民币分存储，避免浮点误差。

## 数据说明

账单默认保存在设备本地。配置坚果云后，备份文件保存到 `/SimpleLedger/backup/`；自动备份只会在数据发生变化时上传，并维护最近 3 份。Git 仓库不包含账号、应用密码、本机 SDK 配置、数据库或测试导出文件。
