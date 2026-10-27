# Quest IME Pinyin

[简体中文](README.md)

Inspired by [Lumince/singularity](https://github.com/Lumince/singularity) and [Vector](https://github.com/JingMatrix/Vector).

An Xposed module that adds Chinese Pinyin input to the Meta Quest system keyboard. It is tested with Vector (the LSPosed fork).

It reuses the system keyboard panel, QWERTY keys, and editor connection, and adds a “Chinese (Pinyin)” language option with offline candidates, composing-text underline, and an expanded candidate area.

**Version: 0.0.1 (versionCode 20).** The module includes an offline SQLite Pinyin backend and does not call iWnn Pinyin functions. It currently supports only the VrShell build listed below.

## Verified behavior and scope

- Meta Quest Browser's top address/search field shows and commits Chinese candidates.
- Composing text is underlined in the mirrored text and original editor where supported.
- Partial candidate selection keeps the remaining Pinyin composition.
- Protected fields such as passwords, email, and numeric inputs retain host behavior.

Only this VrShell build is supported:

| Item | Verified value |
|---|---|
| Package / process | `com.oculus.vrshell`, main process |
| Version | `207.0.0.466.436` / `1045683657` |
| APK SHA-256 | `9c2f1c503031f3adb21964463c8c89c13b790b45c5375ccbd3ed01c5b5c17ed4` |
| Device / framework | Quest 2, Vector 2.2 build 3080 |
| Xposed API | Legacy API 82, compile-time stub only |

The module installs no input hooks when the host version or APK digest does not match. Other system versions require separate adaptation.

## Build on Windows

Requires JDK 17+, Android SDK platform `android-35`, and build-tools `37.0.0`.

```powershell
$env:JAVA_HOME = '<JDK 17 installation directory>'
$env:ANDROID_SDK_ROOT = '<Android SDK installation directory>'
.\build-windows.ps1
# Run only the plain Java regression tests
.\build-windows.ps1 -TestsOnly
```

The full build downloads and verifies the official Xposed API 82 stub and checks the bundled dictionary checksum. It writes `build/quest-ime-pinyin.apk` and `build/build-info.json`; it does not install, enable, or launch the module.

The Android package name is `com.m0e_n00b.questimepinyin`.

Build outputs, dependencies, and signing keys are excluded from Git. A fresh checkout creates a local debug key; APKs signed with different keys cannot update one another. Never commit or distribute private signing keys.

## Installation

Install the module in a compatible Xposed environment, set `com.oculus.vrshell` as its only scope in Vector, enable the module, and restart VrShell. Select “Chinese (Pinyin)” from the keyboard language menu and type `nihao` to test candidates. See the [development guide](docs/DEVELOPMENT.md) for recovery and diagnostics.

## Documentation

| Document | Description |
|---|---|
| [Version policy](docs/VERSION-CONTROL.md) | Branches, versions, tags, and release records |
| [Architecture](docs/ARCHITECTURE.md) | Hook, Pinyin session, editor transaction, and candidate UI |
| [Input contract](docs/INPUT-CONTRACT.md) | Prefixes, candidate consumption, keys, focus, and layout |
| [Development guide](docs/DEVELOPMENT.md) | Windows build, signing, logs, device validation, and recovery |
| [Host adapter](docs/HOST-ADAPTER.md) | Host fingerprints, obfuscated symbols, and resource boundaries |
| [Dictionary plan](docs/DICTIONARY-PLAN.md) | SQLite backend and Traditional Chinese extension direction |
| [Device validation](docs/DEVICE-VALIDATION.md) | Observations and remaining checks |
| [Known issues](docs/KNOWN-ISSUES.md) | Known behavior limits |
| [Dictionary replacement](docs/DICTIONARY-SWAP.md) | Replacing the backend with Rime or another dictionary |
| [Contributing](CONTRIBUTING.md) | Contribution and verification requirements |
| [Third-party sources](THIRD_PARTY.md) | Dependencies, references, and source notices |

## Known issue

If the cursor is moved to the beginning of an active Pinyin composition, Backspace currently does not delete the host character immediately before the composition. See [known issues](docs/KNOWN-ISSUES.md).

## References and licenses

Inspired by [Lumince/singularity](https://github.com/Lumince/singularity) and [Vector](https://github.com/JingMatrix/Vector).

The Pinyin candidate lookup and SQLite query code are adapted from [Wolvic](https://github.com/Igalia/wolvic), pinned at commit `37c7c28b1b269dca9356f449d798dcd1b6bcc8a8`. Those derived source files are covered by **MPL-2.0**; license text and provenance are in [`assets/ime/notices/`](assets/ime/notices/).

The `google_pinyin.db` file is carried by Wolvic and its lineage is traced to Android [PinyinIME](https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/) and [libgooglepinyin](https://code.google.com/archive/p/libgooglepinyin), whose upstream code uses **Apache-2.0**. The Apache-2.0 text is included for attribution; the database's exact upstream copyright notice remains subject to verification against the matching release.
