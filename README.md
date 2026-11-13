# CoreGate · 核心门控

**v2.6.4 / 264** · Android 原生 Root 管理 App 与配套锁核模块 · 作者 zimky

日常锁核与限频，检测游戏自动恢复；支持冻结管理、电池温度策略、录屏/相机恢复和游戏保持锁核。

## 下载与仓库

- [App 正式发布与完整源码](https://github.com/zimky180/CoreGate-App)
- [模块发布与完整源码](https://github.com/zimky180/CoreGate)
- [APK 同步源码与构建仓库](https://github.com/zimky180/CoreGate-APK)
- [下载 v2.6.4 App](https://github.com/zimky180/CoreGate-App/releases/tag/v2.6.4)
- [下载 v2.6.4 模块](https://github.com/zimky180/CoreGate/releases/tag/v2.6.4)

App 的自动更新入口为 **CoreGate-App**；模块更新入口为 **CoreGate**。校验文件为各 Release 的 SHA256SUMS.txt。

## 默认行为与兼容性

- 首次安装只锁最高频的一个大核，cpu0 不下线；限频、温度及其他可选策略默认关闭。
- 重启/升级保留已保存设置。当前样例配置的日常轮询为 10 秒、游戏轮询为 30 秒；历史版本默认值不同，以当前配置为准。
- 温度策略使用电池温度，默认阈值 40℃，可设 35～60℃。
- 需要 Root；App 最低 Android 13。硬件驱动、系统限制及其他性能工具可能影响实际锁核和频率行为。
- 限频恢复优先使用首次快照；快照可能记录其他工具已经限制的频率。本版不含逐档试写能力。

## 安装与恢复

1. 从 Releases 下载模块 ZIP，在模块管理器安装并按提示重启。
2. 从 App 发布页下载 APK 安装。
3. 正常升级保留用户配置。模块不需要 App 常驻；App 提供配置、状态和操作入口。
4. 出现异常：管理器禁用模块，或创建 `/data/adb/modules/CoreGate/disable` 后重启。
5. 无法启动：Recovery 删除 `/data/adb/modules/CoreGate`；彻底重置时再删除 `/data/adb/CoreGate` 后重启。

运行时修改 sysfs，不写系统分区。禁用/卸载并重启可撤销运行时修改。

## 本次修复

- 修复限频配置解析与缓存，避免读取错误后静默回退默认值。
- 修复游戏保持锁核目标名单切换、核心恢复顺序与操作失败反馈。
- 检测范围切换改为可滑动悬浮胶囊。
- 贡献者资料更正为 Dere3046（酷安 UID 37601237）。
- 生命周期标记清理、崩溃自愈与 SAFE_MODE；限频实现沿用 v2.4.2 原版。

完整累计更新请见 [v2.6.4 Release](https://github.com/zimky180/CoreGate-App/releases/tag/v2.6.4) 与 [RELEASE-NOTES.md](RELEASE-NOTES.md)。

## 源码结构

| 路径 | 内容 |
| --- | --- |
| `app/` | 原生 Android App，Compose / Miuix 界面、配置与 Root 桥接、完整资源 |
| `module/` | 可独立运行的模块脚本、配置、库与预编译观察程序 |
| `module/daemon/` | Rust 观察程序源码；迁移第一阶段 |
| `module/tools/` | 模块校验与打包脚本 |
| `.github/workflows/build.yml` | debug/release APK 构建及配套模块打包 |
| `LICENSES/`、`THIRD_PARTY_NOTICES.md` | 第三方许可与声明 |

仓库包含发布所用完整工程文件与资源，保留源码注释。凭据、私钥、机器专用 local.properties 及构建缓存不属于发布源码。

## 构建

使用 JDK 17、Android SDK Platform 37 和 Gradle 9.3.1；依赖版本见 `gradle/libs.versions.toml`。若本机没有 Gradle Wrapper，请安装对应 Gradle。

```sh
gradle --no-daemon assembleDebug assembleRelease
cd module
python3 tools/package_module.py --source . --output ../out
```

GitHub Actions 可在 Actions 页面手动运行；ci-build 分支推送也会触发。

## 贡献者

zimky、寒玉、热情、峩、冬安、沧海、解开超载的心、doze、Dere3046、晨阳、节甘天baby、巴闭Z、DeepSeek。完整展示与个人链接以 App 内贡献者页面为准。

## 许可

项目许可见 [LICENSE](LICENSE)，第三方组件与资源遵守各自声明，见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 和 [LICENSES](LICENSES)。

更新日期：2026-11-13。
