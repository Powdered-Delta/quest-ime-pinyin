# 更换拼音词库

本项目把词库访问隔离在 `PinyinBackend`。替换词库时，通常只需要新增一个后端实现、替换资源和调整 `HostAdapter` 的构造位置，不需要修改宿主 Hook、键盘布局或候选面板。

## 后端契约

实现 `src/com/m0e_n00b/questimepinyin/PinyinBackend.java`：

```java
public interface PinyinBackend {
    List<PinyinCandidate> evaluate(String raw);
    default void close() { }
}
```

`evaluate` 只在串行 worker 调用。每个 `PinyinCandidate` 必须提供 `text`、原始拼音字符坐标中的 `consumedEnd`，以及稳定的候选 `id`。候选必须支持不完整前缀和部分消费；例如选择 `nihao` 中的 `尼` 后，应留下 `hao` 继续查询，不能按汉字显示长度截断原始拼音。

## 替换步骤

1. 新增后端，例如 `RimePinyinBackend` 或其他 SQLite 后端，实现 `PinyinBackend`。
2. 在 `HostAdapter` 创建 `PinyinSession` 的位置替换后端构造；保留 `AsyncPinyinSession` 的串行调用和失效结果检查。
3. 把词库放入 APK 资源，或由后端按自己的方式加载。SQLite 后端应在 worker 初始化、查询和关闭，不能在 UI 线程访问数据库。
4. 删除当前后端专用的摘要、表结构和资源校验；为新资源增加对应的完整性检查。
5. 为前缀、完整词组、部分消费、分隔符、空候选、初始化失败和关闭流程补充测试。
6. 运行 Windows 回归检查：

   ```powershell
   .\build-windows.ps1 -TestsOnly
   .\build-windows.ps1
   ```

## Rime 适配提示

Rime 通常需要词库、方案配置和 `librime` native 库，不是简单替换一个 `.db` 文件。建议让 `RimePinyinBackend` 把 Rime 的候选结果转换为本项目的 `PinyinCandidate`，并明确每个候选实际消费的拼音范围。

## 许可证

词库、方案配置、native 引擎和转换工具分别核对许可证。当前 Wolvic 派生的查询代码按 MPL-2.0 记录；Google Pinyin 上游代码的致谢按 Apache-2.0 记录，但这不代表 Rime 词库或其他新资源也采用 Apache-2.0。替换资源时应在 `THIRD_PARTY.md` 和 `assets/ime/notices/` 增加来源、版本、摘要和完整许可文本。
