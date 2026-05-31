# Project Context
Last updated: 2026-06-01 00:24

## Overview
- **Name**: OneAPIAndroidRebuild
- **Stack**: Native Android Java app, Gradle Android plugin 8.7.3, minSdk 26, targetSdk 35
- **Run**: `gradle.bat assembleRelease` from this folder
- **Purpose**: Android companion app for OneAPI chat, image, Codex, and Claude remote workflows.

## Architecture
- `app/src/main/java/center/oneapi/mobile/` - native Android activity code.
- `app/src/main/res/` - launcher, navigation, drawer, and tool assets.
- `docs/pc-linkage/` - Android and desktop linkage design documents.
- `public/` - source icon assets used to generate Android drawables.

## Key Files
| File | Purpose |
|------|---------|
| `app/src/main/java/center/oneapi/mobile/MainActivity.java` | Main Android UI, networking, chat rendering, remote execution log rendering |
| `app/build.gradle` | Android build config, release signing, arm64-only APK split |
| `app/src/main/AndroidManifest.xml` | App manifest and activity configuration |
| `public/` | Canonical image assets for Android UI icons |

## Current State
- Last worked on: Android subscription wallet purchase flow, in-app Android update download/install flow, settings module ordering/button sizing, and Markdown table horizontal scrolling/cell width behavior.
- Release build verified with `gradle.bat assembleRelease`; APK output remains arm64-only.

## Recent Commits
```
e8267ca -Feature：设备互联  日志同步  AI和image功能接入
0e58ac2 -featire：更新界面、优化功能
4a11cbb -feature：接入部分功能
a0bfab6 -Feature：初版上传
c971b6f Initial commit
```

## Known Issues / Notes
- Keep changes scoped to Android mobile app unless the user explicitly requests desktop or server changes.
- Release APK is configured for `arm64-v8a` only.
