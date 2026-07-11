# Lovable 原型 UI 逻辑与文件映射（Android MVP 参考）

> 适用范围：`e:\code\MyPotato\mypotato-focus-flow-main`（Web 原型）  
> 目标读者：后续 Android Todo App MVP 开发人员 / AI 代码代理  
> 文档关系：本文件补充 `page_design.md` 的视觉规范，聚焦“页面逻辑 + 源码位置 + Android 映射”

## 1. 全局架构总览（Web）

### 1.1 路由与壳层（Route + App Shell）

- 应用入口：`src/main.tsx`
- 路由定义：`src/App.tsx`
- 壳层容器：`src/components/layout/AppShell.tsx`
- 底部导航：`src/components/layout/BottomNav.tsx`

路由清单：

- `/` -> `src/pages/Index.tsx`（Today）
- `/tasks` -> `src/pages/Tasks.tsx`
- `/stats` -> `src/pages/Stats.tsx`
- `/settings` -> `src/pages/Settings.tsx`
- `/task/:id` -> `src/pages/TaskDetail.tsx`

### 1.2 状态管理（State Management）

- 全局 Store：`src/store/useApp.ts`
- 技术：Zustand（含 persist 本地持久化）
- 核心状态域：
  - `tasks`：任务主实体
  - `steps`：任务步骤（子任务）
  - `categories`：任务分类
  - `sessions`：番茄钟会话
  - `settings`：主题、语言、番茄时长配置
- 核心动作（Actions）：
  - 任务：`addTask / updateTask / deleteTask / toggleTask`
  - 步骤：`addStep / updateStep / deleteStep / toggleStep`
  - 会话：`addSession`
  - 配置：`setSettings / importData / resetAll`

### 1.3 核心模型（Domain Model）

- 定义文件：`src/types/index.ts`
- 关键类型：
  - `Task`（任务）
  - `TaskStep`（步骤）
  - `Category`（分类）
  - `PomodoroSession`（番茄记录）
  - `Settings`（应用设置）
- 任务优先级算法：`priorityOf(task)`（四象限：`ui / i / u / n`）

### 1.4 辅助模块

- 国际化（i18n）：`src/lib/i18n.ts`
- 时间格式与日期比较：`src/lib/format.ts`
- 导入导出（JSON/CSV）：`src/lib/io.ts`

---

## 2. 页面设计逻辑与文件位置

以下每页均按“页面定位 -> 核心 UI -> 交互逻辑 -> 状态读写 -> 文件映射”输出。

## 2.1 Today 页面（首页）

- 主文件：`src/pages/Index.tsx`
- 页面定位：展示今日任务、四象限视图、当日统计（完成数/专注时长/连续天数）。

核心 UI 区块：

- 顶部标题区：`PageHeader`
- 欢迎与统计卡片：`Stat`
- 四象限筛选条（All + UI/I/U/N）
- 按象限分组的任务列表：`TaskCard`
- 新增入口：`Fab` + `TaskEditor`

核心交互逻辑：

- “今日任务”过滤：排除 archived；done 任务仅保留今天完成的记录。
- 四象限切换：按 `priorityOf(task)` 分组展示。
- 首页统计计算：
  - 今日完成任务数
  - 今日专注分钟数（work session 汇总）
  - 连续完成天数（streak）

状态读写点：

- 读取：`tasks / sessions`
- 写入：通过 `TaskEditor` 间接触发 `addTask` 或 `updateTask`

关联文件：

- `src/pages/Index.tsx`
- `src/components/TaskCard.tsx`
- `src/components/TaskEditor.tsx`
- `src/components/Fab.tsx`
- `src/lib/format.ts`
- `src/types/index.ts`

## 2.2 Tasks 页面（全量任务）

- 主文件：`src/pages/Tasks.tsx`
- 页面定位：任务全览管理，支持搜索与多维筛选（状态/类型/优先级）。

核心 UI 区块：

- 顶部标题：`PageHeader`
- 搜索框（标题模糊匹配）
- 三行筛选 Chips：
  - 状态：all/todo/done
  - 类型：one-time/long
  - 优先级：all/ui/i/u/n
- 列表项：`TaskCard`
- 新增入口：`Fab` + `TaskEditor`

核心交互逻辑：

- 多条件叠加过滤，最终得到 `filtered` 列表。
- 无结果显示空状态：`EmptyState`。

状态读写点：

- 读取：`tasks`
- 写入：通过 `TaskEditor` 间接写入任务

关联文件：

- `src/pages/Tasks.tsx`
- `src/components/TaskCard.tsx`
- `src/components/TaskEditor.tsx`
- `src/components/EmptyState.tsx`
- `src/types/index.ts`

## 2.3 Stats 页面（统计分析）

- 主文件：`src/pages/Stats.tsx`
- 页面定位：按时间范围（日/周/月）展示专注与完成趋势。

核心 UI 区块：

- 时间范围切换（daily/weekly/monthly）
- KPI 卡片（总专注分钟、专注会话数、已完成任务数）
- 分类专注时长堆叠柱状图（BarChart）
- 完成率饼图（PieChart）
- 步骤完成趋势折线图（LineChart）

核心交互逻辑：

- 切换 range 后重算窗口天数（7/28/90）。
- 基于 `sessions/tasks/categories/steps` 动态聚合图表数据。
- 图表数据按天生成，支持空值回退（Other 分类）。

状态读写点：

- 读取：`tasks / steps / sessions / categories`
- 写入：无直接写入（纯分析页）

关联文件：

- `src/pages/Stats.tsx`
- `src/lib/format.ts`
- `src/store/useApp.ts`

## 2.4 Settings 页面（应用设置）

- 主文件：`src/pages/Settings.tsx`
- 页面定位：主题、语言、番茄参数与数据导入导出。

核心 UI 区块：

- 主题切换：system/light/dark
- 语言切换：en/zh
- 番茄参数：workMin / breakMin
- 数据操作：导出 JSON、导出 CSV、导入 JSON、重置

核心交互逻辑：

- 主题/语言/时长通过 `setSettings` 即时落库。
- 导出：
  - JSON：导出任务、步骤、会话、分类、设置
  - CSV：导出任务主表
- 导入：读取 JSON 后调用 `importData`
- 重置：确认后调用 `resetAll`

状态读写点：

- 读取：`settings / tasks / steps / sessions / categories`
- 写入：`setSettings / importData / resetAll`

关联文件：

- `src/pages/Settings.tsx`
- `src/lib/io.ts`
- `src/lib/i18n.ts`
- `src/store/useApp.ts`

## 2.5 TaskDetail 页面（任务详情）

- 主文件：`src/pages/TaskDetail.tsx`
- 页面定位：单任务详情、步骤管理、番茄钟专注入口。

核心 UI 区块：

- 顶部操作：返回、编辑、删除
- 任务主体：完成状态、标题描述、分类/类型/优先级标签
- 长任务专属：番茄启动按钮 + 累计专注分钟
- 步骤列表：勾选、编辑、删除、添加新步骤
- 弹层：`TaskEditor`（编辑任务）、`Pomodoro`（计时）

核心交互逻辑：

- 根据路由参数 `id` 查询任务，不存在则返回占位提示。
- 步骤按 `order` 排序展示。
- 长任务支持：
  - 针对整个任务启动番茄
  - 针对某一步骤启动番茄
- 番茄结束后记录 `PomodoroSession`，用于统计页和任务专注时长展示。

状态读写点：

- 读取：`tasks / steps / categories / sessions`
- 写入：`toggleTask / deleteTask / addStep / updateStep / deleteStep / toggleStep`

关联文件：

- `src/pages/TaskDetail.tsx`
- `src/components/Pomodoro.tsx`
- `src/components/TaskEditor.tsx`
- `src/store/useApp.ts`

---

## 3. 关键组件与数据流（事件链）

## 3.1 TaskCard 事件链

- 文件：`src/components/TaskCard.tsx`
- 链路：
  - 点击卡片 -> 跳转详情页 `/task/:id`
  - 点击勾选或右滑阈值达成 -> `toggleTask(task.id)` -> 任务状态变化 -> 列表 UI 更新

## 3.2 TaskEditor 事件链

- 文件：`src/components/TaskEditor.tsx`
- 链路：
  - 新建模式 -> `addTask(data)` -> 任务列表新增
  - 编辑模式 -> `updateTask(task.id, data)` -> 详情/列表同步刷新
  - 删除模式 -> `deleteTask(task.id)` -> 任务与相关步骤一起移除

## 3.3 Pomodoro 事件链

- 文件：`src/components/Pomodoro.tsx`
- 链路：
  - 开始计时（work 或 break）-> 秒级倒计时
  - 倒计时归零 -> `addSession(...)` 写入会话 -> work/break 模式自动切换
  - 会话数据被 Today/Stats/TaskDetail 复用（专注分钟与图表聚合）

---

## 4. 数据模型字段语义（Android 建模参考）

## 4.1 Task

- 唯一标识：`id`
- 业务字段：`title/content/note/type/categoryId/urgent/important/status`
- 时间字段：`createdAt/updatedAt/completedAt`
- 约束建议：
  - `title` 必填
  - `completedAt` 仅在 done 场景赋值
  - `status` 与 UI 勾选保持双向一致

## 4.2 TaskStep

- 关联任务：`taskId`
- 排序字段：`order`
- 完成态：`done + completedAt`
- 约束建议：删除任务时级联删除步骤

## 4.3 PomodoroSession

- 关联关系：`taskId`，可选 `stepId`
- 核心字段：`kind(work/break)`、`startedAt`、`endedAt`、`durationSec`
- 统计语义：仅 `work` 会话用于专注时长指标

## 4.4 Settings / Category

- `Settings`：`theme/lang/workMin/breakMin`
- `Category`：`name/color`（当前颜色为 HSL 字符串）

---

## 5. Web -> Android MVP 映射建议

## 5.1 页面与导航映射

- `Route "/"` -> `TodayFragment` 或 `TodayScreen`
- `Route "/tasks"` -> `TasksFragment` 或 `TasksScreen`
- `Route "/stats"` -> `StatsFragment` 或 `StatsScreen`
- `Route "/settings"` -> `SettingsFragment` 或 `SettingsScreen`
- `Route "/task/:id"` -> `TaskDetailFragment` 或 `TaskDetailScreen`

导航建议：

- 保留底部四主 Tab（Today/Tasks/Stats/Settings）
- 详情页作为二级目的地（从 Today/Tasks 进入）

## 5.2 状态管理映射

- Zustand Store -> `ViewModel + UseCase + Repository`
- persist 本地存储 -> `Room + DataStore`
- 事件触发模式：
  - UI Intent -> ViewModel Action -> Repository 写入 -> StateFlow/LiveData 回推 UI

## 5.3 组件能力映射

- `TaskEditor` -> `BottomSheetDialogFragment`（XML）或 `ModalBottomSheet`（Compose）
- `Pomodoro` -> 全屏 `DialogFragment` / Compose Dialog + `CountDownTimer` / 协程定时
- `TaskCard` 右滑完成 -> `ItemTouchHelper`（RecyclerView）或 Compose `swipeable`

## 5.4 MVP 最小实现顺序

1. Today + TaskEditor（先打通新增/完成闭环）
2. TaskDetail + Step 管理 + Pomodoro（补齐深度流程）
3. Tasks 筛选与搜索（提升管理效率）
4. Stats 图表聚合（数据可视化）
5. Settings 导入导出与重置（数据运维能力）

---

## 6. 快速索引（给 AI 的入口）

- 路由入口：`src/App.tsx`
- 全局状态：`src/store/useApp.ts`
- 模型定义：`src/types/index.ts`
- Today：`src/pages/Index.tsx`
- Tasks：`src/pages/Tasks.tsx`
- Stats：`src/pages/Stats.tsx`
- Settings：`src/pages/Settings.tsx`
- TaskDetail：`src/pages/TaskDetail.tsx`
- 任务卡片：`src/components/TaskCard.tsx`
- 任务编辑：`src/components/TaskEditor.tsx`
- 番茄计时：`src/components/Pomodoro.tsx`

> 使用建议：后续 AI 在生成 Android 代码前，先输入本文件 + `page_design.md`，可同时具备视觉规范与业务逻辑上下文。
