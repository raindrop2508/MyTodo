# Stage C：轻量数据结构落地（接入 Room）开发计划

> 文档版本：v1.1\
> 更新日期：2026-07-11\
> 适用阶段：MyPotato Stage C\
> 前置条件：Stage B 已完成，Repository 接口 + FakeRepository + ViewModel + Flow/StateFlow 链路已建立\
> 当前进度：步骤 C1-C6 已完成，步骤 C7-C8 待实施

***

## 一、阶段目标

### 1.1 核心目标

在不改变现有 Repository 接口边界的前提下，引入 Room 业务实体与 RoomRepository，替换当前的 FakeRepository，实现数据持久化。

### 1.2 目标产出

| 目标    | 说明                        |
| ----- | ------------------------- |
| 数据持久化 | 应用重启后业务数据不丢失              |
| 接口不变  | Repository 接口保持不变，上层无感知切换 |
| 响应式刷新 | 沿用现有 Flow/StateFlow 响应式链路 |
| 数据初始化 | 默认分类与演示数据自动初始化            |

***

## 二、实施步骤

### 步骤 C1：定义 Room Entity 实体类 ✅

**文件清单：**

- `data/entity/TaskEntity.kt` - ✅ 创建完成，外键关联 Category，`category_id` 默认值为 0
- `data/entity/TaskStepEntity.kt` - ✅ 创建完成，外键级联删除 Task
- `data/entity/CategoryEntity.kt` - ✅ 创建完成，名称唯一索引
- `data/entity/PomodoroSessionEntity.kt` - ✅ 创建完成，外键关联 Task 和 TaskStep

**设计要点：**

- 表名：小写 + 下划线命名
- 主键：`@PrimaryKey(autoGenerate = true)`
- 外键：`@ForeignKey(onDelete = CASCADE)` 级联删除，`task.category_id` 使用 `SET_DEFAULT`
- 索引：为常用查询字段创建索引（`category_id`, `status`, `created_at` 等）
- 类型映射：枚举值存储为 Int，时间戳存储为 Long（秒）

### 步骤 C2：创建 DAO 接口 ✅

**文件清单：**

- `data/dao/TaskDao.kt` - ✅ 创建完成，支持动态组合查询
- `data/dao/TaskStepDao.kt` - ✅ 创建完成，支持步骤耗时统计
- `data/dao/CategoryDao.kt` - ✅ 创建完成，支持分类数量查询
- `data/dao/PomodoroSessionDao.kt` - ✅ 创建完成，支持会话状态更新

**设计要点：**

- 查询方法返回 `Flow<List<Entity>>`，支持响应式数据变更通知
- 单次查询和写操作使用 `suspend`
- 动态 SQL 查询使用 `@Query` 注解，支持可选参数
- 外键级联策略在 Entity 中定义

### 步骤 C3：更新 AppDatabase ✅

**修改文件：** `data/AppDatabase.kt`

**变更内容：**

- 更新 `@Database` 注解，添加新 Entity（TaskEntity, TaskStepEntity, CategoryEntity, PomodoroSessionEntity）
- 版本号保持为 1（开发阶段使用 `fallbackToDestructiveMigration()`）
- 添加 DAO 抽象方法：`taskDao()`, `taskStepDao()`, `categoryDao()`, `pomodoroSessionDao()`
- 保持 `fallbackToDestructiveMigration()` 策略

### 步骤 C4：创建 Entity-Domain 映射工具 ✅

**文件清单：**

- `data/mapper/EntityMapper.kt` - ✅ 创建完成，使用扩展函数实现双向映射

**设计要点：**

- 使用扩展函数实现双向映射（Entity ↔ Domain）
- 保持领域模型纯净，不引入 Room 依赖
- 映射逻辑集中管理，便于维护
- 使用 `@JvmName` 注解解决 JVM 类型擦除冲突

### 步骤 C5：实现 RoomRepository ✅

**文件清单：**

- `data/repository/RoomTaskRepository.kt` - ✅ 创建完成，实现 TaskRepository 接口
- `data/repository/RoomCategoryRepository.kt` - ✅ 创建完成，实现 CategoryRepository 接口
- `data/repository/RoomPomodoroRepository.kt` - ✅ 创建完成，实现 PomodoroRepository 接口

**设计要点：**

- 严格实现 Repository 接口契约
- Flow 转换：`Flow<List<Entity>>` → `Flow<List<Domain>>`
- 枚举映射：接口层使用枚举，实现层负责枚举↔Int
- 任务完成耗时计算：取步骤耗时总和与创建到完成时间差的最大值

### 步骤 C6：创建数据初始化工具 ✅

**文件清单：**

- `data/initializer/DatabaseInitializer.kt` - ✅ 创建完成

**初始化内容：**

- 默认分类：学习、工作、生活、健康、购物（5条）
- 演示任务：6 条示例任务（与 FakeTaskRepository 一致）
- 演示步骤：关联步骤数据（8条）

**初始化策略：**

- 检查分类表是否为空（`getCategoryCount() == 0`），为空则执行初始化
- 使用 `withContext(Dispatchers.IO)` 确保在 IO 线程执行

### 步骤 C7：切换 ViewModelFactory 依赖 ⏳

**修改文件：** `viewmodel/ViewModelFactory.kt`

**变更内容：**

- 添加 `AppDatabase` 依赖
- 创建 DAO 实例
- 创建 RoomRepository 实例
- 替换所有 FakeRepository 引用

### 步骤 C8：验证与测试 ⏳

**验证清单：**

| 验证项   | 操作            | 预期结果                    |
| ----- | ------------- | ----------------------- |
| 数据持久化 | 新建任务 → 重启应用   | 任务仍存在                   |
| 任务列表  | Today/Tasks 页 | 显示数据库中的任务               |
| 任务详情  | 点击任务进入详情      | 显示完整任务信息                |
| 任务编辑  | 编辑任务保存        | 列表和详情页同步更新              |
| 标记完成  | 勾选任务          | 状态即时更新                  |
| 删除任务  | 删除任务          | 任务及关联步骤被删除              |
| 步骤管理  | 添加/编辑/删除步骤    | 步骤列表正确更新                |
| 分类管理  | 创建/删除分类       | 删除后关联任务 categoryId 置为 0 |
| 默认数据  | 首次安装          | 默认分类和演示数据自动初始化          |

***

## 三、文件变更清单

### 新增文件

| 文件路径                                        | 类型          | 说明                 |
| ------------------------------------------- | ----------- | ------------------ |
| `data/entity/TaskEntity.kt`                 | Entity      | 任务实体               |
| `data/entity/TaskStepEntity.kt`             | Entity      | 步骤实体               |
| `data/entity/CategoryEntity.kt`             | Entity      | 分类实体               |
| `data/entity/PomodoroSessionEntity.kt`      | Entity      | 番茄钟会话实体            |
| `data/dao/TaskDao.kt`                       | DAO         | 任务数据访问对象           |
| `data/dao/TaskStepDao.kt`                   | DAO         | 步骤数据访问对象           |
| `data/dao/CategoryDao.kt`                   | DAO         | 分类数据访问对象           |
| `data/dao/PomodoroSessionDao.kt`            | DAO         | 番茄钟会话数据访问对象        |
| `data/mapper/EntityMapper.kt`               | Mapper      | Entity-Domain 映射工具 |
| `data/repository/RoomTaskRepository.kt`     | Repository  | 任务仓储 Room 实现       |
| `data/repository/RoomCategoryRepository.kt` | Repository  | 分类仓储 Room 实现       |
| `data/repository/RoomPomodoroRepository.kt` | Repository  | 番茄钟仓储 Room 实现      |
| `data/initializer/DatabaseInitializer.kt`   | Initializer | 数据库初始化工具           |

### 修改文件

| 文件路径                            | 修改内容                               |
| ------------------------------- | ---------------------------------- |
| `data/AppDatabase.kt`           | 注册新 Entity、添加 DAO 方法、更新版本号         |
| `viewmodel/ViewModelFactory.kt` | 替换 FakeRepository 为 RoomRepository |

***

## 四、风险与对策

| 风险                  | 对策                                     |
| ------------------- | -------------------------------------- |
| Entity-Domain 映射不一致 | 编写映射单元测试                               |
| 外键级联策略错误            | 正确配置 `@ForeignKey(onDelete = CASCADE)` |
| 数据库版本迁移失败           | 更新版本号至 2                               |
| 默认数据初始化重复           | 检查分类表是否为空                              |
| 并发写入冲突              | 使用 Room 事务                             |
| 查询性能问题              | 为常用字段创建索引                              |

***

## 五、验收标准

| 验收项                                   | 状态     | 备注                                   |
| ------------------------------------- | ------ | ------------------------------------ |
| ✅ 4 个 Entity 类创建完成                    | 已完成    | TaskEntity, TaskStepEntity, CategoryEntity, PomodoroSessionEntity |
| ✅ 4 个 DAO 接口创建完成                      | 已完成    | TaskDao, TaskStepDao, CategoryDao, PomodoroSessionDao |
| ✅ EntityMapper 创建完成                   | 已完成    | 双向映射扩展函数，使用 @JvmName 解决类型擦除冲突     |
| ✅ AppDatabase 注册新表                    | 已完成    | 添加 5 张表注册，版本号保持为 1（开发阶段）           |
| ✅ RoomTaskRepository 实现所有接口方法         | 已完成    | 实现 TaskRepository 全部 14 个方法              |
| ✅ RoomCategoryRepository 实现所有接口方法     | 已完成    | 实现 CategoryRepository 全部 5 个方法            |
| ✅ RoomPomodoroRepository 实现所有接口方法     | 已完成    | 实现 PomodoroRepository 全部 6 个方法            |
| ✅ DatabaseInitializer 创建完成            | 已完成    | 默认分类 5 条、演示任务 6 条、关联步骤 8 条        |
| ⏳ ViewModelFactory 切换为 RoomRepository | 待实施    | 步骤 C7：替换 FakeRepository 为 RoomRepository      |
| ⏳ 应用启动后自动初始化默认数据                      | 待实施    | 需在 Application 或 MainActivity 中调用初始化         |
| ⏳ 新建任务后重启应用数据不丢失                      | 待验证    | 需步骤 C7 完成后验证                        |
| ⏳ 三条主流程正常工作                           | 待验证    | 需步骤 C7 完成后验证                        |
| ⏳ 分类删除后关联任务 categoryId 置为 0           | 待验证    | 需步骤 C7 完成后验证                        |
| ✅ 项目编译通过                              | 已完成    | BUILD SUCCESSFUL，无编译错误                |
| ✅ 数据库表结构验证测试                       | 已完成    | 新增 DatabaseCreationTest 仪器化测试           | |

***

## 六、技术要点

- **Room Entity**：`@Entity`、`@PrimaryKey`、`@ForeignKey`、`@Index`
- **Room DAO**：`@Dao`、`@Query`、`@Insert`、`@Update`、`@Delete`
- **Room Database**：`@Database`、版本管理、迁移策略
- **Entity-Domain 映射**：扩展函数双向转换
- **Flow 响应式**：Room 查询返回 Flow，数据变更自动通知
- **事务管理**：`@Transaction` 保证原子性
- **依赖注入**：ViewModelFactory 集中管理依赖

