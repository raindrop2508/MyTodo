# Android 现代架构与数据存储知识总结

## 1. Room 数据库核心配置 (`AppDatabase`)
- **核心职能**：作为数据库的全局入口，定义了表结构（Entities）、版本号及 DAO 访问接口。
- **设计模式**：采用 **单例模式 (Singleton)**，配合 `@Volatile` 和 `synchronized` 确保线程安全及资源节约。
- **迁移策略**：`fallbackToDestructiveMigration()` 在开发阶段允许直接重建表结构（生产环境需审慎使用）。

## 2. 数据模型与完整性 (`Entity`)
- **索引 (Index)**：
    - **作用**：加速查询（`WHERE`, `ORDER BY`），但会增加写操作成本及存储空间。
    - **联合索引**：适用于多字段组合查询，遵循“最左匹配原则”。
- **外键约束 (ForeignKey)**：
    - **CASCADE (级联)**：父删子随，适用于强依赖关系（如任务与步骤）。
    - **SET_DEFAULT**：父删子归默认值，适用于弱依赖关系（如分类与任务）。

## 3. 数据访问层优化 (`DAO`)
- **一次性操作 (`suspend`)**：用于增、删、改及单次查询，强制在协程中执行，保证主线程安全。
- **响应式流 (`Flow`)**：
    - 不加 `suspend`，返回的是“数据管道”。
    - **自动触发**：数据库变动时，Room 内部的 `InvalidationTracker` 会自动重新查询并推送新结果。
    - **生命周期感知**：配合 UI 生命周期（如 `repeatOnLifecycle`），在后台时自动挂起，销毁时自动掐断。

## 4. 现代 Android 架构 (MVVM + Repository)
- **四层结构**：
    1. **Data Source (Room/云端)**：原始数据源。
    2. **Repository (仓库)**：数据调度中心，负责处理缓存逻辑（SSOT 模式），对 ViewModel 屏蔽来源细节。
    3. **ViewModel**：状态持有者，将原始 Entity 转换为 UI State，作为内存中的数据 Broker。
    4. **UI Layer (Compose/XML)**：纯粹的数据呈现，不处理业务逻辑。
- **单向数据流 (UDF)**：
    - **状态向下流**：`Database -> Repository -> VM -> UI`。
    - **事件向上传**：`UI -> VM -> Repository -> Database`。

## 5. 依赖注入 (Dependency Injection - Hilt)
- **核心价值**：解耦。不需要在每个类里手动 `new` 对象，而是由容器自动注入。
- **应用场景**：
    - **Database/DAO**：作为全局单例注入到 Repository。
    - **Repository**：作为单例注入到 ViewModel。
- **优势**：极大地方便了模块更换和单元测试（可以轻松替换为 Mock 对象）。

## 6. 协程调度器 (Coroutine Dispatchers)
- **Dispatchers.Main**：用于 UI 操作。
- **Dispatchers.IO**：用于数据库读写、网络请求、文件操作。
- **Dispatchers.Default**：用于 CPU 密集型计算（如大型列表排序、图片处理）。
- **最佳实践**：Room 内部会自动处理线程切换，但在 Repository 或 ViewModel 中处理复杂逻辑时，应显式通过 `withContext(Dispatchers.IO)` 确保不阻塞主线程。

## 7. UI 状态建模 (UI State Modeling)
- **LSE 模式**：推荐使用 `Sealed Interface` 或 `data class` 统一定义页面的三种状态：
    - **Loading**：加载中。
    - **Success**：成功（持有数据列表）。
    - **Error**：失败（持有错误信息）。
- **优势**：UI 层只需要一个 `when` 表达式即可处理所有情况，代码逻辑无死角。

## 8. 自动化测试策略
- **单元测试 (Unit Test)**：由于业务逻辑都在 ViewModel 和 Repository 中，且不依赖 Activity 环境，可以使用 JUnit 快速进行逻辑测试。
- **集成测试 (Instrument Test)**：测试 Room 数据库的 SQL 语句是否正确。
- **UI 测试**：使用 Compose Test 检查 UI 组件是否按状态正确显示。

## 9. 跨平台思维对比 (Android vs iOS)
- **架构对等性**：
    - **Android** (Compose + Flow + Room) ≈ **iOS** (SwiftUI + Combine + SwiftData)。
- **语言对等性**：
    - **Kotlin Coroutines** ≈ **Swift async/await**。
    - **Kotlin Data Class** ≈ **Swift Struct**。

## 10. 开发哲学
- **关注点分离**：通过分层使得各模块可独立演进。
- **唯一真相源 (SSOT)**：数据必须有且只有一个权威来源（通常是本地数据库）。
- **可预测性**：单向数据流保证了状态变化有迹可循，降低了复杂页面的调试难度。
