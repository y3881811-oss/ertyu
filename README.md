
# ertyu

一个开源、可扩展的 Android 文件管理器，提供双面板文件管理、多存储引擎切换、压缩包处理与 APK 工具链，代码按功能模块拆分，便于维护和二次开发。

> 当前处于积极开发阶段，功能和接口可能随版本调整。

---

## 功能特性

### 文件管理
- **双面板**：横屏/平板自动双面板，竖屏单面板，可手动切换
- **多标签页**：左右面板各自维护独立标签
- **视图切换**：列表视图 / 网格视图
- **排序与过滤**：按名称、大小、时间排序，可显示隐藏文件
- **路径记忆**：每个面板按目录记住滚动位置，返回时自动恢复

### 存储引擎（可切换）
- **普通文件引擎（推荐）**：基于 SAF，无额外权限，最稳定
- **Shizuku 引擎**：免 Root 访问受限目录（Android 11+），需安装并启动 Shizuku
- **Root 引擎**：完全控制文件系统

### 文件操作
- 复制、移动、重命名、删除、新建文件夹
- 压缩、解压（支持常见压缩格式）
- 批量操作：全选、反选、按类型选择
- 文件对比（选中 2 项时启用）
- **删除二次确认**：确认框内可勾选「移入回收站」
- **回收站**：删除的文件可恢复，可在高级设置中关闭

### 选择模式
- 普通模式**长按**文件 → 进入选择模式并选中该项
- 选择模式下**单击** → 切换选中
- 选择模式下**长按已选中文件** → 弹出操作菜单
- 选择模式下长按未选中文件 → 仅加选，不弹菜单
- 操作菜单随选中数量变化：
  - 选中 1 项：打开、重命名、属性、解压、压缩、分享、安装、签名
  - 选中 N 项：批量重命名、压缩、复制、移动、删除、全选、反选

### APK 工具
- APK 安装、查看、签名、克隆
- 分卷 APK（`.apks` / `.xapk` / `.apkm`）安装
- APK 浏览与十六进制查看
- 校验和计算

### 媒体与预览
- 视频播放、音频播放
- 图片画廊
- SQLite 数据库查看
- 字体预览

### 其他
- WebDAV 共享
- 高级设置：引擎切换、面板模式、默认排序、网格/列表、回收站开关、WebDAV 配置、自定义路径

---

## 截图

> 截图占位，后续补充。

| 双面板 | 选择模式 | 高级设置 |
|---|---|---|
| _待补充_ | _待补充_ | _待补充_ |

---

## 构建

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.x（使用项目自带 wrapper）

### 命令行构建

```bash
# Debug
./gradlew assembleDebug

# Release（需配置签名）
./gradlew assembleRelease
```

产物位于 app/build/outputs/apk/。

签名配置

Release 构建需要在 app/build.gradle.kts 或 local.properties 中配置签名信息，请勿将 keystore 提交到仓库。

---

项目结构

```
app/src/main/java/com/ertyu/
├── MainActivity.kt              # 入口
├── MainScreen.kt                # 主界面布局
├── MainViewModel*.kt            # ViewModel 按功能拆分（文件操作、选择、压缩、对比等）
├── FilePanel.kt                 # 文件面板
├── FilePanelRow.kt              # 列表项、网格项、选择工具栏
├── FileToolbar.kt               # 顶部工具栏
├── FileActionDock.kt            # 底部操作栏
├── FileActionDialog.kt          # 单文件操作对话框
├── SettingsAdvanced.kt          # 高级设置
├── ui/
│   └── SelectionActionSheet.kt  # 选择模式长按操作菜单
├── engine/                      # 存储引擎（SAF / Shizuku / Root）、回收站
├── model/                       # 数据模型（FileItem、PanelState、AppSettings 等）
└── ...
```

---

技术栈

· 语言：Kotlin
· UI：Jetpack Compose + Material 3
· 架构：ViewModel + StateFlow，按功能拆分为多个扩展文件
· 存储：SAF / Shizuku / Root 三级可切换
· 压缩：Apache Commons Compress / zip4j
· APK：smali/baksmali、apksig、REAndroid APKEditor
· 异步：Kotlin Coroutines + Flow

---

路线图

☑ Phase 1：双面板基础文件管理
☑ Phase 2：选择模式、批量操作、回收站、压缩包
☑ Phase 3：Shizuku 引擎接入
☐ Phase 3：APK 工具链完善（签名、克隆、浏览）
☐ Phase 4：WebDAV、插件机制、主题

---

贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 新建分支：git checkout -b feature/your-feature
3. 提交改动：git commit -m "feat: add your feature"
4. 推送分支：git push origin feature/your-feature
5. 提交 Pull Request

提交前请确保 ./gradlew assembleDebug 能通过。

---

许可证

本项目采用 GPL-3.0 许可证，详见 LICENSE。

GPL-3.0 具有传染性：基于本项目的衍生作品需以相同许可证开源。如需闭源或商业授权，请联系维护者。

---

联系

· 仓库：https://github.com/y3881811-oss/ertyu
· Issue：https://github.com/y3881811-oss/ertyu/issues

