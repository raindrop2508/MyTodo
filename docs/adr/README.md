# MyPotato ADR 索引

> 状态：有效
> 最后更新：2026-07-10
> 适用范围：架构决策记录（Architecture Decision Record, ADR）入口
> 目标读者：项目维护者、后续 AI 执行者、架构演进参与者

---

## 一、为什么需要 ADR

当前项目已经形成了不少关键决策，但这些内容分散在规划文档、阶段计划、阶段总结和专题说明中，不利于后续复盘与增量演进。

ADR 的作用是将“为什么这样做”从“做了什么”中拆出来，形成可追溯、可更新的决策记录。

---

## 二、当前建议优先补齐的 ADR 主题

### 1. FakeRepository 先行，Room 后置

- 背景：Stage A 之后需要先打通核心流程，而不希望立即引入完整数据库实体与迁移成本
- 现状来源： [数据流转方案评估](file:///e:/code/MyPotato/docs/stageB/数据流转方案评估.md)

### 2. Repository 接口边界

- 背景：`TaskRepository`、`CategoryRepository`、`PomodoroRepository` 的拆分方式会直接影响 Room 落地与后续扩展
- 现状来源： [B2B3：TaskRepository 接口定义计划](file:///e:/code/MyPotato/docs/stageB/B2B3：TaskRepository%20接口定义计划.md)

### 3. Safe Args 与显式 Intent 并存策略

- 背景：Stage B 已部分启用 Safe Args，但并未完全统一导航发起方式
- 现状来源： [StageB_整体实现总结](file:///e:/code/MyPotato/docs/stageB/StageB_整体实现总结.md)

### 4. 统计页延后策略

- 背景：统计模块的展示内容依赖数据库落地与番茄钟会话数据，过早实现会导致返工
- 现状来源： [项目整体规划文档](file:///e:/code/MyPotato/docs/plan/项目整体规划文档.md)

### 5. Theme Attributes 与深色模式预适配策略

- 背景：项目已完成主题属性化与图标动态着色，但深色模式是否完全上线仍需与实现现状同步判断
- 现状来源： [ui_optimization_and_dark_mode_guide](file:///e:/code/MyPotato/docs/ui_optimization_and_dark_mode_guide.md)

---

## 三、后续新增 ADR 的建议模板

新增 ADR 时建议采用以下结构：

1. 标题：`ADR-000X：<决策主题>`
2. 背景（Context）
3. 备选方案（Options）
4. 最终决策（Decision）
5. 影响与代价（Consequences）
6. 影响范围（Affected Files / Modules）
7. 复盘时机（When to Revisit）

---

## 四、维护约定

- 新增 ADR 时，应在本索引补充一条入口
- 若某项决策已经失效，应在对应 ADR 中明确“废弃原因”，而不是直接删除
- 重大阶段切换时，应复查已有 ADR 是否仍适用
