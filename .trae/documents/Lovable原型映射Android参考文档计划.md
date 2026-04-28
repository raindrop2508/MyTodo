# 计划：Lovable 原型映射 Android 参考文档

## 1. Summary（目标摘要）

- 目标：基于 `e:\code\MyPotato\mypotato-focus-flow-main` 现有 Lovable Web 原型代码，产出 1 份面向 Android Todo App MVP 的参考文档（Markdown）。
- 产物位置：`e:\code\MyPotato\docs\ui-skeleton-android`
- 产物用途：为后续 Android（Material 3 + Navigation + Fragment/Compose）开发提供可直接复用的页面逻辑与文件映射依据。
- 核心内容范围：
  - UI 页面设计逻辑（页面定位、关键模块、关键交互、数据来源）
  - 各页面对应文件位置（路由入口、页面文件、关键组件、状态与模型）
  - Android 落地建议（MVP 阶段最小可行映射）

## 2. Current State Analysis（现状分析）

### 2.1 路由与页面结构

- 路由入口：`src/App.tsx`
- 主容器：`src/components/layout/AppShell.tsx`（含页面切换动画、主题/语言应用）
- 底部导航：`src/components/layout/BottomNav.tsx`
- 页面路由：
  - `/` -> `src/pages/Index.tsx`（Today）
  - `/tasks` -> `src/pages/Tasks.tsx`
  - `/stats` -> `src/pages/Stats.tsx`
  - `/settings` -> `src/pages/Settings.tsx`
  - `/task/:id` -> `src/pages/TaskDetail.tsx`

### 2.2 核心业务与数据层

- 全局状态：`src/store/useApp.ts`（Zustand + persist）
- 核心模型：`src/types/index.ts`（Task、TaskStep、Category、PomodoroSession、Settings）
- 国际化：`src/lib/i18n.ts`（`en/zh` 文案）
- 工具函数：
  - 时间与日期：`src/lib/format.ts`
  - 导入导出：`src/lib/io.ts`

### 2.3 关键交互组件

- 任务卡片：`src/components/TaskCard.tsx`（右滑完成、点击进详情、进度展示）
- 任务编辑：`src/components/TaskEditor.tsx`（新增/编辑统一弹层）
- 番茄钟：`src/components/Pomodoro.tsx`（work/break 计时并落会话）

### 2.4 现有 Android 设计文档基础

- 已存在文档：`e:\code\MyPotato\docs\ui-skeleton-android\page_design.md`
- 该文档偏视觉与 Android UI 规范，尚缺少“从 Lovable 代码到页面逻辑/文件映射”的源码级索引。

## 3. Proposed Changes（拟实施变更）

### 3.1 新增文档文件

- 文件：`e:\code\MyPotato\docs\ui-skeleton-android\lovable_ui_logic_file_map.md`
- 变更类型：新增 Markdown（不修改现有业务代码）
- 原因：集中沉淀 Web 原型 -> Android MVP 的“逻辑与文件映射”，供后续 AI/开发者检索与实现。

### 3.2 文档章节设计（what / why / how）

1. 文档说明与适用范围
- What：说明文档目的、读者对象、与 `page_design.md` 的关系（互补）
- Why：避免后续 AI 只拿视觉规范，缺失业务逻辑路径
- How：给出“先读视觉规范，再读逻辑映射”的使用方式

2. 全局架构总览（Web 原型）
- What：应用壳层、路由树、导航、状态管理、模型定义
- Why：让 Android 端先建立完整心智模型
- How：用“模块 -> 职责 -> 源文件”表格展示

3. 页面级设计逻辑（逐页）
- What：Today / Tasks / Stats / Settings / TaskDetail
- Why：每个页面都要明确“展示什么、如何交互、依赖哪些数据”
- How：每页固定模板：
  - 页面定位
  - 关键 UI 区块
  - 核心交互流程
  - 状态读写点（`useApp` 的哪些字段/方法）
  - 关键源文件清单

4. 组件交互与数据流
- What：TaskCard、TaskEditor、Pomodoro 与页面之间的数据链路
- Why：Android 迁移时最容易遗漏跨组件事件与副作用
- How：按“触发 -> 状态变更 -> UI 更新”描述关键链路

5. 数据模型与字段语义
- What：Task/Step/Session/Settings 字段说明及关键约束
- Why：Android 本地数据库（Room/DataStore）建模需对齐
- How：列出字段含义、可空性、时间戳语义、状态枚举

6. Android MVP 映射建议
- What：Web 页面与 Android 模块映射建议
- Why：降低技术栈差异带来的迁移歧义
- How：
  - Route -> Destination(Fragment/Compose Screen)
  - Zustand Action -> ViewModel Intent/UseCase
  - Persist Store -> Room + DataStore
  - Bottom Sheet / Dialog / Timer 对应 Android 组件建议

7. 实施优先级与最小闭环
- What：建议 MVP 开发顺序
- Why：先打通核心闭环再扩展统计与导入导出
- How：建议顺序：Today+TaskEditor -> TaskDetail+Step+Pomodoro -> Tasks 筛选 -> Stats -> Settings 数据管理

## 4. Assumptions & Decisions（假设与决策）

- 决策 1：本次只新增 1 份“逻辑与文件映射文档”，不改动 `page_design.md` 内容。
- 决策 2：文档语言使用中文，技术名称保留英文（如 Zustand、Route、ViewModel、Room）。
- 决策 3：文档重点是“可供 AI 检索的源码映射”，不是 Android 代码实现细节设计稿。
- 决策 4：严格基于现有仓库真实文件路径输出，不引入仓库中不存在的模块名。

## 5. Verification Steps（验证步骤）

1. 文件落位验证
- 确认 `e:\code\MyPotato\docs\ui-skeleton-android\lovable_ui_logic_file_map.md` 已创建。

2. 内容完整性验证
- 检查是否覆盖 5 个页面：Today / Tasks / Stats / Settings / TaskDetail。
- 检查是否包含全局状态、模型、关键组件、路由与数据流说明。

3. 可检索性验证
- 任意页面在文档中都能定位到对应源文件路径（页面文件 + 关键组件 + 状态入口）。

4. Android 可迁移性验证
- 文档中包含 Web -> Android MVP 映射建议与最小实现顺序，可直接作为后续 AI 输入上下文。

