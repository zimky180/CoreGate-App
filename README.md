# CoreGate App

CoreGate 原生 Android 管理界面，作者 zimky。基于 Compose 与 Miuix，实现锁核状态、游戏策略、限频和冻结管理。具体策略由独立模块守护进程执行，关闭 App 后模块继续工作。

## 下载与源码

- 正式版与 debug APK：[本仓库 Releases](https://github.com/zimky180/CoreGate-App/releases)
- 模块 ZIP 与开源仓库：[zimky180/CoreGate](https://github.com/zimky180/CoreGate)
- 模块 ZIP 下载：[模块 Releases](https://github.com/zimky180/CoreGate/releases)

Android 13（API 33）及以上，需要 root 与配套模块。当前 v2.3.0 仍以骁龙 8 Gen 5 / 一加 Ace 6T 为适配和验证重点，不宣称支持所有设备。

## v2.3.0

初始配置仅锁最高性能的一个核心，其他可选功能默认关闭；升级及重启保留用户设置。温度统一采用电池温度，高温锁核使用独立额外核心名单，游戏名单匹配时跳过，降至阈值以下 3℃后恢复日常名单。支持冻结状态差异提示、默认绿色强调色。图标保留当前版本。

## 公开构建与产物对应

本仓库公开 APK 完整项目、依赖版本、R8 规则、GitHub Actions 工作流与同次构建使用的 `module/` 快照。每次发布关联源码标签与提交；Actions 从该提交构建 debug、release 和模块 ZIP，生成 SHA256SUMS、构建信息及签名证书指纹。debug APK 直接来自该次 Actions 输出；正式版基于同次未签名 APK，由维护者签名后上传，签名前后校验值分别记录；模块 ZIP 在模块仓库发布，两个仓库的模块文件保持逐字节一致。

正式版启用 R8 混淆、代码优化和资源压缩；debug 不混淆并保留调试能力。Actions 输出未签名正式版，维护者自行签名后上传；私钥不进入本仓库或 Actions。debug 使用 Android 调试签名，两种包名相同但签名不同，不能互相覆盖安装；此前自行改签名的 APK 亦须使用对应签名或卸载后安装。模块的数据与 APK 分开保存。

可在 Actions 下载 R8 mapping 工件辅助复查，发布页列出源码提交、构建链接与校验值。二进制不会与源文件具有相同字节；校验用于确认下载附件与该次构建输出完全一致，不承诺不同环境重建的 APK 字节完全相同。

本机仅执行 Kotlin 语法解析、Shell 语法及策略模拟检查，正式编译由公开 Actions 完成；发布前未完成新增温度策略的真机安装回归。

## 构建

JDK 17、Gradle 9.3.1、Android SDK 37，执行 `gradle assembleDebug assembleRelease`。release 输出未签名 APK，由维护者自行签名。工作流不创建签名密钥，也不自动发布未签名 APK。

## 第三方说明

界面使用 Compose、Miuix 与 Material Kolor。液态效果文件保留 AndroidLiquidGlass / compose-miuix-ui 的上游来源注释；相应第三方代码遵循其各自许可证。自有代码使用 MIT 许可证，详见 LICENSE 与 THIRD_PARTY_NOTICES.md。
