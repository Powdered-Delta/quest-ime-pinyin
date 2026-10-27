# 第三方接口与实现来源

## 当前仓库

- Xposed legacy API 82：仅作为编译 stub，运行时实现由设备框架提供。下载地址为 https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar ，SHA-256 为 f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25。stub 不进入 DEX，缓存不入 Git。参考 https://api.xposed.info/ 。
- Inspired by：[Lumince/singularity](https://github.com/Lumince/singularity) 和 [Vector](https://github.com/JingMatrix/Vector)。这里只表示项目启发来源，不表示复制代码或继承其许可证。
- Vector 实测框架提交：`88f8e1faa8b4e7ce20aefabe9c295cd746ea038e`。框架代码没有随模块打包。
- Android SDK 工具及平台 jar：开发者自行安装；工具链不入库。
- 项目使用独立源码和构建脚本。候选区域布局、输入消费范围等行为设计曾参考其他输入法实现；未复制其他应用源码、系统 APK 或反编译宿主代码。
- LSPosed 模块开发流程参考本地 apk-reverse 的 references/lsposed-and-modules.md，使用 Gradle-free 构建、编译 stub 与运行时分离、精确作用域及分层设备证据。

## 当前词库及派生代码

Wolvic 基线为 37c7c28b1b269dca9356f449d798dcd1b6bcc8a8，https://github.com/Igalia/wolvic 。原工程中的 PinyinCandidates.kt / WolvicInputEngine.kt 为 MPL-2.0 派生文件。本仓库 PinyinCandidates.java / SqlitePinyinBackend.java 为基于该实现重新适配的 MPL-2.0 派生文件，完整许可和资源来源记录位于 assets/ime/notices/。

google_pinyin.db 由 Wolvic 携带，来源谱系追溯至 Android PinyinIME 和 libgooglepinyin：

- Android PinyinIME：https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/
- libgooglepinyin：https://code.google.com/archive/p/libgooglepinyin

上述上游代码采用 Apache-2.0。本仓库在 `assets/ime/notices/Apache-2.0.txt` 保留致谢协议文本；数据库文件的具体版权声明仍需按对应上游发布版本核对，不能仅凭文件名或 Wolvic 的 MPL-2.0 代码许可推定。

项目自身发布许可证尚待维护者选择；本次建仓未擅自给全部代码或未来数据添加统一许可证。
