# Stage D：番茄钟计时持久化 — 实际落地方案

> 文档版本：v1.0  
> 更新日期：2026-09-05  
> 状态：已实现（收尾确认版）  
> 适用范围：相对 `main` 的活动计时持久化与冷启动收尾实现说明  
> 关联：GitHub [Issue #14](https://github.com/raindrop2508/MyTodo/issues/14)（完成说明）、[#9](https://github.com/raindrop2508/MyTodo/issues/9)、PR #11  
> 前置：Stage C Room 落地；Stage D1 会话历史落库已具备  
> 简版摘要：[番茄钟活动计时持久化](../stageE/番茄钟活动计时持久化.md)

***

## 一、背景与决策

### 1.1 问题来源（#9）

相对 `main` 上的番茄钟实现（`PomodoroViewModel + CountDownTimer + StateFlow` + Room 历史会话），存在：

| 问题 | main 上的表现 |
|------|----------------|
| 活动计时未完整落库 | 仅有 `startedAt` / `endedAt` / 时长字段 / `status`，无 phase、目标结束时间、暂停剩余等 |
| 进程被杀后无法恢复 | 状态在内存；冷启动后 `loadTask()` 重置为完整专注时长 |
| 会话一致性缺口 | 重置不清会话；`onCleared` 不收尾；异步 `createSession` 可能留下孤儿 `IN_PROGRESS` |
| 专注时长双重扣减 | `(totalTimeMs - timeLeftMs) - pausedDuration` 在 Tick 已停表时再减暂停 |
| 阶段机错误 | 休息结束后仍走「选短休/长休」，无法回到 `FOCUS` |

### 1.2 本轮落地口径（#14）

#9 原方案含活动计时持久化、**静默恢复倒计时**、阶段机与时长修复等。本轮按 **「收尾确认版」** 落地：

- **做**：活动字段持久化、先落库再计时、全库活动会话 ≤ 1、阶段机与时长修复、冷启动弹窗收尾
- **不做（后置）**：杀进程后静默续跑、自动跳转番茄钟页、Foreground Service / AlarmManager

产品取舍：冷启动时由用户确认是否**保留已计专注时长**；不静默恢复倒计时。继续未完成番茄钟（带参进计时页接着跑）另开 Issue。

***

## 二、相对 main 的实现对照

### 2.1 数据层：`pomodoro_session` 扩展

| 项 | main | 本分支 |
|----|------|--------|
| DB version | 2 | **3**（开发期 `fallbackToDestructiveMigration`，无正式 Migration） |
| 活动字段 | 无 | `phase`、`planned_duration_ms`、`target_end_epoch_ms`、`remaining_ms_when_paused`、`pause_started_at_epoch_ms` |
| 活动查询 | 无 | `getActiveSessions(IN_PROGRESS, PAUSED)` |
| 计时写库 | `updateSessionStatusAndDuration` 等粗粒度更新 | `updateTimerState` / `completeSession`（清目标结束与暂停字段）/ `interruptSession` |

Domain / Entity / Mapper 同步扩展；`PomodoroSession.isActive()`、`getPhase()`；完成态 `getActualFocusDurationSec()` 不再二次扣暂停。

### 2.2 领域层：纯逻辑下沉

| 新增 | 职责 |
|------|------|
| `domain/PomodoroPhase` | FOCUS / SHORT_BREAK / LONG_BREAK（原 ViewModel 内 enum 迁出） |
| `domain/PomodoroTimerLogic` | `determineNextPhase`、墙钟减暂停算专注秒、`estimateElapsedFocusSec`、时长格式化 |
| `domain/OrphanPomodoroSettlement` | 冷启动：清理旧活动行 + 组装用户确认 Prompt；保留 / 丢弃写终态 |

### 2.3 ViewModel：先落库再计时

相对 main 的关键变化（`PomodoroViewModel`）：

1. **`sessionMutex`**：串行化开始 / 暂停 / 继续 / 重置 / 完成，降低竞态孤儿行。
2. **开始**：无 `sessionId` 时先 `interruptOtherActiveSessions()`，再 `addSession`（含 phase、planned、targetEnd），再启动 `CountDownTimer`。
3. **暂停 / 继续**：`updateTimerState` 写入 `PAUSED`/`IN_PROGRESS` 与剩余/目标结束时间。
4. **重置**：活动会话 → `INTERRUPTED`，再清空 UI 会话态。
5. **完成**：专注时长用 `PomodoroTimerLogic.calculateActualFocusSec`；`COMPLETED` 的 `focusDurationSec` 存**实际专注秒**。
6. **阶段流转**：休息结束回到 `FOCUS`；仅专注完成增加 `cycleCount`。
7. **`loadTask`**：不静默恢复活动倒计时；冷启动由 `MainActivity` 弹窗收尾。
8. **`onCleared`**：只取消计时器，**不改 DB**（留给冷启动 `OrphanPomodoroSettlement`）。

### 2.4 UI：冷启动收尾

`MainActivity`（仅 `savedInstanceState == null`）：

1. IO 线程 `OrphanPomodoroSettlement.preparePrompt()`
2. 休息阶段残留 → 提示后 `discard`（`INTERRUPTED`）
3. 专注阶段 → 弹窗：
   - 多条活动行：旧记录已自动 `INTERRUPTED`，文案提示清理条数
   - **保留时长**（已计 > 0）→ `keepFocusDuration` → 会话 `COMPLETED`，**不修改 Task / Step**
   - **不保留** / 已计为 0 → `discard` → `INTERRUPTED`
4. 收尾后之后计时一律**新开会话**

文案：`res/values/strings.xml` 中 `pomodoro_orphan_*`、`pomodoro_phase_*`。

### 2.5 测试

| 文件 | 覆盖 |
|------|------|
| `app/src/test/.../PomodoroTimerLogicTest.kt` | 阶段机、墙钟时长、暂停推算、休息阶段 elapsed=0 |
| `app/src/test/.../OrphanPomodoroSettlementTest.kt` | 多活动行清理、保留/丢弃、休息阶段 |
| `DatabaseCreationTest` | 插入会话补齐 v3 新字段 |

***

## 三、状态与写库约定

### 3.1 会话状态（复用 `SessionStatus`）

| 用户操作 | DB 状态 | 说明 |
|----------|---------|------|
| 开始 | `IN_PROGRESS` | 先 insert，再 Tick |
| 暂停 | `PAUSED` | 写 `remainingMsWhenPaused`、`pauseStartedAtEpochMs`；`targetEnd` 置空 |
| 继续 | `IN_PROGRESS` | 累加暂停秒；重写 `targetEndEpochMs` |
| 正常/手动完成（专注有价值） | `COMPLETED` | `focusDurationSec` = 实际专注秒 |
| 重置 / 丢弃 / 打断其它活动行 | `INTERRUPTED` | `interruptSession`；专注时长清 0 |

活动集合 = `IN_PROGRESS` ∪ `PAUSED`；**全库最多 1 条**。

### 3.2 阶段机

```text
FOCUS → SHORT_BREAK → FOCUS
FOCUS →（完成轮数 % longBreakInterval == 0）→ LONG_BREAK → FOCUS
```

仅 `FOCUS` 完成时 `cycleCount++`。

### 3.3 专注时长

```text
actualFocusSec = clamp( (endedAtSec - startedAtSec) - pausedDurationSec , 0 .. plannedFocusSec )
```

- 不以最后一次 `onTick` 的 `timeLeftMs` 为业务真相源
- 不在「已停表的已运行时长」上再减一遍暂停
- 统计口径（已定、待接统计页）：仅 `phase == FOCUS` 且 `status == COMPLETED`

***

## 四、文件变更清单（相对 main）

### 4.1 新增

| 路径 | 说明 |
|------|------|
| `domain/PomodoroPhase.kt` | 阶段枚举 |
| `domain/PomodoroTimerLogic.kt` | 阶段与时长纯逻辑 |
| `domain/OrphanPomodoroSettlement.kt` | 冷启动收尾协调 |
| `app/src/test/.../PomodoroTimerLogicTest.kt` | 领域单测 |
| `app/src/test/.../OrphanPomodoroSettlementTest.kt` | 收尾单测 |

### 4.2 修改（核心）

| 路径 | 说明 |
|------|------|
| `data/AppDatabase.kt` | version 2 → 3 |
| `data/entity/PomodoroSessionEntity.kt` | 活动计时五字段 |
| `data/dao/PomodoroSessionDao.kt` | 活动查询与 timer/complete/interrupt |
| `data/repository/PomodoroRepository.kt` / `RoomPomodoroRepository.kt` | 接口与实现对齐 |
| `data/mapper/EntityMapper.kt` | 字段映射 |
| `domain/PomodoroSession.kt` | 字段与完成态时长语义 |
| `viewmodel/PomodoroViewModel.kt` | 先落库、Mutex、阶段/时长修复 |
| `MainActivity.kt` | 冷启动弹窗 |
| `ui/pomodoro/PomodoroActivity.kt` | `PomodoroPhase` 包路径调整 |
| `res/values/strings.xml` | 孤儿会话与阶段文案 |
| `androidTest/.../DatabaseCreationTest.kt` | 实体构造适配 |

***

## 五、明确不做（后置）

| 能力 | 说明 |
|------|------|
| 静默恢复倒计时 | 不根据 `targetEndEpochMs` 自动续跑 |
| 自动跳转番茄钟页 | 冷启动不导航到未完成会话 |
| 用户确认「继续计时」 | #14 后置：确认后带参进计时页接着倒计时 |
| Foreground Service / AlarmManager | 到点通知未规划落地 |
| 正式 Migration | 发布前再补；当前 destructive |

***

## 六、验收对照（相对 #9 / #14）

| 维度 | 本轮结果 |
|------|----------|
| 活动状态落库 | ✅ phase + 目标结束/暂停剩余等写入 Room |
| 会话一致性 | ✅ 重置打断；开新会话前打断其它活动行；Mutex 降竞态 |
| 阶段切换 | ✅ 休息 → FOCUS；仅专注加轮次 |
| 专注时长 | ✅ 墙钟减暂停，无双重扣减；完成态存实际秒 |
| 冷启动 | ✅ 弹窗收尾，不静默续跑 |
| 统计页接入 | ❌ 仍属 Stage D 后续（D2–D7）；口径已定 |
| 静默恢复 / 继续计时 UX | ❌ 后置 Issue |

***

## 七、与 Stage D 总计划的关系

| Stage D 步骤 | 本方案关系 |
|--------------|------------|
| D1 会话落库 | 在 D1 之上扩展为「活动计时持久化 + 收尾」 |
| D2–D7 统计闭环 | 未包含；依赖本方案写出的可信 `COMPLETED` 专注时长 |
| D8 验证 | 领域单测已补；统计侧仍待 D 步推进 |

总计划见：[StageD_番茄钟会话落库与统计闭环计划](StageD_番茄钟会话落库与统计闭环计划.md)。

***

> 本文档是 Issue #14「实际落地方案」的仓库内权威整理，实施细节以代码为准；简版入口保留在 `docs/plan/stageE/番茄钟活动计时持久化.md`。
