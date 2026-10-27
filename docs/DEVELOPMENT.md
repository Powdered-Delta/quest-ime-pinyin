# 开发规范与二开指南

## 环境与构建

Windows PowerShell、JDK 17+；Android SDK 安装 platform android-35 和 build-tools 37.0.0。Java 源码按 source/target 8 编译，Android minSdk/targetSdk 29；compile API 不等于 targetSdk。

```powershell
$env:JAVA_HOME = '本机 JDK 17 安装目录'
$env:ANDROID_SDK_ROOT = '本机 Android SDK 安装目录'
.\build-windows.ps1
.\build-windows.ps1 -TestsOnly
git diff --check
```

路径为维护环境示例，按本机位置修改。脚本基于 PSScriptRoot，不依赖原工程。Xposed API 82 从官方地址下载到 deps/，摘要不符则中止。API jar 仅作编译 classpath，不作 D8 程序输入。

完整构建依次执行纯 Java 回归、javac、D8、aapt2、zipalign、apksigner，并检查 DEX 不包含 Xposed 框架类。输出 APK、SHA256SUMS.txt 及 apk-badging.txt 均在 build/。原型基线为 130 项纯 Java 检查；词库切片增加前缀／范围／失败及异步保序回归，实际数量以运行输出为准。构建产物清单 build/build-info.json 记录源码、版本和各摘要。

## 修改边界

1. 新行为先明确输入和可观察结果，读对应调用链，再一次完成会话／编辑／UI 的必要修改。
2. 纯拼音匹配和消费修改先跑 TestsOnly；UI、Hook 或资源修改完成完整切片后跑完整脚本。
3. 保留文件行尾，禁止无意全文件重写。更新 docs 中受影响契约与实际验证记录。
4. legacy API 82 是已验证兼容选择；不因框架标识 API 102 就混用 modern API。若迁移，单独完成入口、生命周期和加载验证。
5. 优先模型／方法 Hook 和自有 View，不依赖全局资源 Hook。先解析全部必要成员，失败时保留宿主行为。
6. 包名、入口、偏好名和签名分别处理；项目改名不要求同时改这些已安装兼容标识。

## 签名与本地迁移

脚本在 build/debug.keystore 创建本地调试签名。新克隆产生不同签名，不能覆盖另一签名的同包名 APK。原维护机抽离时仅在忽略的本地 build/ 保留原调试密钥以延续安装；Git 不包含密钥。发布签名及其备份由维护者单独管理，不能把私钥发给贡献者。

## 设备验证（须获得对应操作授权）

设备命令使用 Meta VR CLI，先运行 doctor 检查 platform-tools／授权状态。以下是命令模板，不能把文档示例本身当作用户授权。

```powershell
npx -y metavr doctor
npx -y metavr adb -d <device> install -r <apk>
npx -y metavr adb -d <device> shell am force-stop com.oculus.vrshell
npx -y metavr adb -d <device> shell am start -n com.oculus.vrshell/.HomeActivity
npx -y metavr adb -d <device> shell am start -n com.m0e_n00b.questimepinyin/.ProbeActivity
```

在 Vector 管理器启用模块，作用域只选 com.oculus.vrshell 对应用户，不勾选系统框架或全部包。CLI 配置必须先查该版本 help，使用模块包名与目标作用域完整参数；不直接修改框架数据库或杀守护进程。

安装后分步核对：已安装 APK 摘要 → 新进程 Hook／指纹日志 → 语言入口 → 普通字段 → 浏览器地址栏 → 部分候选 → 保护字段及恢复。不要清空设备日志，不把输入内容写入 logcat 或模块日志。

原型日志标签为 QuestPinyinProbe；Vector 框架日志可能不在 logcat，维护设备使用 /data/adb/lspd/log/modules_*.log。只读筛选标签和最近必要条目，不转储所有应用内容。异常只输出类别；需要更深诊断时添加不含输入文本的状态码。

## 恢复和问题报告

在管理器禁用模块并重启作用域进程，应回到原语言菜单及键盘。若行为异常，记录版本／摘要、字段种类、触发步骤和现象；敏感字段不要提供原输入。宿主升级不匹配时保留指纹保护，转入适配流程。

报告必须区分本地检查通过、APK 已安装、Hook 已启动、用户交互已确认。首次公开发布前再完成原生词库和多字段设备验收，不能仅凭 APK 能编译宣布通用中文输入完成。

## 真实词库自检

ProbeActivity 的“运行真实词库自检”按钮在后台验证固定词组、前缀、部分消费和分隔符，不读取／编辑当前输入框。也可在明确设备授权后，以 am start -n com.m0e_n00b.questimepinyin/.ProbeActivity --ez dictionary_self_test true 启动。Android 日志标签为 QuestPinyinDictionary，仅含检查编号／结果。该自检证明模块进程内的 SQLite 资源链；VrShell 进程中的资源读取和原系统键盘交互仍需分别核对。
