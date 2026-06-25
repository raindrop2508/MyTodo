# UI 骨架验收清单更新与技术文档生成计划

## 一、任务目标

1. 根据当前项目代码实际完成情况，更新 `UI骨架验收清单.md` 中各验收项的勾选状态
2. 撰写项目技术架构与实现说明文档，输出到 `docs/` 目录

***

## 二、项目调研结论

### 2.1 技术栈概览

| 技术领域   | 选型                                            |
| ------ | --------------------------------------------- |
| 开发语言   | Kotlin                                        |
| UI 框架  | ViewBinding + XML 布局 + Material 3             |
| 导航     | Jetpack Navigation Component                  |
| 架构模式   | 单 Activity + 多 Fragment（MVVM 雏形）              |
| 数据持久化  | Room（仅 AppInfo 表，未接入业务）                       |
| 构建工具   | Gradle + Version Catalog (libs.versions.toml) |
| 最低 SDK | API 29 (Android 10)                           |
| 目标 SDK | API 36                                        |

### 2.2 代码结构

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

***

## 三、验收清单逐项评估与更新方案

### Stage A：MVP 体验定义

#### A1. 导航结构

| 验收项                                              | 当前状态 | 更新为  | 依据                                                                                                                                                                                                                                                                      |
| ------------------------------------------------ | ---- | ---- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 底部导航 4 个入口：Today / Tasks / Statistics / Settings | \[x] | \[x] | [bottom\_nav\_menu.xml](file:///e:/code/MyPotato/app/src/main/res/menu/bottom_nav_menu.xml) + [main\_nav\_graph.xml](file:///e:/code/MyPotato/app/src/main/res/navigation/main_nav_graph.xml)                                                                           |
| 顶部 AppBar 标题正确且一致                                | \[x] | \[x] | 各 Fragment 布局内均有标题 TextView，文案取自 strings.xml                                                                                                                                                                                                                            |
| 深层页面具备返回路径                                       | \[x] | \[x] | [TaskDetailActivity](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt#L46-L48) 和 [PomodoroActivity](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/pomodoro/PomodoroActivity.kt#L50-L52) 均有 Toolbar 返回键 |

#### A2. Today 页骨架

| 验收项                  | 当前状态 | 更新为      | 依据                                                                                                                                                 |
| -------------------- | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| 顶部展示日期与问候语区域         | \[x] | \[x]     | [TodayFragment.setupHeaderDate()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt#L63-L67) 显示日期；布局中有问候语区域 |
| 快速筛选 UI：全部 / 单次 / 长时 | \[x] | **\[ ]** | **实际实现为四象限筛选（全部/紧急且重要/重要/紧急/其他），不是按任务类型筛选，与验收清单不符**                                                                                                |
| 任务列表按四象限分组的结构可见      | \[x] | **\[ ]** | 有四象限筛选 Chip，但列表是扁平列表，未按四象限分组展示                                                                                                                     |
| FAB 新建入口可见，点击有占位反馈   | \[x] | \[x]     | [TodayFragment.setupFab()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt#L107-L111) + BottomSheet 弹窗    |
| 空态存在且提供"新建任务"引导      | \[x] | \[x]     | layout\_empty\_state 存在，引导文案为"从右下角新增一个任务"                                                                                                          |

#### A3. Tasks 页骨架

| 验收项                     | 当前状态     | 更新为      | 依据                                                                                                                                                            |
| ----------------------- | -------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 顶部 Tab：按类别 / 按四象限 / 按状态 | **\[ ]** | **\[x]** | [TasksFragment.setupDimensionTabs()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt#L119-L128) 实现了分段控制器（RadioGroup） |
| 筛选区控件：类别、任务类型、日期范围      | **\[ ]** | **\[ ]** | 有类别、四象限、状态筛选，**缺少任务类型筛选和日期范围筛选**                                                                                                                              |
| 列表项支持展示步骤摘要区域           | **\[ ]** | **\[x]** | item\_today\_task.xml 有步骤标签 tvStepTag，有步骤数时显示                                                                                                                 |
| 空态存在且有引导                | **\[ ]** | **\[x]** | layout\_tasks\_empty\_state 存在                                                                                                                                |

#### A4. Statistics 页骨架

| 验收项                   | 当前状态 | 更新为      | 依据                           |
| --------------------- | ---- | -------- | ---------------------------- |
| 顶部筛选：日/周/月、类别、任务类型    | \[x] | **\[ ]** | 有日/月/年切换（非周），**缺少类别和任务类型筛选** |
| 模块 A/B/C 容器结构存在       | \[x] | \[x]     | 有统计卡片（已完成/进行中/完成率）+ 堆叠柱状图    |
| 模块 A 空态/说明：仅统计长时任务/步骤 | \[x] | **\[ ]** | 页面无任何关于"仅统计长时任务"的说明文案        |

#### A5. Settings 页骨架

| 验收项                    | 当前状态 | 更新为      | 依据                                                                                                                                                             |
| ---------------------- | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 主题模式入口：跟随系统/浅色/深色      | \[x] | \[x]     | [SettingsFragment](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/settings/SettingsFragment.kt#L55-L70) 有三选项对话框；但 MainActivity 强制浅色，切换未实际生效 |
| 语言入口：跟随系统/简体中文/English | \[x] | \[x]     | 入口存在，点击后 Toast "即将上线"（占位实现）                                                                                                                                    |
| 数据管理入口：导入/导出/清空        | \[x] | **\[ ]** | 仅有"导出 JSON"入口（Toast 占位），**缺少导入和清空入口**                                                                                                                          |
| 关于入口：版本号、检查更新占位        | \[x] | \[x]     | 版本号读取自 PackageManager，检查更新为 Toast 占位                                                                                                                           |

#### A6. 关键规则落点

| 验收项                          | 当前状态 | 更新为      | 依据                                                                                                                                                                                           |
| ---------------------------- | ---- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 单次任务不可启动番茄钟（入口禁用/隐藏 + 原因说明）  | \[x] | \[x]     | [TaskDetailActivity.renderSingleTask()](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/ui/tasks/TaskDetailActivity.kt#L96-L106) 隐藏番茄钟按钮并显示 btnSingleTaskHint（"单次任务不支持番茄钟计时"） |
| Statistics 口径表达：单次任务不纳入时间段统计 | \[x] | **\[ ]** | 统计页无任何口径说明文字                                                                                                                                                                                 |

### Stage B：核心流程跑通

全部保持 \[ ] 未完成状态，符合当前实际。

***

## 四、技术文档撰写方案

### 4.1 输出文件

* 文件路径：`e:\code\MyPotato\docs\项目技术架构与实现说明.md`

* 文档语言：中文

### 4.2 文档结构

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

***

## 五、执行步骤

1. **修改验收清单**：根据上述评估表，更新 `UI骨架验收清单.md` 中各验收项的勾选状态
2. **生成技术文档**：按照文档结构撰写项目技术架构与实现说明，输出到 `docs/` 目录

***

## 六、涉及文件

**修改文件：**

* [UI骨架验收清单.md](file:///e:/code/MyPotato/docs/ui_skeleton/UI骨架验收清单.md)

**新建文件：**

* `e:\code\MyPotato\docs\项目技术架构与实现说明.md`

**参考文件（只读）：**

* 所有 Kotlin 源码文件

* 资源文件（themes.xml、colors.xml、strings.xml、布局文件等）

