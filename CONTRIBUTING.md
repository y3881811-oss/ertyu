# 贡献指南 (Contributing)

感谢你对 ertyu 的关注！如果你想为这个项目做出贡献，请遵循以下流程：

## 提交 Issue
- 在提交新 Issue 前，请先搜索是否已经存在相关问题。
- 请尽量使用仓库提供的 Issue 模板，详细描述 Bug 的复现步骤或功能需求。

## 提交 Pull Request (PR)
1. Fork 本仓库并克隆到本地。
2. 基于 `main` 分支创建一个新的分支：`git checkout -b feature/your-feature` 或 `fix/your-bug`。
3. 在本地开发并测试（确保 `assembleDebug` 能通过）。
4. 提交代码：请遵循 **Conventional Commits** 规范，例如：
   - `feat: 增加跨标签页复制功能`
   - `fix: 修复解压带密码的压缩包时闪退的问题`
5. 推送分支并提交 Pull Request。PR 会自动触发 GitHub Actions 构建检查，请确保通过。

## 代码规范
- 使用 Kotlin 官方代码风格（4 个空格缩进）。
- UI 组件尽量使用 Jetpack Compose。
- 新功能请尽量附带注释。