# Stage C Room 数据库实现总结

> 文档版本：v1.0
> 更新日期：2026-07-11
> 适用阶段：MyPotato Stage C 完成后

---

## 一、数据库实现概览

### 1.1 实现状态

Stage C 已全部完成，MyPotato 应用成功从内存 FakeRepository 切换到 Room 持久化存储：

| 步骤 | 内容 | 状态 |
|------|------|------|
| C1 | 定义 Room Entity 实体类 | ✅ |
| C2 | 创建 DAO 接口 | ✅ |
| C3 | 更新 AppDatabase | ✅ |
| C4 | 创建 Entity-Domain 映射工具 | ✅ |
| C5 | 实现 RoomRepository | ✅ |
| C6 | 创建数据初始化工具 | ✅ |
| C7 | 切换 ViewModelFactory 依赖 | ✅ |
| C8 | 验证与测试 | ✅（编译通过） |

### 1.2 数据库架构

```
MyPotato 数据层架构
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│              ViewModel (Flow/StateFlow)                  │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                   Repository Layer                       │
│     TaskRepository / CategoryRepository / ...            │
│           ▲                      ▲                      │
│           │                      │                      │
│  RoomTaskRepository      FakeTaskRepository              │
│  (运行时)                (保留)                          │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      DAO Layer                           │
│     TaskDao / TaskStepDao / CategoryDao / ...            │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      Room Database                       │
│  Database: potato_db (v1)                               │
│  Tables: app_info, task, task_step, category,           │
│          pomodoro_session                               │
└─────────────────────────────────────────────────────────┘
```

---

## 二、Room 中如何创建数据表

### 2.1 创建数据表的三个核心组件

Room 创建数据表需要三个核心组件的配合：

| 组件 | 作用 | 示例文件 |
|------|------|----------|
| **Entity** | 定义表结构（表名、字段、约束） | `TaskEntity.kt` |
| **DAO** | 定义数据访问接口（增删改查） | `TaskDao.kt` |
| **Database** | 注册 Entity 和 DAO，创建数据库实例 | `AppDatabase.kt` |

### 2.2 Step 1：定义 Entity（实体类）

**文件：** [TaskEntity.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/entity/TaskEntity.kt)

```kotlin
@Entity(
    tableName = "task",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index("category_id"),
        Index("status"),
        Index("created_at"),
        Index("is_urgent"),
        Index("is_important")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "content")
    val content: String?,
    
    // ... 其他字段
)
```

**关键注解说明：**

| 注解 | 作用 |
|------|------|
| `@Entity(tableName = "task")` | 标记为数据库实体，指定表名 |
| `@PrimaryKey(autoGenerate = true)` | 主键，自增 |
| `@ColumnInfo(name = "xxx")` | 指定列名，默认使用字段名 |
| `@ForeignKey` | 外键约束，支持级联操作 |
| `@Index` | 创建索引，提升查询性能 |

### 2.3 Step 2：创建 DAO 接口

**文件：** [TaskDao.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/dao/TaskDao.kt)

```kotlin
@Dao
interface TaskDao {
    // 响应式查询：返回 Flow，数据变化自动通知
    @Query("SELECT * FROM task ORDER BY created_at DESC")
    fun getTasks(): Flow<List<TaskEntity>>

    // 单次查询：使用 suspend
    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    // 动态组合查询：支持可选参数
    @Query("""
        SELECT * FROM task 
        WHERE (:categoryId IS NULL OR category_id = :categoryId)
          AND (:status IS NULL OR status = :status)
        ORDER BY created_at DESC
    """)
    fun getTasksByQuery(categoryId: Long?, status: Int?): Flow<List<TaskEntity>>

    // 插入操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    // 更新操作
    @Update
    suspend fun update(task: TaskEntity)

    // 删除操作
    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

**关键注解说明：**

| 注解 | 作用 |
|------|------|
| `@Dao` | 标记为数据访问对象接口 |
| `@Query` | 自定义 SQL 查询，编译时检查语法 |
| `@Insert` | 插入记录，支持冲突策略 |
| `@Update` | 更新记录 |
| `@Delete` | 删除记录 |

### 2.4 Step 3：创建 Database 类

**文件：** [AppDatabase.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/AppDatabase.kt)

```kotlin
@Database(
    entities = [
        AppInfo::class,
        TaskEntity::class,
        TaskStepEntity::class,
        CategoryEntity::class,
        PomodoroSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun taskDao(): TaskDao
    abstract fun taskStepDao(): TaskStepDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "potato_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**关键配置说明：**

| 配置项 | 说明 |
|--------|------|
| `entities` | 注册所有实体类 |
| `version` | 数据库版本号，用于迁移 |
| `exportSchema` | 是否导出 schema（开发阶段可关闭） |
| `fallbackToDestructiveMigration()` | 版本不匹配时销毁重建（开发阶段使用） |

---

## 三、构建接口方式（Repository 模式）

### 3.1 接口定义

**文件：** [TaskRepository.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/repository/TaskRepository.kt)

```kotlin
interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    fun getTasksByQuery(query: TaskQuery): Flow<List<Task>>
    // ... 其他方法
}
```

### 3.2 接口实现（RoomRepository）

**文件：** [RoomTaskRepository.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/repository/RoomTaskRepository.kt)

```kotlin
class RoomTaskRepository(
    private val taskDao: TaskDao,
    private val taskStepDao: TaskStepDao
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getTasks().map { it.toDomainList() }
    }

    override suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override fun getTasksByQuery(query: TaskQuery): Flow<List<Task>> {
        return taskDao.getTasksByQuery(
            categoryId = query.categoryId,
            taskType = query.taskType?.value,
            status = query.status?.value,
            // ...
        ).map { it.toDomainList() }
    }
}
```

### 3.3 Entity-Domain 映射

**文件：** [EntityMapper.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/mapper/EntityMapper.kt)

使用扩展函数实现双向映射，保持领域模型纯净：

```kotlin
fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        title = this.title,
        content = this.content,
        // ...
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        title = this.title,
        content = this.content,
        // ...
    )
}
```

### 3.4 依赖注入（ViewModelFactory）

**文件：** [ViewModelFactory.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/viewmodel/ViewModelFactory.kt)

```kotlin
fun getInstance(context: Context): ViewModelFactory {
    return instance ?: synchronized(this) {
        val database = AppDatabase.getDatabase(context)
        val taskRepo = RoomTaskRepository(database.taskDao(), database.taskStepDao())
        val categoryRepo = RoomCategoryRepository(database.categoryDao(), database.taskDao())
        val pomodoroRepo = RoomPomodoroRepository(database.pomodoroSessionDao())
        ViewModelFactory(taskRepo, categoryRepo, pomodoroRepo, context.applicationContext)
            .also { instance = it }
    }
}
```

---

## 四、Room 与 GreenDao 的区别

### 4.1 核心区别对比

| 维度 | Room | GreenDao |
|------|------|----------|
| **所属生态** | Jetpack 官方库 | 第三方开源库 |
| **代码生成方式** | 编译时注解处理（KSP） | 编译时代码生成（APT） |
| **语言支持** | Kotlin-native（suspend、Flow、Coroutines） | Java 代码生成，Kotlin 需互操作 |
| **实体定义** | 注解方式（`@Entity`） | 注解方式（`@Entity`） |
| **查询方式** | 注解 SQL（`@Query`），编译时检查 | 链式 API + SQL |
| **响应式支持** | 原生支持 Flow/LiveData | 需配合 RxJava |
| **数据库创建** | 抽象类继承 `RoomDatabase` | `DaoMaster` 工厂模式 |
| **迁移策略** | `Migration` 类 + `addMigrations()` | `MigrationHelper` 工具类 |
| **事务管理** | `@Transaction` 注解 | `DaoSession.startTransaction()` |
| **线程安全** | 自动处理（suspend + Flow） | 需手动管理 |
| **社区支持** | Google 维护，文档完善 | 社区维护，更新较慢 |
| **学习曲线** | 较低（与 Jetpack 集成） | 中等 |

### 4.2 代码对比：创建数据表

**Room 方式：**

```kotlin
// Entity
@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String
)

// DAO
@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun getTasks(): Flow<List<TaskEntity>>
    
    @Insert
    suspend fun insert(task: TaskEntity): Long
}

// Database
@Database(entities = [TaskEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
```

**GreenDao 方式：**

```java
// Entity
@Entity
public class TaskEntity {
    @Id(autoincrement = true)
    private Long id;
    private String title;
}

// DAO（自动生成）
public interface TaskDao extends Dao<TaskEntity, Long> {
    List<TaskEntity> loadAll();
    long insert(TaskEntity entity);
}

// 数据库创建
DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "potato_db");
SQLiteDatabase db = helper.getWritableDatabase();
DaoMaster daoMaster = new DaoMaster(db);
DaoSession daoSession = daoMaster.newSession();
TaskDao taskDao = daoSession.getTaskDao();
```

### 4.3 代码对比：查询方式

**Room 方式（响应式）：**

```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM task WHERE status = :status ORDER BY created_at DESC")
    fun getTasksByStatus(status: Int): Flow<List<TaskEntity>>
    
    @Query("""
        SELECT * FROM task 
        WHERE (:keyword IS NULL OR title LIKE '%' || :keyword || '%')
        ORDER BY created_at DESC
    """)
    fun searchTasks(keyword: String?): Flow<List<TaskEntity>>
}
```

**GreenDao 方式：**

```java
// 简单查询
List<TaskEntity> tasks = taskDao.queryBuilder()
    .where(TaskEntityDao.Properties.Status.eq(status))
    .orderDesc(TaskEntityDao.Properties.CreatedAt)
    .list();

// 模糊查询
List<TaskEntity> tasks = taskDao.queryBuilder()
    .where(TaskEntityDao.Properties.Title.like("%" + keyword + "%"))
    .list();
```

### 4.4 选择建议

| 场景 | 推荐方案 |
|------|----------|
| 新项目，使用 Jetpack 组件 | **Room** |
| 需要原生 Flow/LiveData 支持 | **Room** |
| 追求编译时 SQL 检查 | **Room** |
| 已有 GreenDao 项目维护 | **GreenDao** |
| 需要高性能 ORM（缓存优化） | **GreenDao** |

---

## 五、关键技术要点

### 5.1 响应式数据流

Room 查询返回 `Flow<List<Entity>>`，数据变化自动通知上层：

```kotlin
// DAO 层
@Query("SELECT * FROM task ORDER BY created_at DESC")
fun getTasks(): Flow<List<TaskEntity>>

// Repository 层
override fun getTasks(): Flow<List<Task>> {
    return taskDao.getTasks().map { it.toDomainList() }
}

// ViewModel 层
val tasks = taskRepository.getTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
```

### 5.2 外键约束与级联操作

```kotlin
@Entity(
    foreignKeys = [
        // TaskStep 关联 Task，删除 Task 时级联删除步骤
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        ),
        // Task 关联 Category，删除 Category 时 category_id 置为默认值
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ]
)
```

### 5.3 数据初始化

**文件：** [DatabaseInitializer.kt](file:///C:/code/MyTodo/app/src/main/java/com/gordon/mypotato/data/initializer/DatabaseInitializer.kt)

```kotlin
class DatabaseInitializer(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val taskStepDao: TaskStepDao
) {
    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            if (categoryDao.getCategoryCount() == 0) {
                initializeDefaultData()
            }
        }
    }
}
```

**触发时机：** 在 `MyPotatoApp.onCreate()` 中调用

```kotlin
class MyPotatoApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer(
                database.categoryDao(),
                database.taskDao(),
                database.taskStepDao()
            ).initializeIfNeeded()
        }
    }
}
```

### 5.4 枚举类型存储

枚举值存储为 Int，通过 `value` 属性映射：

```kotlin
// 领域层枚举
enum class TaskStatus(val value: Int) {
    TODO(0),
    IN_PROGRESS(1),
    COMPLETED(2)
}

// Entity 层使用 Int 存储
@ColumnInfo(name = "status")
val status: Int

// Repository 层转换
val taskType = query.taskType?.value  // 枚举 → Int
```

---

## 六、文件清单

### 6.1 新增文件

| 文件路径 | 类型 | 说明 |
|----------|------|------|
| `data/entity/TaskEntity.kt` | Entity | 任务实体 |
| `data/entity/TaskStepEntity.kt` | Entity | 步骤实体 |
| `data/entity/CategoryEntity.kt` | Entity | 分类实体 |
| `data/entity/PomodoroSessionEntity.kt` | Entity | 番茄钟会话实体 |
| `data/dao/TaskDao.kt` | DAO | 任务数据访问对象 |
| `data/dao/TaskStepDao.kt` | DAO | 步骤数据访问对象 |
| `data/dao/CategoryDao.kt` | DAO | 分类数据访问对象 |
| `data/dao/PomodoroSessionDao.kt` | DAO | 番茄钟会话数据访问对象 |
| `data/mapper/EntityMapper.kt` | Mapper | Entity-Domain 双向映射 |
| `data/repository/RoomTaskRepository.kt` | Repository | 任务仓储 Room 实现 |
| `data/repository/RoomCategoryRepository.kt` | Repository | 分类仓储 Room 实现 |
| `data/repository/RoomPomodoroRepository.kt` | Repository | 番茄钟仓储 Room 实现 |
| `data/initializer/DatabaseInitializer.kt` | Initializer | 默认数据初始化工具 |
| `MyPotatoApp.kt` | Application | 应用入口，管理数据库和初始化 |

### 6.2 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `data/AppDatabase.kt` | 注册新 Entity、添加 DAO 方法 |
| `viewmodel/ViewModelFactory.kt` | 替换 FakeRepository 为 RoomRepository |
| `AndroidManifest.xml` | 注册自定义 Application |

---

## 七、验证清单

| 验证项 | 状态 |
|--------|------|
| ✅ 项目编译通过 | BUILD SUCCESSFUL |
| ⏳ 数据持久化（新建任务 → 重启应用） | 待设备验证 |
| ⏳ 默认数据自动初始化（首次安装） | 待设备验证 |
| ⏳ 任务列表显示（Today/Tasks 页） | 待设备验证 |
| ⏳ 任务编辑保存后列表刷新 | 待设备验证 |
| ⏳ 删除任务及关联步骤 | 待设备验证 |
| ⏳ 分类删除后关联任务 categoryId 置为 0 | 待设备验证 |