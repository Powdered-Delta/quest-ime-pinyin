# Implementation history — Quest IME Pinyin

A standalone Vector/LSPosed module for the fixed-mapping system-keyboard integration test. It adds **中文（拼音）** to the existing language menu and uses the existing English QWERTY keys. The test fixtures cover `nihao`, `ni` and `hao`; this is not a Chinese dictionary engine and does not use iWnn yet.

Current UI acceptance: candidate strip contains candidates only, left-to-right (`你好`, `你`, `呢`, `泥`, `尼` for the sample), with a downward expansion control. No mode badge or repeated Pinyin in that strip. The current composing range is underlined in the mirrored text; the original editor receives styled composing text where supported. Choosing a partial `ni` candidate preserves the remaining `hao`. Browser compatibility remains part of the same patch.

Expanded-candidate acceptance: measure each candidate at natural width, fit the first X in the available strip width (reserve the expand button only on overflow), and show only subsequent choices in the expansion. Use the measured keyboard width and height for the entire expansion background and hit area. The `ni` and `hao` test fixtures each include at least twenty choices. Before every future headset screenshot, ask in a preparation dialog and wait for the user's explicit “准备好了” response; do not reuse an earlier confirmation for another capture.

The reference is the existing `SearchCandidateStrip` two-pass measurement and `CinemaInputConsole` hidden-candidate overlay in `spatial-workbench-compose/.../SearchComponents.kt`, with the Web prototype under `docs/prototypes/workbench/index.html` and the prior input-method PRDs consulted read-only. These references establish natural-width overflow, remainder-only expansion, bounded keyboard coverage, and vertical scrolling without moving the surrounding layout.

## Supported host

- Package: `com.oculus.vrshell`, main process only.
- versionName: `207.0.0.466.436`; versionCode: `1045683657`.
- SHA-256: `9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4`.
- Device framework observed read-only: Vector v2.2, build `3080-88f8e1fa-JingMatrix-Vector`, Quest 2.
- Legacy Xposed API 82, compile-only. No resource hooks, target APK replacement, root filesystem overlay, or root-level code execution from the module.

Fingerprint or adapter-shape mismatch leaves host behavior unchanged. This module is intentionally tied to this exact system build; do not remove the gate to use it on another version.

## Build on Windows

From this directory:

```powershell
.\build-windows.ps1
```

The script runs JVM tests, compiles the module, packages it with the Android build tools, and produces a locally debug-signed APK at `build/quest-pinyin-probe.apk`. It never installs or enables it. Build dependencies and signing material stay under ignored `deps/` and `build/`. Xposed classes must not be bundled in the APK.

The independent module does not change the root Gradle graph or the ViriViri application. Its Windows build is its applicable verification command; building the unrelated ViriViri APK would not verify these hooks.

## Implementation

`MainHook` checks the package, process, version and APK hash before installing hooks. `HostAdapter` resolves the expected host members before changing behavior. It adds a native key model in the vacant bottom-left area of the existing language selector, preserving the original language choices and cancel button.

Selecting Chinese runs the host's normal English-layout transition while persisting a separate module-owned Chinese flag in the host's private `quest_pinyin_probe` preferences. The host's language resolver is not given an invented enum or unknown locale. Switching to an existing language clears that flag. Reopening the keyboard retains the selected mode but does not retain composition across input sessions.

In Chinese mode, a candidate strip is inserted into the existing `SuggestionPanelView` region. Its controls do not take editor focus. The original suggestion children are restored when Chinese mode is inactive, unavailable for a field, or the probe fails. Native keyboard geometry and scene entities are not authored or replaced.

Text handling is intercepted before the original Latin buffer/autocorrection path, with a lower dispatch hook for direct commands. Recognized events have one owner. `TEXT` and `COMMAND` use the host actions; composing text updates the pinned host's composing counters and uses its existing input connection once, with an `UnderlineSpan`. The host's mirrored EditText receives a separate module-owned underline only for the exact active composition before the caret. Candidate taps are checked against the composition generation and input connection to reject stale taps. Client editors may discard styling, so the original-field underline is not guaranteed across applications.

The module handles ordinary Android text fields and URI fields (including browser address/search bars) with a live input connection. Its supported layout list includes English text, simple English, the browser address bar and generic URI QWERTY layouts in the pinned host. Password, visible-password, web-password, numeric, phone, date and email fields use the original input behavior. Native/Unity input paths without a normal `EditorInfo` and `InputConnection` are not claimed as supported by this prototype. No entered text, composition or candidates are logged.

Queued selection updates for older composing lengths are suppressed only while awaiting an acknowledgement. A one-second watchdog bounds this wait: if the editor cannot acknowledge composition, the visible Latin preedit is finished and that input connection returns to original behavior until a new input session. This timeout is a provisional fallback, not a measured latency guarantee.

## Local verification status

The final Windows build passed 76 fixed-session assertions, 27 field/layout-policy cases, 12 editor-action/batch/selection regressions, 7 composing-range cases and 8 natural-width fitting cases (130 total). Java compilation, D8, resource/manifest linking, APK signature verification, alignment and absence of bundled Xposed framework definitions passed. The regression checks include natural-width overflow with conditional arrow reservation, partial candidate consumption, allowing browser URI input while preserving password handling, rejecting non-text layouts, marking only the active occurrence before the caret, and preventing empty-composition backspace from deleting an editor selection twice.

Current artifact version: `0.10-probe` (versionCode 10). SHA-256: `7b9ffdddff21a250814726db05fed30b5ce015c03a1f087b7245128ac2e5029a`. The matching digest is also written to `build/SHA256SUMS.txt` and was verified against the installed APK on `192.168.0.234:5555`.

Under explicit user authorization the module was installed and enabled through Vector's own CLI, scoped only to `com.oculus.vrshell/0`; VrShell was restarted. Vector's module log confirms fingerprint match and successful Hook installation in the new process. The user confirmed selecting Chinese and seeing `你好`; a white-on-white candidate theme issue was fixed with an explicit dark background and white text in 0.3. On the updated build the user confirmed a clear candidate and exactly-once commit. A requested headset screenshot was captured and inspected: Chinese mode, the `nihao` composition and the readable dark-background `你好` candidate are visible above QWERTY. See [DEVICE-VALIDATION.md](DEVICE-VALIDATION.md) for the evidence boundary.

The user confirmed build 0.4 supports candidates and correct commit in Meta Quest Browser's address/search bar, and confirmed 0.6 underlines in both mirror and original editor. Build 0.6 expansion was visually incorrect: it repeated strip entries and only covered a small corner. Build 0.7 measures natural candidate widths, shows only the fitting prefix, and expands the hidden remainder in a vertically scrollable panel with the keyboard's explicit measured bounds. The `ni` and `hao` fixtures each have twenty choices; `nihao` includes `你好` plus the twenty `ni` choices. Selecting a partial `ni` candidate leaves `hao` composing. The inspected 0.7 screenshot confirms full key-area coverage and disjoint candidate regions: eight entries fit the strip, the expansion starts at `腻`, and runtime bounds are `780x290` (`hiddenFrom=8`). Expanded-candidate interaction and scrolling still require device acceptance. Build 0.5 briefly added content-free UI-gate diagnostics; transient `inputType=0` logs were not a browser failure, as later `ui ready: layout=7f120000` and user feedback confirmed.

Build 0.8 uses the same explicit flat candidate background, press ripple, font and 48dp row height in both strip and expansion. Partial candidate commits batch the committed prefix and remaining composition; selection notifications received during the batch are deferred until the final edit state, preventing an intermediate commit acknowledgement from ending the remaining preedit. JVM editor-range tests cover `nihao` → `尼` plus composing `hao` at range 1–4 → `尼好`. Installed APK digest and successful Hook startup were verified. The user confirmed that build 0.8 correctly retains hao after choosing a single character. Visual acceptance was deferred by the user.

Build 0.9 follows the prefix and raw-consumption contract from `input-method-engine/PinyinCandidates.kt` and the existing QWERTY input PRD: incomplete input retains candidates, each fixture candidate stores its raw consumed end, and selection removes only that prefix plus adjacent separators. In this limited fixture, `h`/`ha` predict hao choices; `nih`/`niha` offer 你好 and ni choices; selecting 尼 from `nih` leaves h with hao candidates. The expansion left inset is derived from the measured strip content edge relative to the keyboard, including the host SuggestionPanelView outer margin, rather than equalizing inner padding alone. This remains a ni/hao probe, not the full production lexicon. The user requested no screenshot during this update.

Build 0.10 removes the Chinese suggestion container outer left/start margin and left padding, along with the custom strip and expansion left padding. The original host layout parameters and padding are saved and restored on language/session cleanup or failure. Candidate button content padding is retained. Windows build and 130 checks passed; screenshot verification was explicitly waived for this revision.

## Manual device acceptance (not performed by the build)

Device mutations require explicit user authorization. Authorization for the current device run was granted; do not assume it applies to unrelated future deployments. Scope only to `com.oculus.vrshell`; do not select all packages or system framework.

1. Install the built module, enable it in Vector, scope it to VrShell, and restart the scoped process through an approved device action. Confirm the module log says the host fingerprint matched and hooks were installed.
2. In an ordinary text field open the system keyboard and its language menu. Confirm there is a separate **中文（拼音）** entry and all original language options and cancel remain usable.
3. Choose Chinese; confirm QWERTY. Type `nihao`; confirm underlined composition and candidate **你好**, with no language label or duplicate Pinyin in the candidate strip. Tap it; the field must contain exactly one **你好**, without residual Latin text.
4. Repeat using space and enter to choose the candidate. Check backspace during composition, unmatched Latin input, and repeated candidate taps. Confirm a tap from an old composition cannot commit after focus changes.
5. Switch to English and type normally; switch to another original language as a regression check. Reopen the keyboard and check mode persistence and cleared composition.
6. Check password, numeric and email fields continue to use original behavior. Verify Chinese input in the browser address/search bar and ordinary webpage text fields separately.
7. Disable the module and restart the scoped process; confirm the original language menu and original keyboard return. Module preference data is inert when the module is disabled.

Only the actual device test can verify ray/hand interaction, panel clipping, candidate touch routing, and editor compatibility. A successful local build does not certify those outcomes.

## Sources and maintenance

The target is locally analyzed; raw evidence and `FEASIBILITY.zh-CN.md` are under `D:/Workspace/unity/viriviri/artifacts/system-ime-192.168.0.234-20261027/analysis/` (original private analysis workspace; not distributed here). Decompiled Java is navigation material; load-bearing field/method names and input events were also checked against DEX instruction exports.

- [Vector at the observed framework commit](https://github.com/JingMatrix/Vector/tree/88f8e1faa8b4e7ce20aefabe9c295cd746ea038e)
- [Official Xposed API](https://api.xposed.info/reference/de/robv/android/xposed/XposedBridge.html)
- [Modern API changes, including removal of resource hooks](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API)

This work is intentionally maintained outside Trellis at the user's request. No generated APK, original system APK, dependency JAR or signing key should be committed as source.
