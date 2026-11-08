# 纯黑媒体管理器 2026.11.08.210636

Android 15（SDK 35）媒体管理项目，Kotlin + Jetpack Compose，当前构建目标仅为 `arm64-v8a`。

## 功能
- 纯黑界面；顶部预留行，第二行刷新/新旧排序/日期或大小排序，第三行标签。
- 内置“全部、图片、视频、最近删除”，支持新增和删除自定义标签。
- 长按媒体进入多选，点击其他媒体可增减选择；双指缩放网格列数（4～10列）。
- 多选后可批量添加自定义标签，或移入“最近删除”。移入时会清除该媒体的其他自定义标签。
- 点击媒体通过 Android `ACTION_VIEW` 打开系统可处理该 MIME 类型的查看器。

## 最近删除说明
本版本的“最近删除”是应用内标签分类，不会物理删除文件，也不会调用 Android 系统回收站 API。文件仍保留在 MediaStore 中。系统回收站需要通过 `MediaStore.createTrashRequest` 发起用户确认流程，尚未接入。

## 构建
需要 JDK 17、Android SDK 35 和 Gradle 8.9：
```bash
gradle :app:assembleDebug
```
GitHub Actions 支持 push 到 `main`/`master` 或手动触发，产物为 arm64-v8a APK。

## 本次构建标识
- applicationId：`com.chme.pureblackmedia.v20261108210636`
- versionName：`2026.11.08.210636`
- versionCode：`2026110821`
- ABI：`arm64-v8a`

当前提交为源码工程；Actions 尚未确认运行，因此 APK 尚未编译验证。
