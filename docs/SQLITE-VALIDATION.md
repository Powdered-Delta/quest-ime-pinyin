# SQLite 后端验证 — 2026-10-27

## 源码与构建

- 版本规范提交：832fdfd；main 保持该集成基线。
- 原型基线标签：baseline/probe-0.10，指向 fffc16f，不是发布标签。
- 词库功能提交：8f9870f，分支 codex/sqlite-pinyin-backend。
- 开发版本 0.11.0-alpha.1 / versionCode 11，未创建发布标签。
- Windows 完整构建通过，164 项纯 Java 检查通过（异步 11、宽度 8、组合范围 7、编辑 12、fixture 76、字段 27、候选后端 23）。
- APK SHA-256：e0f1504477d7c0034de117a7a3e619956348bd5746ea68bda9629be1e58f3a6a。
- build/build-info.json 记录干净源码提交、版本、签名、宿主和词库摘要。
- 内置数据库与 APK 内资源摘要均为 920f621c690355d33ed6b9732bb7fa3e297b36c379d6b352ef3643be43b83635。
- 产物检查曾发现 Windows aapt2 嵌套 assets 的反斜杠名称；已在签名前标准化，检查最终 ZIP 名称无反斜杠并验证精确路径。

## 设备证据

- 指定设备 192.168.0.234:5555；ADB 37.0.1-15733141。
- 安装成功，已安装 APK 摘要与上面产物匹配。
- VrShell 新进程 PID 8630：sqlite pinyin backend / host fingerprint matched；原文本布局达到 ui ready。
- 显式启动 ProbeActivity 的固定词库自检，模块进程 PID 8948、worker 9041。
- 日志确认数据库按固定摘要打开，self-test passed; checks=9; UI-thread guard verified。
- 9 项实际 Android SQLite 固定检查覆盖 nihao、woshi、shurufa、h、nih、选择尼后保留 h、继续选择好和 xi'an 的边界；另验证主线程查询保护。日志不包含用户输入。
- 自检只读取固定词组，不读取或改写用户文本。它证明模块进程中的真实词库／SQLite 链，不能替代宿主进程中的键盘交互验收。
- 本轮未截图。

## 待用户交互确认

- 原系统键盘中 woshi / shurufa 的候选及上屏。
- nihao 选择尼后继续选择好；h / nih 的候选行为。
- Meta Quest Browser 地址栏、快速输入／退格、字段切换、语言切换及保护字段回归。
- 繁体方案未实现，数据独立公开分发许可仍待核定；未公开推送或发布。
