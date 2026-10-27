# Meta Quest IME Pinyin 拼音输入法

[English](README.en.md)

为 Meta Quest 系统键盘添加中文拼音输入的 Xposed 模块。项目针对 Vector（LSPosed 分支）开发，复用系统键盘的空间面板、QWERTY 按键和原输入连接，通过语言菜单提供“中文（拼音）”、离线词库候选、组合文本下划线及展开候选区。

**当前版本：0.0.1（versionCode 20）。** Android 包名为 `com.m0e_n00b.questimepinyin`。当前包含串行异步 SQLite 拼音候选后端，并修复组合输入中的部分选字、光标移动、退格、插入和候选提交行为。尚未调用 iWnn 拼音函数；目前只适配下文列出的 VrShell 版本。

## 支持范围

| 项目 | 已核对值 |
|---|---|
| 宿主包名／进程 | `com.oculus.vrshell` 主进程 |
| 宿主版本 | `207.0.0.466.436` / `1045683657` |
| 宿主 APK SHA-256 | `9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4` |
| 实测设备／框架 | Quest 2，Vector 2.2 build 3080 |
| Xposed 接口 | legacy API 82，编译期 stub |

Meta Quest Browser 顶部地址／搜索栏已通过用户交互验收。宿主版本或 APK 摘要不匹配时，模块不会安装输入行为 Hook。其他系统版本需要独立适配。

## 构建

需要 Windows、JDK 17+、Android SDK `android-35` 和 build-tools `37.0.0`。无需 Gradle。

```powershell
$env:JAVA_HOME = '安装 JDK 17 的本机路径'
$env:ANDROID_SDK_ROOT = '安装 Android SDK 的本机路径'
.\build-windows.ps1
# 仅运行纯 Java 回归测试
.\build-windows.ps1 -TestsOnly
```

完整构建会校验 Xposed API 82 stub 和内置词库的 SHA-256，输出 `build/quest-ime-pinyin.apk`、摘要文件及构建清单。脚本不会安装或启动 APK。构建签名是本机调试密钥；不得提交或分发私钥。发布构建应使用维护者单独管理的签名。

## 安装与启用

在兼容的 Xposed 环境安装模块，在 Vector 中只为 `com.oculus.vrshell` 设置作用域并启用模块，然后重启 VrShell。打开系统键盘后，在语言菜单选择“中文（拼音）”，输入 `nihao` 验证候选。包名从历史原型变更为 `com.m0e_n00b.questimepinyin`，升级时需要在 Vector 中为新包重新配置作用域。

详细流程、安全边界与故障恢复见[开发与二开指南](docs/DEVELOPMENT.md)。

## 已知问题

- 拼音组合期间将光标移到组合最左端后按退格，目前不会删除组合前的宿主字符；该问题涉及 composing 区与宿主光标边界，待明确编辑语义后单独修复。

## 文档

- [架构](docs/ARCHITECTURE.md)
- [输入行为规范](docs/INPUT-CONTRACT.md)
- [宿主适配](docs/HOST-ADAPTER.md)
- [版本控制与发布](docs/VERSION-CONTROL.md)
- [已知问题](docs/KNOWN-ISSUES.md)
- [设备验证记录](docs/DEVICE-VALIDATION.md)
- [贡献指南](CONTRIBUTING.md)
- [第三方来源与许可说明](THIRD_PARTY.md)
- [词库替换说明](docs/DICTIONARY-SWAP.md)

## 许可与词库

Inspired by [Lumince/singularity](https://github.com/Lumince/singularity) and [Vector](https://github.com/JingMatrix/Vector).

拼音候选及 SQLite 查询代码参考 [Wolvic](https://github.com/Igalia/wolvic)，固定参考提交为 `37c7c28b1b269dca9356f449d798dcd1b6bcc8a8`。对应派生代码按 **MPL-2.0** 提供，许可文本和来源记录见 [`assets/ime/notices/`](assets/ime/notices/)。

词库文件 `google_pinyin.db` 由 Wolvic 携带，来源谱系追溯至 Android 开源项目 [PinyinIME](https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/) 和 [libgooglepinyin](https://code.google.com/archive/p/libgooglepinyin)，相关上游代码采用 **Apache-2.0**。本项目保留 Apache-2.0 致谢和许可证文本；数据库文件的具体上游版权声明仍以对应发布版本为准。
