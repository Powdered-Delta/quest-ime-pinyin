# 独立建仓验证 — 2026-10-27

- 本仓库包含独立源码、纯 Java 测试和 Windows 构建脚本，不依赖其他应用的构建图。
- 历史参考实现不属于本仓库；本项目保留必要的来源与许可记录，不复制宿主 APK 或反编译宿主代码。
- src / tests / assets / res 共 15 个文件与抽离源逐字节一致；Android 包名及版本保留 0.10-probe / versionCode 10。
- 从新仓库目录执行完整 build-windows.ps1，使用 JDK 17、android-35、build-tools 37.0.0，退出码 0。
- 130 项纯 Java 检查通过；Java / D8 / aapt2 / 签名及对齐检查通过，DEX 未打包 Xposed 框架实现。
- 新目录构建 APK：build/quest-pinyin-probe.apk，SHA-256：95a2d2f7e3c09fb8ea95ae6e65928e3aa17cb455f1f5841c3ad5395d976c884b。
- 该 APK 是本地重新打包产物；历史设备安装的 0.10 摘要仍为 7b9ffdddff21a250814726db05fed30b5ce015c03a1f087b7245128ac2e5029a。构建包含打包时间等元数据，不宣称可逐字节重现。
- 新产物没有重新安装或做设备截图；本次仅验证建仓、独立构建与文档。
- Markdown 本地链接检查通过；build/debug.keystore 与 deps/api-82.jar 确认被 Git 忽略。
- 调试密钥仅为维护机忽略目录中的本地延续，不进入 Git 或源码分发。
- README、架构、输入契约、Hook 适配、二开、词库计划、贡献和来源记录已独立整理，不依赖原项目文档才能理解当前实现。
- 尚未接入真实词库，尚未指定项目发布许可证，也未创建或推送远程仓库。
