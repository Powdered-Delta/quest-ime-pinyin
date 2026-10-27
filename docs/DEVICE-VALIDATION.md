# Device validation — 2026-10-27

Target: Quest 2 at `192.168.0.234:5555`. Vector `2.2 / 3080`, API 102. Local ADB `37.0.1-15733141`. This work is not recorded in Trellis.

## Observed

- Current installed baseline is 0.10-probe: SHA-256 `7b9ffdddff21a250814726db05fed30b5ce015c03a1f087b7245128ac2e5029a`. Installed digest matches; the restarted VrShell process PID 27778 reached `ui ready: layout=7f12007e`. No screenshot was taken for 0.10 at the user's request.
- Vector module `com.m0e_n00b.questpinyinprobe` enabled via its own shipped CLI.
- Scope list contains exactly `com.oculus.vrshell`, user 0.
- VrShell was restarted. Module log confirms `hooks installed; fixed-mapping probe; host fingerprint matched` in build 0.7 PID 15974 (earlier build 0.6 PID 8755).
- The user reported being able to select Chinese and then seeing the `你好` candidate on build 0.2. This is user-observed UI evidence, not an automated assertion of commit behavior.
- The same report identified a white-on-white button theme. Build 0.3 sets explicit white candidate text and dark RGB(55,68,87) background tint and is installed with its hash checked.
- A local diagnostic Activity was added to test without writing into existing user applications. It contains no network, text history, persistence or entered-text logging.
- On build 0.3 the user explicitly confirmed: candidate is clearly visible and tapping it commits exactly once.
- At the user's request, `build/device-candidate-verified.png` was captured with the adjusted headset viewpoint and visually inspected. It shows the Chinese mode label, `nihao` preedit, and readable white `你好` on a dark button above the QWERTY rows. The test editor's upper area is outside the screenshot, so this image establishes candidate layout and contrast; the commit result is supported by the user's confirmation.
- The user clarified that build 0.4 correctly shows candidates and commits text in Meta Quest Browser's top address/search bar. Earlier failure feedback had been mixed across versions. No input-type-0 fallback was introduced; the verified URI and layout support was retained.
- The user confirmed build 0.6 underlines in both the mirrored text and original editor.
- After explicit screenshot preparation confirmation, `build/device-expanded-before.png` was captured and inspected. It proves the 0.6 expansion repeated the strip candidates and only covered a small top-left corner of the keys. A successful build had not proved its geometry.
- Build 0.7 corrects candidate partitioning using natural measured widths and explicitly sizes the scrollable expansion to the keyboard's width/height; local 99-case verification passed and the installed hash matched. After a fresh explicit readiness confirmation, `build/device-expanded-0.7.png` was captured and visually inspected: eight naturally fitted strip candidates (`你好` through `倪`), followed by an expanded region starting at `腻` with no duplicates. The overlay covers the full key area. Runtime diagnostics confirm `expanded bounds=780x290 hiddenFrom=8`.

- Build 0.8 installed and VrShell restarted: host fingerprint matched and hooks installed in PID 18857. It batches partial commits and defers synchronous selection notifications; candidate backgrounds and heights are shared across the strip and expansion. Local 103-case verification passed. The user confirmed partial selection correctly retains hao. They deferred screenshot acceptance and identified prefix matching and the left content inset as follow-up defects.

- Build 0.9 passed 130 local checks and APK packaging/signature verification. It adds prefix predictions with explicit raw consumption ranges and derives expansion left padding from the actual strip position including its host outer margin. No screenshot was taken.

- Build 0.10 removes container left margins/padding from both candidate regions while saving/restoring original host geometry. Windows packaging and 130 checks passed. SHA-256: `7b9ffdddff21a250814726db05fed30b5ce015c03a1f087b7245128ac2e5029a`. Screenshot acceptance was explicitly waived.

## Pending

- Build 0.9 prefix candidates (`h`/`ha`, `nih`/`niha`), partial consumption, and measured left-edge alignment on device. Expanded-candidate selection, scrolling, and visual style remain unverified. The user requested no screenshot for this update.
- Space/enter selection, backspace, language round-trip, focus lifecycle, protected-field behavior, and module-disable restoration on device.
- Actual hand/controller interaction and layout clipping across different panel sizes.

## Commands used

Commands ran through `metavr adb -d 192.168.0.234:5555` or `metavr capture`, except the local ADB version query.

```text
install -r <local APK>
shell su -c '/data/adb/modules/zygisk_vector/cli scope set com.m0e_n00b.questpinyinprobe com.oculus.vrshell/0'
shell su -c '/data/adb/modules/zygisk_vector/cli modules enable com.m0e_n00b.questpinyinprobe'
shell su -c '/data/adb/modules/zygisk_vector/cli scope ls com.m0e_n00b.questpinyinprobe'
shell am force-stop com.oculus.vrshell
shell am start -n com.oculus.vrshell/.HomeActivity
shell am start -n com.m0e_n00b.questpinyinprobe/.ProbeActivity
shell pm path com.m0e_n00b.questpinyinprobe | cut -d: -f2 | xargs sha256sum
shell su -c 'grep -F QuestPinyinProbe /data/adb/lspd/log/modules_*.log | tail -5'
```

Vector's runtime configuration was changed through its official CLI, not by editing its persisted database or killing its daemon. Earlier parameterless syntax probing was rejected by automatic approval review; read-only help and explicit module/package arguments resolved it without an outstanding block.

Historical screenshot paths refer to the original private workspace build directory. Screenshots, device dumps and the original VrShell APK are intentionally not included in this repository. This repository extraction does not constitute a new device acceptance run.
