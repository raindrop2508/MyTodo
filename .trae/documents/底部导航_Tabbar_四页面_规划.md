# 底部导航 Tabbar + 四页面（Plan）

## 摘要
- 目标：在现有单 MainActivity 的基础上，落地底部导航（Bottom Navigation / Tabbar），包含 Today / Tasks / Statistics / Settings 四个入口，并实现四个页面之间的跳转（暂用文字占位，不加入业务逻辑）。
- 实现方式：Navigation Component（Jetpack Navigation）+ Fragment + BottomNavigationView（Material Components）。
- UI 约束：不保留顶部 Toolbar（Top App Bar）；页面内容仅展示占位文本。
- 文案策略：默认 `values/strings.xml` 使用简体中文（后续再补全 i18n 目录）。

## 现状分析（基于仓库实际内容）
- 当前是单 MainActivity：MainActivity 使用 ViewBinding/DataBinding 绑定 [MainActivity.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/MainActivity.kt) 与 [activity_main.xml](file:///e:/code/MyPotato/app/src/main/res/layout/activity_main.xml)。
- 当前 `activity_main.xml` 仍是示例 UI（按钮 + TextView + DataBinding 文本），不包含底部导航与 NavHost。
- 依赖已具备：
  - Navigation Component：`androidx.navigation:navigation-fragment-ktx`、`androidx.navigation:navigation-ui-ktx` 已在 [app/build.gradle](file:///e:/code/MyPotato/app/build.gradle) 引入。
  - Material Components：`com.google.android.material:material` 已引入。
- 当前工程尚无 navigation graph（`res/navigation/*` 不存在），也无 bottom menu（`res/menu/*` 不存在）。

## 方案概览
### 页面与导航结构
- 单 Activity（MainActivity）承载：
  - `FragmentContainerView` 作为 `NavHostFragment` 容器
  - `BottomNavigationView` 作为底部 Tabbar
- 4 个 Fragment（占位页面）：
  - TodayFragment
  - TasksFragment
  - StatisticsFragment
  - SettingsFragment

### 资源结构（新增）
- `app/src/main/res/navigation/main_nav_graph.xml`
- `app/src/main/res/menu/bottom_nav_menu.xml`
- `app/src/main/res/layout/fragment_today.xml`（其余 3 个同理）
- `app/src/main/res/values/strings.xml`（新增 tab 文案）

## 具体改动（文件级）
### 1) 替换 Activity 布局为「内容区 + 底部导航」
- 修改 [activity_main.xml](file:///e:/code/MyPotato/app/src/main/res/layout/activity_main.xml)
  - 移除现有示例控件与 DataBinding `<data>` 变量
  - 使用 `ConstraintLayout`（或 `CoordinatorLayout`）承载：
    - `FragmentContainerView`（id：`nav_host_fragment`，`app:navGraph="@navigation/main_nav_graph"`，`app:defaultNavHost="true"`）
    - `BottomNavigationView`（id：`bottom_nav`，`app:menu="@menu/bottom_nav_menu"`）
  - 约束关系：`FragmentContainerView` 顶部贴父容器，底部约束到 `BottomNavigationView` 顶部；`BottomNavigationView` 底部贴父容器。

### 2) 新增底部菜单（4 个 Tab）
- 新增 `app/src/main/res/menu/bottom_nav_menu.xml`
  - 4 个 `<item>`：Today / Tasks / Statistics / Settings
  - 关键点：`item` 的 `android:id` 必须与 navigation graph 中对应 destination 的 id 完全一致（才能被 `setupWithNavController` 正确联动）。
  - 初期不强制配置 icon（用户需求允许仅文字占位）；如后续需要再补充 4 个矢量图标资源。

### 3) 新增 Navigation Graph（4 个 destination）
- 新增 `app/src/main/res/navigation/main_nav_graph.xml`
  - `startDestination` 指向 Today
  - 4 个 `<fragment>` destination，分别绑定到 4 个 Fragment 类
  - 每个 destination 配置 `android:label` 绑定字符串资源（便于后续补充 Toolbar 或无障碍描述）。

### 4) 新增 4 个 Fragment（占位页面）
- 新增 Kotlin 文件（建议路径与包名）：
  - `app/src/main/java/com/gordon/mypotato/ui/today/TodayFragment.kt`
  - `app/src/main/java/com/gordon/mypotato/ui/tasks/TasksFragment.kt`
  - `app/src/main/java/com/gordon/mypotato/ui/statistics/StatisticsFragment.kt`
  - `app/src/main/java/com/gordon/mypotato/ui/settings/SettingsFragment.kt`
- 实现方式：
  - `class TodayFragment : Fragment(R.layout.fragment_today)`（其余同理）
  - 不添加任何业务逻辑，仅承载静态布局。

### 5) 新增 4 个 Fragment 布局（占位文本）
- 新增布局文件：
  - `fragment_today.xml` / `fragment_tasks.xml` / `fragment_statistics.xml` / `fragment_settings.xml`
- 每个布局只放一个居中的 `TextView`，文本引用 string resources（禁止硬编码文案）。

### 6) MainActivity 绑定 BottomNavigationView 与 NavController
- 修改 [MainActivity.kt](file:///e:/code/MyPotato/app/src/main/java/com/gordon/mypotato/MainActivity.kt)
  - 移除 `setSupportActionBar(binding.toolbar)`（因为不保留 Toolbar）
  - 获取 NavController（通过 `NavHostFragment`）
  - 调用 `setupWithNavController` 将 `BottomNavigationView` 与 NavController 绑定，完成：
    - 点击 Tab 切换页面
    - 系统返回键按 Navigation back stack 工作

### 7) 新增字符串资源（Tab 名称）
- 修改 [strings.xml](file:///e:/code/MyPotato/app/src/main/res/values/strings.xml)
  - 新增 4 个 tab 文案（简体中文）：
    - Today：今天
    - Tasks：任务
    - Statistics：统计
    - Settings：设置
  - 同时为 navigation graph 的 `label` 与菜单 `title` 复用这些字符串。

## 关键技术点（便于代码审查）
- Navigation Component（Jetpack Navigation）：NavHostFragment、NavController、Navigation Graph（XML）。
- Navigation UI（Jetpack Navigation UI）：`BottomNavigationView.setupWithNavController(...)`。
- Material Components（Material Design）：BottomNavigationView（Tabbar）。
- ViewBinding：MainActivity 使用 `ActivityMainBinding` 绑定视图。

## 假设与决策
- 决策：采用 Navigation Component + Fragment；不保留 Toolbar；Tab 文案默认简体中文。
- 决策：底部导航先不配置图标（仅文字占位），满足“暂时使用文字或空白”的需求；后续如要对齐 Material3（Material Design 3），再补充矢量图标资源。
- 约束：遵守“不新增注释”的工程约定（除非你明确要求补充注释）。

## 验证步骤（执行阶段）
- 编译验证：`./gradlew :app:assembleDebug`
- 安装运行：启动 App 后应看到底部 4 个 Tab；点击每个 Tab 显示对应页面占位文字。
- 回退验证：从任意 Tab 切换到其它 Tab 后，按系统返回键应按导航栈行为返回（或在根目的地退出）。

