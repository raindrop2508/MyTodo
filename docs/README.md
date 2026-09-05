# MyPotato 文档索引

> 状态：有效  
> 最后更新：2026-09-05  
> 适用范围：`docs/` 全目录导航与阅读指引  
> 目标读者：后续 AI 执行者、项目维护者、新接手开发者

---

## 一、这份索引解决什么问题

本索引用于解决以下问题：

- 新读者不知道先看哪篇；
- 多篇文档同时描述同一主题，但缺少权威来源说明；
- 部分文档属于历史阶段文档，容易被误当作当前现状；
- 后续 AI 执行任务前，缺少稳定、低歧义的文档入口。

---

## 二、推荐阅读顺序

### 1. 新开发者快速上手

1. [项目整体规划文档](plan/项目整体规划文档.md)
2. [项目技术架构与实现说明](项目技术架构与实现说明.md)
3. [StageC_Room数据库实现总结](plan/stageC/StageC_Room数据库实现总结.md)
4. [番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md)（D1 之上的活动会话字段与冷启动收尾）
5. [B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md)

### 2. 后续 AI 执行前建议读取

1. [本索引](README.md)
2. 仓库根目录 [agent.md](../agent.md)（项目简述、分层与编码约定）
3. [项目整体规划文档](plan/项目整体规划文档.md)
4. [项目技术架构与实现说明](项目技术架构与实现说明.md)
5. [glossary.md](glossary.md)
6. 按任务主题补读对应阶段计划 / 总结

---

## 三、当前阶段概览

| 阶段 | 状态 | 说明 |
|------|------|------|
| Stage A | ✅ 完成 | UI 骨架（含部分验收细项未闭合） |
| Stage B | ✅ 完成 | Repository 接口 + ViewModel + Flow/StateFlow 主链路 |
| Stage C | ✅ 完成 | Room 业务实体落地，运行时使用 RoomRepository |
| Stage D | ⏳ 进行中 | D1 会话落库已完成；统计三模块闭环待做 |
| 活动计时持久化 | ✅ 已实现 | 在 D1 之上扩展活动字段 + 冷启动孤儿会话收尾（见 Stage E 专题） |
| Stage E | ⏳ 待启动 | 深色模式、i18n、测试体系等质量加固 |

**当前实现权威口径：**

- 任务 / 步骤 / 分类 / 番茄钟主流程：Room 持久化 + ViewModel 驱动
- 统计页：仍为 Mock 数据，时间维度映射错位等问题仍在
- FakeRepository：已从工程中移除，仅保留历史文档中的概念说明

---

## 四、权威来源矩阵

| 主题 | 权威/首选文档 | 说明 |
|------|---------------|------|
| 项目总体路线 | [项目整体规划文档](plan/项目整体规划文档.md) | 目标、阶段划分、下一步路线 |
| 当前架构与实现 | [项目技术架构与实现说明](项目技术架构与实现说明.md) | 与代码对齐的阶段性总览 |
| Room 数据库实现 | [StageC_Room数据库实现总结](plan/stageC/StageC_Room数据库实现总结.md) | Entity/DAO/Repository/初始化 |
| 番茄钟活动会话 | [番茄钟活动计时持久化](plan/stageE/番茄钟活动计时持久化.md) | DB v3 字段、落库时机、冷启动收尾 |
| Stage D 计划 | [StageD_番茄钟会话落库与统计闭环计划](plan/stageD/StageD_番茄钟会话落库与统计闭环计划.md) | D1–D8；当前 D1 已完成 |
| 领域模型 | [B1：领域数据模型定义](plan/stageB/B1：领域数据模型定义.md) | 字段、枚举、关系口径 |
| Stage B 历史总结 | [StageB_整体实现总结](plan/stageB/StageB_整体实现总结.md) | FakeRepository 时代链路，勿当现状唯一来源 |
| 数据流方案取舍 | [数据流转方案评估](plan/stageB/数据流转方案评估.md) | 为何 Fake 先行再接 Room |
| UI 工程化 | [ui_optimization_and_dark_mode_guide](plan/stageA/ui_optimization_and_dark_mode_guide.md) | 主题属性化、图标着色 |
| UI 视觉参考 | [page_design](plan/stageA/ui-skeleton-android/page_design.md) | 参考稿，不完全等同落地 |

---

## 五、文档状态说明

- `有效`：当前仍可直接作为实施或理解依据
- `有效（含历史背景）`：当前可读，但正文中保留了阶段演进信息
- `参考稿`：用于理解目标方向，不等同于当前真实实现
- `历史文档`：保留阶段上下文，不应直接作为当前状态判断依据

---

## 六、目录说明

### 1. `docs/plan/`

- 项目总纲与各阶段计划 / 总结
- `plan/stageA/`：UI 骨架、主题与视觉参考
- `plan/stageB/`：FakeRepository 时期计划与总结（历史）
- `plan/stageC/`：Room 落地计划与实现总结
- `plan/stageD/`：番茄钟会话落库与统计闭环计划
- `plan/stageE/`：质量加固与活动计时持久化等专题

### 2. 根级专题文档

- `项目技术架构与实现说明.md`：架构与实现总览（优先对照代码）
- `glossary.md`：统一术语
- `adr/README.md`：架构决策索引（模板与待补主题）

---

## 七、术语与决策入口

- 术语表：[glossary.md](glossary.md)
- 决策索引：[adr/README.md](adr/README.md)
- Agent 约定：[agent.md](../agent.md)

---

## 八、维护约定

- 若同一主题出现在多篇文档中，应在本索引中明确「权威来源」
- 若某篇文档与当前实现存在阶段偏差，应优先补「状态」与「适用范围」，而不是直接删除历史内容
- 文档内工程路径优先使用仓库相对路径，如 `app/src/...`、`docs/...`
- 发生阶段切换时，应至少同步更新：
  - `docs/README.md`
  - `docs/plan/项目整体规划文档.md`
  - `docs/项目技术架构与实现说明.md`
  - 对应阶段总结 / 专题文档
  - 必要时同步根目录 `agent.md` 与 `docs/glossary.md`
