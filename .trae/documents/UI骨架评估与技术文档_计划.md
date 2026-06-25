# UI 骨架评估与技术文档生成计划

## 一、项目调研结论

### 1.1 项目概览

MyPotato 是一款基于 Android 原生开发的番茄钟 TODO 应用，采用 Kotlin 语言、Jetpack 组件库与 Material Design 3 设计规范。当前处于 **UI 骨架阶段**，主要页面结构已搭建完成，数据层仅有基础框架，业务逻辑通过写死的 Mock 数据进行演示。

### 1.2 技术栈

| 技术领域 | 选型 |
|---------|------|
| 开发语言 | Kotlin |
| UI 框架 | ViewBinding + XML 布局 + Material 3 |
| 导航 | Jetpack Navigation Component |
| 架构模式 | 单 Activity + 多 Fragment（MVVM 雏形） |
| 数据持久化 | Room（仅 AppInfo 表，未接入业务） |
| 构建工具 | Gradle + Version Catalog (libs.versions.toml) |
| 最低 SDK | API 29 (Android 10) |
| 目标 SDK | API 36 |

### 1.3 代码结构

```
app/src/main/java/com/gordon/mypotato/
├── MainActivity.kt              # 主 Activity，承载底部导航与 Fragment 容器
├── MainViewModel.kt             # 共享 ViewModel（目前仅初始化数据库）
├── data/
│   ├── AppDatabase.kt           # Room 数据库单例
│   ├── dao/
│   │   └── AppInfoDao.kt        # AppInfo DAO
│   └── entity/
│       └── AppInfo.kt           # 应用信息实体
└── ui/
    ├── today/
    │   └── TodayFragment.kt     # Today 页（首页）
    ├── tasks/
    │   ├── TasksFragment.kt     # Tasks 列表页
    │   └── TaskDetailActivity.kt # 任务详情页
    ├── statistics/
    │   ├── StatisticsFragment.kt # 统计页
    │   └── StatisticsStackBarView.kt # 堆叠柱状图自定义 View
    ├── settings/
    │   └── SettingsFragment.kt  # 设置页
    └── pomodoro/
        └── PomodoroActivity.kt  # 番茄钟页面
```

---

## 二、UI 骨架验收清单逐项评估

### Stage A：MVP 体验定义（先 UI 骨架）

#### A1. 导航结构 ✅ 基本完成

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 底部导航 4 个入口：Today / Tasks / Statistics / Settings | ✅ 通过 | [bottom_nav_menu.xml](file:///e:/code/MyPotato/app/src/main/res/menu/bottom_nav_menu.xml) + [main_nav_graph.xml](file:///e:/code/MyPotato/app/src/main/res/navigation/main_nav_graph.xml) 已实现 |
| 顶部 AppBar 标题正确且一致 | ⚠️ 部分通过 | 各 Fragment 有标题文案，但未使用统一 Top AppBar 组件，标题在各自布局内 |
| 深层页面具备返回路径 | ✅ 通过 | [TaskDetailActivity](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt) 和 [PomodoroActivity](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/pomodoro/PomodoroActivity.kt) 均有 Toolbar 返回键 |

#### A2. Today 页骨架 ⚠️ 部分符合（筛选维度与验收清单不一致）

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 顶部日期与问候语区域 | ✅ 通过 | [TodayFragment.setupHeaderDate()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt#L63-L67) 实现了日期显示 |
| 快速筛选 UI：全部 / 单次 / 长时 | ❌ 不符合 | 实际实现是四象限筛选（全部/紧急且重要/重要/紧急/其他），不是按任务类型筛选 |
| 任务列表按四象限分组的结构可见 | ⚠️ 部分通过 | 有四象限筛选 Chip，但列表是扁平列表，未按四象限分组展示 |
| FAB 新建入口可见，点击有占位反馈 | ✅ 通过 | [TodayFragment.setupFab()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt#L107-L111) + BottomSheet 弹窗 |
| 空态存在且有引导 | ✅ 通过 | layout_empty_state 存在，有"新建任务"引导文案 |

#### A3. Tasks 页骨架 ⚠️ 基本完成（缺日期范围筛选）

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 顶部 Tab：按类别 / 按四象限 / 按状态 | ✅ 通过 | [TasksFragment.setupDimensionTabs()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt#L119-L128) 实现了分段控制器 |
| 筛选区控件：类别、任务类型、日期范围 | ⚠️ 部分通过 | 有类别、四象限、状态筛选，**缺少任务类型筛选和日期范围筛选** |
| 列表项支持展示步骤摘要区域 | ✅ 通过 | item_today_task.xml 有步骤标签（tvStepTag） |
| 空态存在且有引导 | ✅ 通过 | layout_tasks_empty_state 存在 |

#### A4. Statistics 页骨架 ⚠️ 部分完成（缺类别/任务类型筛选 + 口径说明）

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 顶部筛选：日/周/月、类别、任务类型 | ⚠️ 部分通过 | 有日/月/年切换（非周），**缺少类别和任务类型筛选** |
| 模块 A/B/C 容器结构存在 | ✅ 通过 | 有统计卡片（已完成/进行中/完成率）+ 堆叠柱状图 |
| 模块 A 空态/说明：仅统计长时任务/步骤 | ❌ 缺失 | 页面无任何关于"仅统计长时任务"的说明文案 |

#### A5. Settings 页骨架 ⚠️ 部分完成（导入/清空缺失 + 语言仅占位）

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 主题模式入口：跟随系统/浅色/深色 | ⚠️ 部分通过 | UI 有三选项，但 [MainActivity](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/MainActivity.kt#L15) 强制 MODE_NIGHT_NO，深色模式未实际生效 |
| 语言入口：跟随系统/简体中文/English | ⚠️ 部分通过 | 入口存在但点击仅 Toast "即将上线"，无实际切换逻辑 |
| 数据管理入口：导入/导出/清空 | ⚠️ 部分通过 | 仅有"导出 JSON"入口（Toast 占位），**缺少导入和清空入口** |
| 关于入口：版本号、检查更新占位 | ✅ 通过 | 版本号读取自 PackageManager，检查更新为 Toast 占位 |

#### A6. 关键规则落点 ⚠️ 部分通过

| 验收项 | 状态 | 说明 |
|-------|------|------|
| 单次任务不可启动番茄钟（入口禁用/隐藏 + 原因说明） | ✅ 通过 | [TaskDetailActivity.renderSingleTask()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt#L96-L106) 隐藏番茄钟按钮并显示 btnSingleTaskHint |
| Statistics 口径表达：单次任务不纳入时间段统计 | ❌ 缺失 | 统计页无任何口径说明文字 |

### Stage B：核心流程跑通 ❌ 未开始

Stage B 各项均未完成，属于下一阶段工作。

---

## 三、评估结论

### 3.1 总体完成度

**Stage A 完成度约 70%**，核心骨架已搭建，但存在以下主要差距：

1. **Today 页筛选维度错位**：验收清单要求"全部/单次/长时"，实际是四象限筛选
2. **Statistics 页缺少关键筛选和口径说明**：无类别/任务类型筛选，无"仅统计长时任务"说明
3. **Settings 页功能残缺**：导入/清空入口缺失，语言切换未实现，主题切换不生效
4. **缺少 Task Edit 页面**：验收清单提到 Task Edit，但当前仅有详情页，编辑入口是占位

### 3.2 是否达到"最小实现"

**未完全达到**。虽然主要页面的 UI 框架已搭建，但验收清单中明确要求的部分功能点缺失，特别是：
- Statistics 页的"仅统计长时任务"口径说明是关键规则落点，必须明确表达
- Today 页的筛选维度与验收清单不一致
- Settings 页的三个数据管理入口（导入/导出/清空）不完整

---

## 四、技术文档撰写计划

### 4.1 文档输出位置

- 文件路径：`e:\code\MyPotato\docs\项目技术架构与实现说明.md`
- 文档语言：中文

### 4.2 文档结构大纲

```
1. 项目概述
   1.1 项目定位
   1.2 技术栈总览

2. 项目结构
   2.1 包结构说明
   2.2 模块划分

3. 架构设计
   3.1 整体架构（单 Activity + 多 Fragment）
   3.2 导航架构（Jetpack Navigation）
   3.3 数据层架构（Room + 后续 Repository 规划）

4. 核心页面实现
   4.1 MainActivity（底部导航 + Fragment 容器）
   4.2 Today 页面
   4.3 Tasks 页面
   4.4 TaskDetail 页面
   4.5 Statistics 页面（含自定义 View）
   4.6 Settings 页面
   4.7 Pomodoro 页面

5. 数据层现状
   5.1 Room 数据库配置
   5.2 现有实体与 DAO
   5.3 当前数据来源（Mock 数据）

6. UI 设计规范
   6.1 Material 3 主题配置
   6.2 颜色系统
   6.3 自定义组件

7. 核心技术点
   7.1 ViewBinding
   7.2 RecyclerView 适配器模式
   7.3 自定义 View 绘制
   7.4 BottomSheetDialog 使用

8. 当前局限与后续规划
   8.1 已知问题
   8.2 技术债
   8.3 下阶段方向
```

### 4.3 涉及的核心文件

**UI 层：**
- [MainActivity.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/MainActivity.kt)
- [TodayFragment.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt)
- [TasksFragment.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt)
- [TaskDetailActivity.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt)
- [StatisticsFragment.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/statistics/StatisticsFragment.kt)
- [StatisticsStackBarView.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/statistics/StatisticsStackBarView.kt)
- [SettingsFragment.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/settings/SettingsFragment.kt)
- [PomodoroActivity.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/pomodoro/PomodoroActivity.kt)

**数据层：**
- [AppDatabase.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/data/AppDatabase.kt)
- [AppInfo.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/data/entity/AppInfo.kt)
- [AppInfoDao.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/data/dao/AppInfoDao.kt)

**资源文件：**
- [themes.xml](file:///e:/code/MyPotato/app/src/main/res/values/themes.xml)
- [colors.xml](file:///e:/code/MyPotato/app/src/main/res/values/colors.xml)
- [strings.xml](file:///e:/code/MyPotato/app/src/main/res/values/strings.xml)
- [main_nav_graph.xml](file:///e:/code/MyPotato/app/src/main/res/navigation/main_nav_graph.xml)

---

## 五、风险与注意事项

1. **文档基于当前代码快照**：技术文档反映的是当前代码状态，后续代码变更后需同步更新文档
2. **不修改业务代码**：本次任务仅生成评估与文档，不调整任何业务逻辑或 UI 代码
3. **代码引用格式**：文档中所有文件/函数引用均使用 `file:///` 绝对路径格式，便于 IDE 跳转
