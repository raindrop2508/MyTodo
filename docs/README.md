# MyPotato 文档索引

> 状态：有效
> 最后更新：2026-07-10
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

1. [项目整体规划文档](file:///c:/code/MyTodo/docs/plan/项目整体规划文档.md)
2. [StageB_整体实现总结](file:///c:/code/MyTodo/docs/stageB/StageB_整体实现总结.md)
3. [项目技术架构与实现说明](file:///c:/code/MyTodo/docs/项目技术架构与实现说明.md)
4. [B1：领域数据模型定义](file:///c:/code/MyTodo/docs/stageB/B1：领域数据模型定义.md)
5. [StageB_核心流程跑通计划](file:///c:/code/MyTodo/docs/stageB/StageB_核心流程跑通计划.md)

### 2. 后续 AI 执行前建议读取

1. [本索引](file:///c:/code/MyTodo/docs/README.md)
2. [项目整体规划文档](file:///c:/code/MyTodo/docs/plan/项目整体规划文档.md)
3. [StageB_整体实现总结](file:///c:/code/MyTodo/docs/stageB/StageB_整体实现总结.md)
4. [B1：领域数据模型定义](file:///c:/code/MyTodo/docs/stageB/B1：领域数据模型定义.md)
5. 按任务主题补读对应专题文档

---

## 三、当前阶段概览

- Stage A：UI 骨架已完成
- Stage B：主体已完成，已形成 `Repository + ViewModel + Flow/StateFlow + UI` 的主数据链路
- Stage C：已完成，业务实体已接入 Room，数据已持久化，默认数据自动初始化
- 统计页、深色模式完整上线、多语言与测试体系仍属于后续迭代范围

当前实现现状请优先以 [StageB_整体实现总结](file:///c:/code/MyTodo/docs/stageB/StageB_整体实现总结.md) 和 [StageC_Room数据库实现总结](file:///c:/code/MyTodo/docs/plan/stageC/StageC_Room数据库实现总结.md) 为准。

---

## 四、权威来源矩阵

| 主题 | 权威/首选文档 | 说明 |
|------|---------------|------|
| 项目总体路线 | [项目整体规划文档](file:///c:/code/MyTodo/docs/plan/项目整体规划文档.md) | 用于理解目标、阶段划分、下一步路线 |
| 当前实现现状 | [StageB_整体实现总结](file:///c:/code/MyTodo/docs/stageB/StageB_整体实现总结.md) | 反映 Stage B 后的真实架构状态 |
| Room 数据库实现 | [StageC_Room数据库实现总结](file:///c:/code/MyTodo/docs/plan/stageC/StageC_Room数据库实现总结.md) | Stage C Room 集成实现总结，包含与 GreenDao 对比 |
| 领域模型 | [B1：领域数据模型定义](file:///c:/code/MyTodo/docs/stageB/B1：领域数据模型定义.md) | 字段、枚举、关系的权威口径 |
| Stage B 执行细节 | [StageB_核心流程跑通计划](file:///c:/code/MyTodo/docs/stageB/StageB_核心流程跑通计划.md) | 适合追溯 B1-B9 的计划与偏差 |
| Stage C 执行细节 | [StageC_轻量数据结构落地计划](file:///c:/code/MyTodo/docs/plan/stageC/StageC_轻量数据结构落地计划.md) | Stage C 实施计划与验收标准 |
| 数据流方案取舍 | [数据流转方案评估](file:///c:/code/MyTodo/docs/stageB/数据流转方案评估.md) | 解释为什么先走 FakeRepository，再接 Room |
| UI 工程化与深色模式预适配 | [ui_optimization_and_dark_mode_guide](file:///c:/code/MyTodo/docs/ui_optimization_and_dark_mode_guide.md) | 主题属性化、图标着色、字符串资源化 |
| UI 视觉设计参考 | [page_design](file:///c:/code/MyTodo/docs/ui-skeleton-android/page_design.md) | 参考稿，不完全等同于当前落地实现 |

---

## 五、文档状态说明

- `有效`：当前仍可直接作为实施或理解依据
- `有效（含历史背景）`：当前可读，但正文中保留了阶段演进信息
- `参考稿`：用于理解目标方向，不等同于当前真实实现
- `历史文档`：保留阶段上下文，不应直接作为当前状态判断依据

---

## 六、目录说明

### 1. `docs/plan/`

- 存放项目总纲、里程碑与阶段路线图

### 2. `docs/stageB/`

- 存放 Stage B 的计划、总结、数据流方案与领域模型定义
- 若需要理解当前业务数据链路，优先阅读本目录

### 3. `docs/ui_skeleton/`

- 存放 Stage A 时期的需求大纲、UI 骨架方案与验收清单
- 主要价值是保留产品初始目标与阶段验收上下文

### 4. `docs/ui-skeleton-android/`

- 存放 UI 视觉/交互参考稿和外部原型映射资料
- 该目录内容偏参考，不一定完全代表当前代码实现

### 5. 根级专题文档

- `ui_optimization_and_dark_mode_guide.md`：UI 工程化专题
- `Material_Design_3_Elevation_Overlay_技术详解.md`：Material 3 Elevation Overlay 问题专题
- `项目技术架构与实现说明.md`：项目架构与实现的阶段性总览

---

## 七、术语与决策入口

- 术语表： [glossary.md](file:///c:/code/MyTodo/docs/glossary.md)
- 决策索引： [adr/README.md](file:///c:/code/MyTodo/docs/adr/README.md)

---

## 八、维护约定

- 若同一主题出现在多篇文档中，应在本索引中明确“权威来源”
- 若某篇文档与当前实现存在阶段偏差，应优先补“状态”与“适用范围”，而不是直接删除历史内容
- 文档内工程路径优先使用仓库相对路径，如 `app/src/...`、`docs/...`
- 发生阶段切换时，应至少同步更新：
  - `docs/README.md`
  - `docs/plan/项目整体规划文档.md`
  - 对应阶段总结文档
