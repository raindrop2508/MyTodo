# 修复：底部 Tab 菜单未选中项被隐藏（Plan）

## 摘要
- 目标：修复底部 Tabbar 在选中某个 Tab 时，其它 Tab “被完全隐藏”的问题；期望效果为：
  - 4 个 Tab（今天/任务/统计/设置）始终可见；
  - 选中项文字加粗；
  - 未选中项文字不加粗（正常字重），不应被隐藏。
- 范围：仅 UI 表现与样式修复，不引入业务逻辑。

## 现状分析（基于仓库实际内容）
- 底部导航使用 `BottomNavigationView`，布局位于 [activity_main.xml](file:///e:/code/MyPotato/app/src/main/res/layout/activity_main.xml#L21-L28)。
- 底部菜单定义在 [bottom_nav_menu.xml](file:///e:/code/MyPotato/app/src/main/res/menu/bottom_nav_menu.xml#L1-L19)：
  - 仅配置了 `title`，未配置 `icon`。
- 现象推断（与 Material 底部导航默认行为相关）：
  - `BottomNavigationView` 在某些默认模式下会隐藏未选中项的 label（只显示 icon）；
  - 当前菜单没有 icon，因此“未选中项 label 被隐藏 + icon 不存在”会导致未选中项看起来完全消失，只剩下选中项的文本。

## 方案与决策
- 不要求补 icon（因为当前阶段允许“文字或空白”），直接通过组件属性强制 label 始终显示，并关闭水平位移/缩放导致的“只剩一个”的视觉效果。
- 通过 `itemTextAppearanceActive/Inactive` 实现“选中项加粗、未选中项不加粗”。

## 具体改动（文件级）
### 1) 调整 BottomNavigationView 的显示策略
- 修改 [activity_main.xml](file:///e:/code/MyPotato/app/src/main/res/layout/activity_main.xml#L21-L28)
  - 在 `BottomNavigationView` 上新增/调整属性：
    - `app:labelVisibilityMode="labeled"`：强制所有 Tab 始终显示文字标签（label）。
    - `app:itemHorizontalTranslationEnabled="false"`：关闭横向平移/位移（避免选中时挤压其它项）。
    - `app:itemIconSize="0dp"`：当前不提供 icon 时，避免为 icon 预留空间（仅文字导航）。
    - `app:itemTextAppearanceActive="@style/TextAppearance.MyPotato.BottomNav.Active"`：选中项文本样式（加粗）。
    - `app:itemTextAppearanceInactive="@style/TextAppearance.MyPotato.BottomNav.Inactive"`：未选中项文本样式（正常）。

### 2) 新增两个 TextAppearance 样式
- 新增 `app/src/main/res/values/styles.xml`
  - 定义：
    - `TextAppearance.MyPotato.BottomNav.Active`（parent 使用 `TextAppearance.Material3.LabelMedium`，`android:textStyle=bold`）
    - `TextAppearance.MyPotato.BottomNav.Inactive`（parent 使用 `TextAppearance.Material3.LabelMedium`，`android:textStyle=normal`）

## 关键技术点（便于代码审查）
- Material Components（Material Design）：`BottomNavigationView` 的 label 显示策略与交互行为控制。
- Typography（Material 3 TextAppearance）：通过 `itemTextAppearanceActive/Inactive` 控制选中/未选中字重差异。

## 验收标准（手工）
- 启动 App：底部 4 个 Tab 文案始终可见，不会出现“只剩一个按钮/其它按钮消失”。
- 点击任意 Tab：
  - 当前 Tab 的文字加粗；
  - 其它 Tab 文字保持正常字重且可见。

## 验证步骤（执行阶段）
- Android Studio 运行安装后验证上述验收标准。
- 若后续决定补 icon：可去掉 `app:itemIconSize="0dp"` 并在 `bottom_nav_menu.xml` 为每个 item 增加 `android:icon`（另开任务处理）。

