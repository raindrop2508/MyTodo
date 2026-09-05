# MyPotato — Agent 指南

> 给 AI / 协作者的项目入口。详细设计见 `docs/`；本文件只保留执行时最需要的约定。

## 项目是什么

- **名称：** MyPotato（包名 `com.gordon.mypotato`）
- **类型：** Android 原生番茄钟 TODO，本地离线优先
- **能力：** 四象限任务管理 + 长时任务番茄钟（专注/短休/长休）+ Room 持久化
- **当前阶段：** Stage A–C 完成；Stage D 进行中（会话落库与活动计时已完成，**统计页仍为 Mock**）

## 技术栈（速查）

| 项 | 值 |
|----|-----|
| 语言 | Kotlin |
| UI | ViewBinding + XML + Material 3 |
| 架构 | 单 Activity + 多 Fragment + MVVM |
| 数据 | Room（`potato_db` v3）+ Repository 接口 + `Room*Repository` |
| 异步 | Coroutines / Flow / StateFlow |
| minSdk / targetSdk | 29 / 36 |
| DI | 无框架；`ViewModelFactory.getInstance(context)` |

## 代码分层

```
ui/          → 只做展示与交互，不直接访问 DAO
viewmodel/   → 持有 StateFlow，调用 Repository，承载页面规则
domain/      → 纯模型与纯逻辑（优先可单测，如 PomodoroTimerLogic）
data/        → Entity / DAO / Mapper / RoomRepository / Initializer
```

**禁止：** UI 绕过 Repository 写库；在 ViewModel 外散落业务写路径；重新引入 FakeRepository 作为默认运行时实现（除非明确用于测试）。

## 关键业务规则

1. **仅长时任务**可进番茄钟；详情页与 `PomodoroViewModel` 双重校验。
2. 番茄钟：**先落库再计时**；全库活动会话（`IN_PROGRESS` + `PAUSED`）≤ 1。
3. 冷启动孤儿会话由 `OrphanPomodoroSettlement` + `MainActivity` 弹窗收尾；**不**静默恢复倒计时。
4. 统计口径（已定、待接页）：只统计 `phase == FOCUS` 且 `status == COMPLETED`。
5. 开发期 Room 使用 `fallbackToDestructiveMigration`；正式 Migration 发布前再补。

## 编码规范

- 优先匹配现有文件风格（命名、缩进、ViewBinding 模式）。
- Fragment：`_binding` 可空 + `onDestroyView()` 置空。
- 颜色走主题属性 `?attr/...`，避免布局硬编码色值。
- 用户可见文案进 `strings.xml`，避免硬编码中文到 Kotlin（设置 Toast 占位等历史例外除外）。
- 改动保持最小范围；不做无关重构；不擅自扩大 Scope。
- 新增文档优先放 `docs/plan/stageX/`，并更新 `docs/README.md` 权威矩阵。
- 路径在文档中用仓库相对路径（`app/src/...`、`docs/...`）。

## 目录与文档入口

| 用途 | 路径 |
|------|------|
| 文档索引 | `docs/README.md` |
| 架构总览 | `docs/项目技术架构与实现说明.md` |
| 术语 | `docs/glossary.md` |
| 总规划 | `docs/plan/项目整体规划文档.md` |
| Room 总结 | `docs/plan/stageC/StageC_Room数据库实现总结.md` |
| Stage D 计划 | `docs/plan/stageD/StageD_番茄钟会话落库与统计闭环计划.md` |
| 活动计时落地方案 | `docs/plan/stageD/StageD_番茄钟计时持久化_实际落地方案.md`（Issue #14） |
| 活动计时摘要 | `docs/plan/stageE/番茄钟活动计时持久化.md` |

## 改代码前建议

1. 读本文件 + `docs/README.md` 权威矩阵中对应文档。
2. 确认任务属于哪一层（UI / VM / Domain / Data），避免跨层泄漏。
3. 若动番茄钟或会话表结构，同步核对 `PomodoroSession` Entity/Domain/Mapper 与 DB version。
4. 能单测的纯逻辑放 `domain/`，并补充/更新 `app/src/test`。

## 明确不做（除非用户要求）

- 云同步、账号体系
- 杀进程后静默续跑番茄钟 / 自动跳转未完成番茄钟页
- Foreground Service / AlarmManager 到点通知（当前未规划落地）
- 未要求时不创建 git commit、不改 git config、不强推
