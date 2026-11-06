# CoreGate App

CoreGate 原生 Android 管理界面，作者 zimky。基于 Compose 与 Miuix，实现锁核状态、游戏策略、限频和冻结管理。具体策略由独立模块守护进程执行，关闭 App 后模块继续工作。

## 下载与源码

- APK 下载：[本仓库 Releases](https://github.com/zimky180/CoreGate-App/releases)
- 模块 ZIP 与开源仓库：[zimky180/CoreGate](https://github.com/zimky180/CoreGate)
- 模块 ZIP 下载：[模块 Releases](https://github.com/zimky180/CoreGate/releases)

Android 13（API 33）及以上，需要 root 与配套模块。当前 v2.3.0 仍以骁龙 8 Gen 5 / 一加 Ace 6T 为适配和验证重点，不宣称支持所有设备。

## v2.3.0

- **页面重构**：采用 Compose + Miuix 原生界面，重新整理锁核状态、策略、日常限频和设置页面，加入悬浮底部导航与实时核心状态展示。
- **增加主题选择**：支持浅色、深色及跟随系统，提供强调色选择和动态配色，默认使用绿色强调色。
- **增加温度锁核 / 解核**：支持「高温锁核」与「高温解封」两种模式，统一使用电池温度，阈值可在 35～60℃ 调整，默认 40℃。高温锁核可单独选择额外封锁的核心，降至阈值以下 3℃ 后撤销额外封锁、恢复日常名单；高温解封在达到阈值时恢复全部核心并解除 CoreGate 限频。
- **增加冻结管理**：支持查看应用冻结状态、执行冻结与解冻，并显示应用名称和状态差异；操作时仅当前项目显示进行中状态。
- **策略优先级完善**：识别到名单内游戏时优先恢复全部核心；已开启的录屏 / 相机恢复、充电恢复同样优先于高温额外锁核。策略由模块执行，关闭 App 后仍然生效。
- **默认配置与升级迁移**：首次安装只锁最高频的一个大核，其余可选功能默认关闭；重启保留当前设置。升级时清理已删除功能的残留配置，并保留迁移备份。
- **图标与体验修正**：应用图标更新为「八核休眠」，修正主题配色、温度阈值拖动和冻结列表刷新体验。
- **构建与更新**：正式版开启 R8 混淆、代码优化与资源压缩，修正模块更新下载链接。

## 公开构建与产物对应

本仓库公开 APK 完整项目、依赖版本、R8 规则、GitHub Actions 工作流与同次构建使用的 `module/` 快照。发布页关联源码提交与 Actions 构建，BUILD_INFO.json 记录构建来源、文件摘要及签名证书指纹，SHA256SUMS.txt 用于校验下载文件。模块 ZIP 在模块仓库发布，两个仓库的模块文件保持逐字节一致。

正式版启用 R8 混淆、代码优化和资源压缩，R8 mapping 可用于崩溃回溯。已发布 APK 的程序与资源条目与对应 CI 输出逐字节核对，APK v2 签名及内容摘要校验通过；不同环境重建不保证 APK 字节完全相同。模块数据与 APK 分开保存。

本机执行 Kotlin 语法解析、Shell 语法及策略模拟检查，正式编译由公开 Actions 完成；新增温度策略仍需真机安装回归验证。

## 构建

JDK 17、Gradle 9.3.1、Android SDK 37，执行 `gradle assembleDebug assembleRelease`。Actions 提供 APK、模块 ZIP、构建信息和 R8 mapping 工件。

## 第三方说明

界面使用 Compose、Miuix 与 Material Kolor。液态效果文件保留 AndroidLiquidGlass / compose-miuix-ui 的上游来源注释；相应第三方代码遵循其各自许可证。自有代码使用 MIT 许可证，详见 LICENSE 与 THIRD_PARTY_NOTICES.md。
