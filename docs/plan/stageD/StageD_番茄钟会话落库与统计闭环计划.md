# Stage D：番茄钟会话落库与统计闭环开发计划

> 文档版本：v1.0\
> 更新日期：2026-07-12\
> 适用阶段：MyPotato Stage D\
> 前置条件：Stage C（Room 数据结构落地）已完成，Repository 接口 + RoomRepository + ViewModel + Flow/StateFlow 链路已建立\
> 当前进度：待启动

***

## 一、阶段目标

### 1.1 核心目标

Stage D 的核心目标是**完成番茄钟会话持久化与统计页面三模块闭环**，让用户能够看到真实的任务执行数据和统计分析。

### 1.2 目标产出

| 目标 | 说明 |
|------|------|
| 番茄钟会话落库 | 每次番茄钟计时结束后，会话数据持久化到 Room 数据库 |
| 统计模块 A 闭环 | 时间段分布图表接入真实数据（PomodoroSession + TaskStep） |
| 统计模块 B 闭环 | 完成情况概览卡片接入真实数据 |
| 统计模块 C 实现 | 新增类别步骤完成时间容器与图表 |
| 图表库选型定型 | 评估并确定 MPAndroidChart 或 Vico 作为图表库 |
| 统计口径说明 | 添加"仅统计长时任务/步骤"口径说明文案 |
| 空态视图 | 添加统计数据为空时的引导视图 |
| 时间维度修复 | 修复今日/本周/本月与 DAY/MONTH/YEAR 的映射错位 |

### 1.3 数据流转链路

```
PomodoroActivity -> PomodoroViewModel -> PomodoroRepository -> RoomDatabase
                                                              │
StatisticsFragment <- StatisticsViewModel <- TaskRepository <-┘
                                         <- PomodoroRepository <-
```

***

## 二、实施步骤

### 步骤 D1：番茄钟会话落库

**描述：**
完善 PomodoroViewModel，在番茄钟会话结束时调用 PomodoroRepository 保存会话数据到数据库。

**前置条件：**
- `PomodoroSessionEntity` 已创建（Stage C）
- `PomodoroSessionDao` 已创建（Stage C）
- `RoomPomodoroRepository` 已实现（Stage C）

**实现要点：**

| 场景 | 操作 | 说明 |
|------|------|------|
| 开始计时 | 创建 `PomodoroSession`，状态设为 RUNNING，记录 `startedAt` | 从 `System.currentTimeMillis() / 1000` 获取秒级时间戳 |
| 暂停计时 | 更新会话状态为 PAUSED，记录暂停时间 | 累计暂停时长 |
| 继续计时 | 更新会话状态为 RUNNING | 恢复计时 |
| 计时结束 | 更新会话状态为 COMPLETED，记录 `endedAt`、`focusDurationSec`、`breakDurationSec`、`cycles` | 调用 `PomodoroRepository.updateSession()` |
| 手动结束 | 更新会话状态为 COMPLETED，记录实际专注时长 | 支持提前结束 |

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `viewmodel/PomodoroViewModel.kt` | 添加会话创建、状态更新、落库逻辑 |
| `ui/pomodoro/PomodoroActivity.kt` | 确保会话结束时调用 ViewModel 的落库方法 |

**验证标准：**
- 完成一次番茄钟计时后，数据库 `pomodoro_session` 表中存在对应记录
- 会话记录包含正确的 `taskId`、`startedAt`、`endedAt`、`focusDurationSec`
- 暂停/继续操作正确累计专注时长
- 手动结束能正确记录实际专注时长

***

### 步骤 D2：图表库选型评估

**描述：**
评估 MPAndroidChart 和 Vico 两个图表库，确定最终选型并集成。

**评估维度：**

| 维度 | MPAndroidChart | Vico |
|------|----------------|------|
| 所属生态 | 第三方开源库 | 第三方开源库（JetBrains 维护） |
| 语言支持 | Java/Kotlin | Kotlin-native |
| 图表类型 | 丰富（折线、柱状、饼图、雷达等） | 基础（折线、柱状、组合图） |
| 动画支持 | 丰富 | 基础 |
| 文档完善度 | 高 | 中等 |
| 社区活跃度 | 高 | 中等 |
| 学习曲线 | 较低 | 较低 |
| 包体积 | 较大 | 较小 |
| 与 Compose 集成 | 需适配 | 原生支持 |
| 最新更新 | 2024 年 | 2025 年 |

**选型决策：**

基于当前项目使用 ViewBinding + XML 架构，且 MPAndroidChart 成熟度更高、文档更完善，**推荐选择 MPAndroidChart**。

**集成步骤：**

1. 在 `libs.versions.toml` 中添加 MPAndroidChart 版本
2. 在 `app/build.gradle` 中添加依赖
3. 在统计页布局中添加 `com.github.mikephil.charting.charts.BarChart` 或 `LineChart`

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `gradle/libs.versions.toml` | 添加 mpandroidchart 版本号 |
| `app/build.gradle` | 添加 implementation 依赖 |

**验证标准：**
- Gradle 同步成功，无依赖冲突
- 统计页布局能正常引入图表控件

***

### 步骤 D3：统计模块 A 闭环（时间段分布）

**描述：**
将统计页模块 A 的堆叠柱状图接入真实数据，替换当前硬编码的 Mock 数据。

**数据来源：**
- `PomodoroSession`：长时任务的专注时长分布
- `TaskStep`：长时任务的步骤完成时长分布

**实现要点：**

1. **数据聚合逻辑**：
   - 根据时间维度（日/周/月）聚合数据
   - 按任务类型过滤，仅统计长时任务（`taskType == LONG`）
   - 按分类分组统计

2. **Repository 扩展方法**：
   ```kotlin
   // 在 RoomPomodoroRepository 中添加
   fun getSessionDurationByTimeRange(startTime: Long, endTime: Long): Flow<List<SessionDurationSummary>>
   fun getSessionDurationByCategory(timeRange: LongRange): Flow<List<CategoryDurationSummary>>
   ```

3. **StatisticsViewModel**：
   - 添加时间范围计算方法
   - 添加数据聚合逻辑
   - 暴露 `statisticsData` StateFlow

4. **UI 更新**：
   - 替换 `StatisticsStackBarView` 的 Mock 数据
   - 支持多系列堆叠（不同分类的时长分布）

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `data/repository/RoomPomodoroRepository.kt` | 添加按时间范围和分类的统计查询方法 |
| `data/dao/PomodoroSessionDao.kt` | 添加统计聚合查询 |
| `viewmodel/StatisticsViewModel.kt` | 添加统计数据聚合与状态管理 |
| `ui/statistics/StatisticsFragment.kt` | 接入 ViewModel 数据，移除 Mock 数据 |
| `ui/statistics/StatisticsStackBarView.kt` | 支持多系列数据渲染 |

**验证标准：**
- 统计页显示真实的番茄钟会话时长分布
- 切换时间维度（今日/本周/本月）数据正确更新
- 按分类筛选数据正确过滤
- 仅显示长时任务数据

***

### 步骤 D4：统计模块 B 闭环（完成情况）

**描述：**
将统计页模块 B 的概览卡片（已完成数、进行中数、完成率）接入真实数据。

**数据来源：**
- `Task`：任务状态统计
- `TaskStep`：步骤完成统计

**实现要点：**

1. **Repository 扩展方法**：
   ```kotlin
   // 在 RoomTaskRepository 中添加
   suspend fun getTaskCountByStatus(timeRange: LongRange): TaskStatusCount
   suspend fun getStepCompletionRate(timeRange: LongRange): Double
   ```

2. **StatisticsViewModel**：
   - 添加任务统计数据获取逻辑
   - 添加完成率计算（完成任务数 / 总任务数）
   - 暴露 `summaryData` StateFlow

3. **UI 更新**：
   - 替换硬编码的统计数值
   - 添加动画过渡效果

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `data/repository/RoomTaskRepository.kt` | 添加任务统计查询方法 |
| `data/dao/TaskDao.kt` | 添加按状态计数查询 |
| `viewmodel/StatisticsViewModel.kt` | 添加概览数据聚合逻辑 |
| `ui/statistics/StatisticsFragment.kt` | 接入真实统计数据 |

**验证标准：**
- 概览卡片显示真实的任务完成情况
- 完成率计算准确
- 切换时间维度数据正确更新

***

### 步骤 D5：统计模块 C 实现（类别步骤完成时间）

**描述：**
实现统计页模块 C（类别步骤完成时间），包含容器结构和图表展示。

**功能需求：**
- 按类别分组，显示各类别的步骤完成总时长
- 使用横向柱状图或饼图展示
- 支持点击类别筛选

**实现要点：**

1. **布局设计**：
   - 添加模块 C 容器（CardView）
   - 添加标题栏（"类别步骤完成时间"）
   - 添加图表区域（横向柱状图）
   - 添加图例

2. **数据聚合逻辑**：
   ```kotlin
   // 在 RoomTaskRepository 中添加
   fun getCategoryStepDuration(timeRange: LongRange): Flow<List<CategoryStepDuration>>
   ```

3. **UI 更新**：
   - 创建 `StatisticsHorizontalBarView` 或使用 MPAndroidChart
   - 绑定数据并渲染

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `data/repository/RoomTaskRepository.kt` | 添加类别步骤时长统计方法 |
| `data/dao/TaskStepDao.kt` | 添加按类别聚合查询 |
| `viewmodel/StatisticsViewModel.kt` | 添加模块 C 数据聚合逻辑 |
| `ui/statistics/StatisticsFragment.kt` | 添加模块 C 容器布局 |
| `res/layout/fragment_statistics.xml` | 添加模块 C 布局 |

**验证标准：**
- 模块 C 容器结构存在且可见
- 显示各分类的步骤完成时长分布
- 图表数据与数据库一致

***

### 步骤 D6：统计口径说明与空态

**描述：**
添加"仅统计长时任务/步骤"口径说明文案，并实现数据为空时的空态视图。

**实现要点：**

1. **口径说明文案**：
   - 在统计页顶部添加说明文案："统计数据仅包含长时任务及其步骤"
   - 在模块 A/B/C 分别添加小字体说明
   - 添加到 `strings.xml`

2. **空态视图**：
   - 当无统计数据时显示空态插图和引导文案
   - 空态文案："暂无统计数据，完成长时任务后将在此显示"
   - 添加空态插图（或使用现有插图）

3. **UI 状态管理**：
   - `StatisticsViewModel` 添加 `isEmpty` 状态
   - 根据数据是否为空切换正常视图与空态视图

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `res/values/strings.xml` | 添加统计口径说明和空态文案 |
| `viewmodel/StatisticsViewModel.kt` | 添加数据为空状态判断 |
| `ui/statistics/StatisticsFragment.kt` | 添加口径说明和空态视图切换逻辑 |
| `res/layout/fragment_statistics.xml` | 添加口径说明 TextView 和空态布局 |

**验证标准：**
- 统计页显示"仅统计长时任务/步骤"口径说明
- 无数据时显示空态视图
- 有数据时正常显示统计图表

***

### 步骤 D7：统计页时间维度修复

**描述：**
修复统计页时间维度映射错位问题（UI 文案"今日/本周/本月"映射到 `DAY/MONTH/YEAR`）。

**问题分析：**

当前代码中 `setupModes()` 将 UI 文案映射为：
- "今日" → `StatisticsMode.DAY`
- "本周" → `StatisticsMode.MONTH`
- "本月" → `StatisticsMode.YEAR`

正确映射应为：
- "今日" → `StatisticsMode.DAY`
- "本周" → `StatisticsMode.WEEK`
- "本月" → `StatisticsMode.MONTH`

**实现要点：**

1. **更新枚举定义**：
   - 添加 `WEEK` 枚举值
   - 移除 `YEAR`（或保留作为扩展）

2. **修复映射关系**：
   - "今日" → `DAY`（过去 24 小时）
   - "本周" → `WEEK`（本周一至当前）
   - "本月" → `MONTH`（本月 1 日至当前）

3. **时间范围计算**：
   - 根据枚举值计算对应的时间戳范围
   - 传递给 Repository 查询

**影响文件：**

| 文件路径 | 修改内容 |
|----------|----------|
| `domain/StatisticsMode.kt` | 添加 WEEK 枚举值 |
| `viewmodel/StatisticsViewModel.kt` | 修复时间范围计算逻辑 |
| `ui/statistics/StatisticsFragment.kt` | 修复 Chip 与枚举的映射 |

**验证标准：**
- "今日"显示过去 24 小时数据
- "本周"显示本周数据
- "本月"显示本月数据
- 文案与实际模式一致

***

### 步骤 D8：验证与测试

**描述：**
对 Stage D 所有功能进行验证测试。

**验证清单：**

| 验证项 | 操作 | 预期结果 |
|--------|------|----------|
| 番茄钟会话落库 | 完成一次番茄钟计时 | `pomodoro_session` 表新增记录 |
| 统计模块 A | 切换时间维度 | 图表显示真实数据 |
| 统计模块 B | 查看概览卡片 | 显示真实完成数和完成率 |
| 统计模块 C | 查看类别步骤完成时间 | 显示各分类步骤时长分布 |
| 口径说明 | 打开统计页 | 显示"仅统计长时任务/步骤"说明 |
| 空态视图 | 清空所有长时任务数据 | 显示空态引导 |
| 时间维度修复 | 切换今日/本周/本月 | 数据范围正确对应 |
| 分类筛选 | 选择不同分类 | 图表数据正确过滤 |
| 项目编译 | Gradle 构建 | BUILD SUCCESSFUL |

**测试覆盖：**

| 测试类型 | 覆盖范围 |
|----------|----------|
| 单元测试 | Repository 统计查询方法 |
| 集成测试 | 番茄钟会话落库流程 |
| UI 测试 | 统计页数据展示 |

***

## 三、文件变更清单

### 3.1 新增文件

| 文件路径 | 类型 | 说明 |
|----------|------|------|
| `domain/StatisticsMode.kt` | 枚举 | 统计时间维度枚举（DAY/WEEK/MONTH） |
| `data/repository/StatisticsSummary.kt` | 数据类 | 统计汇总数据模型 |
| `ui/statistics/StatisticsHorizontalBarView.kt` | 自定义 View | 模块 C 横向柱状图（如使用自定义实现） |

### 3.2 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `gradle/libs.versions.toml` | 添加 MPAndroidChart 版本号 |
| `app/build.gradle` | 添加 MPAndroidChart 依赖 |
| `data/dao/PomodoroSessionDao.kt` | 添加统计聚合查询方法 |
| `data/dao/TaskDao.kt` | 添加按状态计数查询 |
| `data/dao/TaskStepDao.kt` | 添加按类别聚合查询 |
| `data/repository/RoomPomodoroRepository.kt` | 添加统计查询方法 |
| `data/repository/RoomTaskRepository.kt` | 添加统计查询方法 |
| `viewmodel/PomodoroViewModel.kt` | 添加会话落库逻辑 |
| `viewmodel/StatisticsViewModel.kt` | 添加统计数据聚合逻辑 |
| `ui/pomodoro/PomodoroActivity.kt` | 确保会话结束时落库 |
| `ui/statistics/StatisticsFragment.kt` | 接入真实数据，添加口径说明和空态 |
| `ui/statistics/StatisticsStackBarView.kt` | 支持多系列数据渲染 |
| `res/layout/fragment_statistics.xml` | 添加模块 C 布局和口径说明 |
| `res/values/strings.xml` | 添加统计相关文案 |

***

## 四、风险与对策

| 风险 | 对策 |
|------|------|
| 图表库引入导致包体积增大 | 选择轻量级图表库，或使用现有自定义 View |
| 统计查询性能问题 | 为常用统计字段创建索引，使用 Room 查询优化 |
| 时间维度边界计算错误 | 编写单元测试验证时间范围计算 |
| 空态视图与正常视图切换异常 | 使用 StateFlow 统一管理视图状态 |
| 模块 C 布局与现有风格不一致 | 复用现有卡片样式和间距规范 |

***

## 五、验收标准

| 验收项 | 状态 | 备注 |
|--------|------|------|
| ✅ D1：番茄钟会话落库 | 已实现 | 完成计时后数据库有记录，支持暂停/继续/手动结束 |
| ✅ D2：图表库选型评估与集成 | 待实现 | MPAndroidChart 集成成功 |
| ✅ D3：统计模块 A 闭环 | 待实现 | 时间段分布图表接入真实数据 |
| ✅ D4：统计模块 B 闭环 | 待实现 | 完成情况概览卡片接入真实数据 |
| ✅ D5：统计模块 C 实现 | 待实现 | 类别步骤完成时间容器与图表 |
| ✅ D6：统计口径说明与空态 | 待实现 | 口径说明文案 + 空态视图 |
| ✅ D7：统计页时间维度修复 | 待实现 | 今日/本周/本月映射正确 |
| ✅ D8：验证与测试 | 待实现 | 全流程验证通过 |

***

## 六、技术要点

- **MPAndroidChart**：Android 图表库，支持多种图表类型和动画效果
- **Room 查询优化**：`@Query` 自定义 SQL，使用聚合函数（SUM、COUNT、GROUP BY）
- **时间范围计算**：使用 `Calendar` 或 `LocalDate` 计算时间边界
- **状态管理**：`StateFlow` 管理统计数据和视图状态
- **空态设计**：数据为空时显示引导视图，提升用户体验
- **口径说明**：明确统计范围，避免用户误解

***

> 本计划作为 Stage D 的执行指南，严格按顺序推进。完成后进入 Stage E（架构补齐 + 质量加固）。