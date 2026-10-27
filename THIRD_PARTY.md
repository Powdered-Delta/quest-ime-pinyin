# 第三方接口与实现来源

## 当前仓库

- Xposed legacy API 82：仅作为编译 stub，运行时实现由设备框架提供。下载地址为 https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar ，SHA-256 为 f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25。stub 不进入 DEX，缓存不入 Git。参考 https://api.xposed.info/ 。
- Vector：实测框架提交 88f8e1faa8b4e7ce20aefabe9c295cd746ea038e，参考 https://github.com/JingMatrix/Vector/tree/88f8e1faa8b4e7ce20aefabe9c295cd746ea038e 。框架代码没有随模块打包。
- Android SDK 工具及平台 jar：开发者自行安装；工具链不入库。
- 原型源码从 ViriViri 私有工作区 tools/quest-pinyin-probe 抽离。自然宽度两次拟合、隐藏余项展开及消费范围契约参考其既有输入法实现与文档；原应用、系统 APK、反编译宿主代码未复制。
- LSPosed 模块开发流程参考本地 apk-reverse 的 references/lsposed-and-modules.md，使用 Gradle-free 构建、编译 stub 与运行时分离、精确作用域及分层设备证据。

## 当前词库及派生代码

Wolvic 基线为 37c7c28b1b269dca9356f449d798dcd1b6bcc8a8，https://github.com/Igalia/wolvic 。原工程中的 PinyinCandidates.kt / WolvicInputEngine.kt 为 MPL-2.0 派生文件。本仓库 PinyinCandidates.java / SqlitePinyinBackend.java 为基于该实现重新适配的 MPL-2.0 派生文件，完整许可和资源来源记录位于 assets/ime/notices/。

google_pinyin.db 的来源、大小和摘要见 docs/DICTIONARY-PLAN.md。数据库许可应独立核对，不能仅因同目录源码是 MPL-2.0 就推定数据许可。本地功能分支已携带未经修改的该数据库用于接入验证；没有携带 OpenWnn AAR/native 库。独立数据许可仍须在公开分发前确认，本次没有公开分发。

项目自身发布许可证尚待维护者选择；本次建仓未擅自给全部代码或未来数据添加统一许可证。
