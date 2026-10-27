# Quest IME Pinyin

为 Meta Quest 系统键盘添加中文拼音输入的 Xposed 模块，当前在 Vector（LSPosed 分支）上开发与验证。

项目保留系统键盘的空间面板、QWERTY 按键及原输入连接，向语言菜单增加 **中文（拼音）**，提供中文候选、组合文本下划线及覆盖按键区域的展开候选区。

**当前开发版本为 0.11.0-alpha.1（versionCode 11）。** 已接入离线 SQLite 拼音词库及串行异步候选后端，固定 `ni/hao` 会话仅保留作回归。尚未调用 iWnn 拼音函数；仍只支持下表固定宿主，设备验收情况见验证记录。

## 当前能力与验证边界

- Meta Quest Browser 顶部地址／搜索栏：用户已确认中文候选和上屏正常。
- 组合文本：用户确认原输入框及键盘顶部镜像均有下划线。
- 分段选字：0.8 用户确认选择单字后保留 `hao`，可继续选字。
- 顶部候选按实际词宽动态容纳前 X 项；展开区只显示余下候选，覆盖整个按键区。
- 0.9 增加 `h` / `ha` 及 `nih` / `niha` 前缀匹配；0.10 移除两个候选区域的容器左边距。相关本地检查通过，最新视觉复验由用户明确跳过。
- 密码、邮箱、数字等受保护或非文本字段保留原系统行为。

当前只适配这一份 VrShell：

| 项目 | 已核对值 |
|---|---|
| 包名／进程 | `com.oculus.vrshell` 主进程 |
| 版本 | `207.0.0.466.436` / `1045683657` |
| APK SHA-256 | `9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4` |
| 实测设备／框架 | Quest 2，Vector 2.2 build 3080 |
| 模块接口 | legacy Xposed API 82，compile-only |

宿主版本或摘要不匹配时不安装行为 Hook。其他系统版本需要独立适配，不能直接删除指纹保护。

## Windows 构建

需要 JDK 17+、Android SDK platform `android-35` 和 build-tools `37.0.0`，无需 Gradle 或 ViriViri 工程。

```powershell
.\build-windows.ps1 -JdkHome 'C:\path\to\jdk-17' -SdkRoot 'C:\path\to\Android\Sdk'
# 仅运行纯 Java 回归检查
.\build-windows.ps1 -JdkHome 'C:\path\to\jdk-17' -TestsOnly
```

首次完整构建会下载并校验官方 Xposed API 82 stub；内置词库也会校验源文件及 APK 中的摘要。输出为 `build/quest-pinyin-probe.apk`，沿用历史文件名。同时输出 `build/build-info.json`，包含源码提交、脏状态、安装版本、APK／词库／签名证书及宿主摘要。脚本仅构建，不安装、不启用模块、不操作设备。

当前 Android 包名 `com.m0e_n00b.questpinyinprobe`、入口类和偏好名保留，用于兼容已安装原型；本次独立建仓没有更改这些标识。模块显示名称已更新为 Quest IME Pinyin；入口类、包名和偏好名保持兼容。

`build/`、`deps/` 和签名密钥均不进入 Git。新克隆会生成自己的本地调试签名；同包名覆盖安装必须使用原安装版本对应的签名。不得将维护者密钥提交或随贡献包分发。

## 使用与调试

在已具备兼容 Xposed 环境的设备安装模块后，在管理器中仅将作用域设为 `com.oculus.vrshell`，启用并重启对应进程。打开系统键盘，语言菜单选择 **中文（拼音）**，用 `nihao` 测试候选。具体设备命令、恢复方法及证据要求见 [二开指南](docs/DEVELOPMENT.md)。

## 文档入口

| 文档 | 内容 |
|---|---|
| [版本控制](docs/VERSION-CONTROL.md) | 分支、版本、基线、发布标签与产物记录 |
| [架构](docs/ARCHITECTURE.md) | Hook → 中文会话 → 编辑事务 → 候选 UI |
| [输入行为规范](docs/INPUT-CONTRACT.md) | 前缀、候选消费、按键、焦点及几何约束 |
| [开发规范与二开](docs/DEVELOPMENT.md) | Windows 构建、签名、日志、设备验证及恢复 |
| [宿主适配](docs/HOST-ADAPTER.md) | 版本指纹、混淆符号及资源边界 |
| [真实词库下一阶段](docs/DICTIONARY-PLAN.md) | Wolvic 词库、异步接入和繁体扩展 |
| [独立建仓验证](docs/EXTRACTION-VERIFICATION.md) | 独立构建、源码完整性和产物摘要 |
| [词库后端验证](docs/SQLITE-VALIDATION.md) | 当前 alpha 构建与真实 SQLite 自检证据 |
| [设备验证](docs/DEVICE-VALIDATION.md) | 观察记录与尚待验证项 |
| [实现沿革](docs/IMPLEMENTATION-HISTORY.md) | 原型迭代、行为及构建记录 |
| [贡献指南](CONTRIBUTING.md) | 提交范围与验证要求 |
| [第三方来源](THIRD_PARTY.md) | API、参考实现及未来资源来源 |

仓库包含固定来源和摘要的原始 SQLite 词库及来源记录，不包含原系统 APK、反编译宿主代码、屏幕截图或 native 引擎。词库独立数据许可仍需在公开分发前核定；当前没有发布或推送远程。项目不使用 Trellis。
