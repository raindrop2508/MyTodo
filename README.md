# MyPotato - 番茄钟 TODO 应用

> 一款本地离线优先的 Android 番茄钟 TODO 应用，融合四象限优先级管理与番茄工作法。

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat\&logo=kotlin)
![Material3](https://img.shields.io/badge/Material%20Design-3-757575?style=flat\&logo=materialdesign)
![Room](https://img.shields.io/badge/Room-2.6.1-3DDC84?style=flat\&logo=android)
![minSdk](https://img.shields.io/badge/minSdk-29-3DDC84?style=flat\&logo=android)
![Version](https://img.shields.io/badge/version-1.0.0-blue?style=flat)

***

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [开发环境](#开发环境)
- [构建与运行](#构建与运行)
- [版本历史](#版本历史)
- [后续规划](#后续规划)
- [许可证](#许可证)

***

## 项目简介

MyPotato 是一款\*\*本地离线优先（Local First）\*\*的 Android 原生 TODO 应用，将四象限优先级管理（Eisenhower Matrix）与番茄工作法（Pomodoro Technique）深度融合，帮助用户聚焦真正重要的任务。

### 核心理念

- **离线优先**：所有数据存储在本地 Room 数据库中，无需联网即可使用
- **单一数据源**：Repository 模式确保页面间数据一致性
- **响应式 UI**：Flow + StateFlow 驱动界面自动刷新
- **Material Design 3**：遵循最新的 Material You 设计规范

***

## 功能特性

### ✅ V1.0 已实现

#### 任务管理

- **新建任务**：支持 Today / Tasks 双入口快速创建
- **四象限分类**：紧急 / 重要两个维度组合（紧急且重要、重要、紧急、其他）
- **任务类型**：一次性任务 / 长时任务（支持番茄钟）
- **任务编辑**：标题、描述、备注、类型、分类、优先级均可修改
- **任务完成**：标记完成 / 取消完成，状态即时同步
- **任务删除**：删除确认，级联删除子步骤

#### 步骤管理

- **子步骤 CRUD**：任务下的步骤列表，支持新增、编辑、删除
- **步骤排序**：拖拽排序（编辑态）
- **完成状态**：独立的完成标记与完成时间记录
- **进度展示**：任务详情页显示完成进度（x/y）

#### 分类管理

- **动态分类**：从 Repository 动态生成分类 Chip，无硬编码
- **颜色标识**：分类带颜色标签，便于快速识别
- **默认分类**：内置初始分类，首次启动自动初始化

#### 番茄钟

- **倒计时**：默认 25 分钟专注时长，可自定义
- **状态机**：IDLE / RUNNING / PAUSED / COMPLETED 完整状态流转
- **暂停/继续**：支持暂停与继续，暂停时长不计入专注时间
- **手动结束**：可提前结束当前番茄钟
- **短休息 / 长休息**：完整的工作-休息循环机制
- **会话持久化**：番茄钟会话存入 Room 数据库，重启不丢失
- **声音提醒**：完成时播放提示音（可开关）

#### 数据持久化

- **Room 数据库**：4 张业务表（Task / TaskStep / Category / PomodoroSession）
- **Repository 模式**：统一数据访问入口，屏蔽底层实现
- **响应式数据流**：Flow / StateFlow 驱动 UI 自动刷新

#### 设置页

- **番茄钟参数自定义**：工作时长、短休息、长休息、长休息间隔
- **声音开关**：番茄钟完成提示音开关
- **版本信息**：显示当前应用版本号

### 🚧 规划中（后续版本）

- 统计页面（时间段分布、完成率、类别分析）
- 深色模式适配
- 多语言支持（简体中文 / 英文）
- 数据导入导出（JSON / CSV）
- 任务提醒与通知
- 桌面小组件

***

## 技术架构

### 整体架构

采用 Google 推荐的 **单 Activity + 多 Fragment + MVVM** 架构：

```
┌─────────────────────────────────────────────┐
│            UI 层（Fragment/Activity）        │
│  Today / Tasks / Detail / Edit / Settings /  │
│  Pomodoro                                   │
└──────────────────┬──────────────────────────┘
                   │ 依赖
┌──────────────────▼──────────────────────────┐
│             ViewModel 层                     │
│  状态持有 + StateFlow 暴露 + 业务逻辑         │
└──────────────────┬──────────────────────────┘
                   │ 依赖
┌──────────────────▼──────────────────────────┐
│           Repository 层（接口）               │
│  统一数据访问入口，屏蔽底层数据来源            │
└──────────────────┬──────────────────────────┘
                   │ 实现
┌──────────────────▼──────────────────────────┐
│         Room 数据层                          │
│  Entity / DAO / Database / Mapper            │
└─────────────────────────────────────────────┘
```

### 技术栈

| 技术领域      | 选型                                 | 版本         |
| --------- | ---------------------------------- | ---------- |
| 开发语言      | Kotlin                             | 2.0.21     |
| UI 框架     | ViewBinding + XML + Material3      | 1.13.0     |
| 导航        | Jetpack Navigation Component       | 2.9.5      |
| 架构模式      | MVVM + Repository                  | -          |
| 数据持久化     | Room                               | 2.6.1      |
| 异步        | Kotlin Coroutines + Flow/StateFlow | -          |
| 生命周期      | AndroidX Lifecycle                 | 2.8.7      |
| 构建工具      | Gradle + Version Catalog           | AGP 8.12.3 |
| 最低 SDK    | API 29（Android 10）                 | -          |
| 目标/编译 SDK | API 36                             | -          |

### 核心设计模式

1. **Repository 模式**：统一数据访问入口，Fake → Room 无缝切换
2. **MVVM 架构**：View 无状态，ViewModel 驱动 UI
3. **状态机模式**：番茄钟会话状态流转清晰可控
4. **单例模式**：Room 数据库 Double-Checked Locking 懒加载
5. **适配器模式**：RecyclerView Adapter + ViewBinding

***

## 项目结构

```
MyPotato/
├── app/                                    # 应用主模块
│   └── src/main/
│       ├── java/com/gordon/mypotato/
│       │   ├── MainActivity.kt             # 主 Activity（导航容器）
│       │   ├── MyPotatoApp.kt              # Application 类
│       │   ├── data/                       # 数据层
│       │   │   ├── AppDatabase.kt          # Room 数据库
│       │   │   ├── dao/                    # DAO 接口
│       │   │   ├── entity/                 # Room 实体
│       │   │   ├── mapper/                 # Entity ↔ Domain 映射
│       │   │   ├── repository/             # Repository 实现
│       │   │   └── initializer/            # 数据库初始化
│       │   ├── domain/                     # 领域模型（纯 Kotlin）
│       │   ├── ui/                         # UI 层
│       │   │   ├── today/                  # 今日页
│       │   │   ├── tasks/                  # 任务列表/详情/编辑
│       │   │   ├── pomodoro/               # 番茄钟页
│       │   │   ├── settings/               # 设置页
│       │   │   ├── statistics/             # 统计页（暂不开放入口）
│       │   │   └── common/                 # 公共 UI 组件
│       │   └── viewmodel/                  # ViewModel 层
│       └── res/                            # 资源文件
│           ├── layout/                     # 布局文件
│           ├── menu/                       # 菜单资源
│           ├── navigation/                 # 导航图
│           ├── drawable/                   # 矢量图与选择器
│           ├── values/                     # 字符串、颜色、尺寸、主题
│           └── values-night/               # 深色模式主题（待填充）
├── docs/                                   # 项目文档
│   ├── plan/                               # 各阶段规划文档
│   └── stageB/ / stageC/ / stageD/         # 各阶段总结与设计
├── gradle/
│   └── libs.versions.toml                  # Version Catalog 依赖管理
├── build.gradle                            # 根构建脚本
├── settings.gradle                         # 项目设置
└── README.md                               # 本文件
```

***

## 开发环境

### 必备工具

- **Android Studio**：最新稳定版（建议 Hedgehog 及以上）
- **JDK**：11（AGP 8.x 要求）
- **Android SDK**：
  - compileSdk = 36
  - minSdk = 29
  - targetSdk = 36

### 推荐配置

- 真机或模拟器运行 Android 10（API 29）及以上
- 至少 8GB 内存（推荐 16GB）
- 启用 ADB 调试（真机调试时）

***

## 构建与运行

### 1. 克隆项目

```bash
git clone <repository-url>
cd MyTodo
```

### 2. 打开项目

1. 启动 Android Studio
2. 选择 **Open an Existing Project**
3. 选择 `MyTodo` 项目根目录
4. 等待 Gradle 同步完成（首次打开可能需要几分钟下载依赖）

### 3. 配置设备

- **模拟器**：通过 AVD Manager 创建 Android 10+ 模拟器
- **真机**：连接 Android 手机，开启「开发者选项」和「USB 调试」

### 4. 运行应用

1. 在工具栏选择目标设备
2. 点击 **Run** 按钮（▶️）或使用快捷键 `Shift + F10`
3. 等待应用安装并启动

### 5. 构建 Release 包

```bash
# 编译 Release APK
./gradlew assembleRelease

# 输出位置：app/build/outputs/apk/release/
```

> **注意**：当前 Release 构建未配置签名密钥，如需发布请自行配置签名。

***

## 版本历史

### v1.0.0（当前版本）

**发布日期**：2026-07

**核心功能**：

- 任务管理完整 CRUD（新建 / 编辑 / 完成 / 删除）
- 四象限优先级分类（紧急 / 重要矩阵）
- 一次性任务 / 长时任务双类型
- 子步骤管理（新增 / 编辑 / 删除 / 完成）
- 番茄钟完整功能（倒计时 / 暂停 / 继续 / 手动结束）
- 番茄钟会话持久化（Room 数据库）
- 分类管理（动态 Chip 生成）
- 设置页（番茄钟参数自定义、声音开关）
- Room 数据持久化，应用重启数据不丢失

**架构特点**：

- 单 Activity + 多 Fragment + MVVM
- Repository 模式 + Room 实现
- Flow/StateFlow 响应式数据流
- Material Design 3 主题系统

***

## 后续规划

详见项目内 `docs/plan/` 目录下的规划文档。整体路线：

| 阶段      | 目标                       | 状态            |
| ------- | ------------------------ | ------------- |
| Stage A | MVP UI 骨架 + Material3 主题 | ✅ 完成          |
| Stage B | 核心流程跑通 + FakeRepository  | ✅ 完成          |
| Stage C | Room 数据结构落地 + 持久化        | ✅ 完成          |
| Stage D | 番茄钟会话落库 + 统计闭环           | 🔄 进行中（D1 完成） |
| Stage E | 深色模式 + i18n + 测试补齐       | ⏳ 待启动         |

***

## 许可证

待补充（建议选择 MIT 或 Apache 2.0）

***

> Made with Gordon Mark using Kotlin & Material Design 3

