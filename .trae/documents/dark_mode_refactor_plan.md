# 深色模式适配与资源重构计划

## 摘要与方案评估

您提出“将所有颜色以及文字都统一整理至 `res/values/` 并将颜色按照使用场景进行命名”的思路**非常合理且必要**，这是消除硬编码、规范项目资源的第一步。

**有没有更好的方案？**
**有。** 仅仅将颜色按场景命名（如 `@color/text_primary`）然后写死在布局文件中，在切换深色模式时，仍然需要维护两套 `colors.xml`（`values` 和 `values-night`），并且会导致同一种颜色产生大量别名，难以维护。

业界更推荐的**最佳实践**是结合 **Material Design 主题属性（Theme Attributes）** 与 **矢量图动态着色（Icon Tinting）**：

1. **语义化主题属性**：布局文件中不再直接引用 `@color/xxx`，而是引用主题属性 `?attr/colorOnSurface`（代表表面上的文字颜色）、`?attr/colorSurface`（代表卡片背景色）等。系统会根据当前是深色还是浅色模式，自动从 `themes.xml` 中提取对应的实际颜色。
2. **单一矢量图 + 动态着色**：不再区分 `ic_pause_white` 和 `ic_reset_black`。图标文件只需一份（如 `ic_pause.xml`，内部使用纯黑或纯白），在布局中通过 `app:tint="?attr/colorOnSurface"` 或 `app:iconTint="?attr/colorOnPrimary"` 动态赋予颜色。

## 拟议变更（以番茄钟页面为例进行重构）

### 1. 矢量图标重构

* 将带有颜色后缀的图标重命名为通用名称：

  * `ic_play_white_24dp.xml` -> `ic_play_24dp.xml`

  * `ic_pause_white_24dp.xml` -> `ic_pause_24dp.xml`

  * `ic_reset_black_24dp.xml` -> `ic_reset_24dp.xml`

* 图标内部的 `android:fillColor` 统一改为中性色（如 `#FF000000`）。

### 2. 主题与颜色体系完善 (`themes.xml` & `colors.xml`)

* 在 `res/values/colors.xml` 中清理冗余命名，保留基础色板（如 `gray_900`, `white`, `red_500` 等）。

* 在 `res/values/themes.xml` (浅色主题) 中映射属性：

  * `colorSurface` -> 纯白 (卡片背景)

  * `colorOnSurface` -> 深黑 (主文本)

  * `colorOnSurfaceVariant` -> 灰色 (次要文本)

  * `android:colorBackground` -> 浅灰 (页面背景)

* 在 `res/values-night/themes.xml` (深色主题) 中映射属性：

  * `colorSurface` -> 深灰 (暗色卡片背景)

  * `colorOnSurface` -> 纯白 (暗色主文本)

  * 依此类推。

### 3. 布局文件重构 (`activity_pomodoro.xml`)

将现有的硬编码颜色替换为动态属性：

* `android:background="@color/page_bg_light"` -> `?android:attr/colorBackground`

* `app:cardBackgroundColor="@color/card_bg_white"` -> `?attr/colorSurface`

* `android:textColor="@color/text_primary"` -> `?attr/colorOnSurface`

* `android:textColor="@color/text_secondary"` -> `?attr/colorOnSurfaceVariant`

* 按钮图标引入：

  * `app:iconTint="?attr/colorOnPrimary"` (对于实心按钮的白色图标)

  * `app:iconTint="?attr/colorOnSurface"` (对于轮廓按钮的图标)

### 4. 代码逻辑更新 (`PomodoroActivity.kt`)

* 倒计时逻辑中动态切换图标的引用更新为新的无颜色后缀的 drawable ID：

  * `R.drawable.ic_play_24dp`

  * `R.drawable.ic_pause_24dp`

### 5. 字符串整理

* 检查 `activity_pomodoro.xml` 中是否还有遗漏的硬编码中文字符（目前在前一任务中已基本提取至 `strings.xml`，这里做最后的兜底检查）。

## 验证步骤

1. **静态检查**：确认布局文件中不再包含 `@color/text_...` 或硬编码颜色，全部替换为 `?attr/...`。
2. **编译运行**：验证项目能够正常编译，番茄钟页面在浅色模式下 UI 表现与之前一致。
3. **深色模式测试（可选）**：若设备或模拟器切换至深色模式，页面卡片、文字与图标能自动反转颜色，不再出现白底黑字无法看清的情况。

