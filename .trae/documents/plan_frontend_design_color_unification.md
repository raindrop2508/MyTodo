# 统一 APP UI 颜色与风格重构计划

## 1. 目标与范围 (Summary)
根据提供的设计图与用户确认，当前需要将 APP 的核心组件（任务卡片、底部导航栏 Tabbar、FAB 按钮、添加任务弹窗的类型与分类标签等）颜色与样式统一为截图中的极简、高对比度风格。

**修改范围：**
- `colors.xml`: 补充核心品牌色（Indigo）。
- `styles.xml` / `selectors`: 调整 Chip、Card、Tabbar Indicator 的样式与状态颜色。
- `activity_main.xml`: 引入底导选中态背景。
- `item_today_task.xml`: 调整任务卡片与内部“长时/单次”标签样式。
- `bottom_sheet_add_task_placeholder.xml`: 替换为全局统一的极简标签样式。

## 2. 具体修改步骤 (Proposed Changes)

### 2.1 颜色系统更新 (`colors.xml`)
- 新增品牌色 `<color name="brand_indigo">#4F46E5</color>`（用于匹配截图中深蓝紫色的 Icon 与文字）。
- 保留 `<color name="brand_fab_bg">#E8EAF6</color>` 作为极浅的底色（用于 Tabbar Indicator 和 FAB 背景）。

### 2.2 底部导航栏与 FAB 同步
- **`bottom_nav_item_colors.xml`**: 选中状态颜色修改为 `@color/brand_indigo`。
- **`styles.xml`**: 新增 `Widget.MyPotato.BottomNav.ActiveIndicator` 样式，指定 `android:color` 为 `@color/brand_fab_bg`。
- **`activity_main.xml`**: 为 BottomNavigationView 增加 `app:itemActiveIndicatorStyle` 属性。
- **`fragment_today.xml`** / **`fragment_tasks.xml`**: 将 FAB 的 `app:tint` 修改为 `@color/brand_indigo`。

### 2.3 统一极简标签选择器 (Selectors)
根据“选中黑框+黑字+白底，未选中无框+灰字+透明底”的极简规范，修改以下文件：
- **`chip_bg_color_selector.xml`**: 选中为 `card_bg_white`，未选中为 `transparent`。
- **`chip_text_color_selector.xml`**: 选中为 `text_primary`，未选中为 `text_secondary`。
- **`chip_stroke_color_selector.xml`**: 选中为 `text_primary`，未选中为 `transparent`。

### 2.4 底部弹窗标签应用统一风格
- **`styles.xml`**: 新增 `Widget.MyPotato.Button.OutlinedToggle`（继承 OutlinedButton），应用上述三大 selector，使按钮组外观等同于 Chip。
- **`bottom_sheet_add_task_placeholder.xml`**: 
  - 将“单次/长时”按钮 `style` 替换为 `Widget.MyPotato.Button.OutlinedToggle`。
  - 将“分类”Chip 组的 `style` 替换为全局自定义的 `Widget.MyPotato.Chip.Filter`。

### 2.5 任务卡片 UI 还原 (`item_today_task.xml`)
- **卡片外观 (`styles.xml`)**: 修改 `Widget.MyPotato.Card.White`，继承自 `Outlined`，设置 `cardElevation=0dp`, `strokeWidth=1dp`, `strokeColor=@color/outline_soft`。
- **卡片内部标签**: 
  - 新增 `res/drawable/bg_tag_outline.xml` (纯白底色 + 1dp `outline_soft` 灰色描边 + 8dp 圆角)。
  - `item_today_task.xml` 中将“长时/单次”标签 (`tv_type_tag`) 的 `android:background` 设置为该 drawable，并移除原有的 `backgroundTint`，以完美还原截图中的浅灰描边白底效果。

## 3. 验证步骤 (Verification)
1. 编译并运行项目，进入 Today 页面。
2. 观察底部导航栏“今日”是否为浅紫色底+深紫色 Icon。
3. 观察任务卡片是否为无阴影、浅灰描边的扁平设计，“单次”标签是否为浅灰描边。
4. 点击 FAB（浅紫色底+深紫 Icon），打开添加弹窗。
5. 观察弹窗内的“类型”与“分类”选项，是否满足选中时黑框黑字、未选中时无框灰字的极简风格。