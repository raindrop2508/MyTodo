# UI 优化与深色模式预适配技术文档

> 状态：有效（技术专题）
> 最后更新：2026-07-10
> 适用范围：UI 主题属性化、图标动态着色、字符串资源化与深色模式预适配
> 说明：本文聚焦工程化改造结果与后续深色模式落地方法，不代表深色模式已在当前版本完全上线；当前阶段状态请结合 `docs/plan/项目整体规划文档.md` 与 `docs/stageB/StageB_整体实现总结.md` 一并判断

## 一、 背景与目的

为了提升代码的灵活性与可维护性，同时为未来适配深色模式（Dark Mode）做好底层架构铺垫，本项目对全局的 UI 资源进行了深度的重构与优化。本次优化的核心思想是：**消除硬编码，实现颜色、图标与具体页面的解耦**，全面接入 Material Design 的主题属性（Theme Attributes）系统。

## 二、 核心重构点总结

### 1. 颜色硬编码替换为主题属性（Theme Attributes）
以往的布局文件中直接引用了静态的颜色值（例如 `@color/text_primary`、`@color/page_bg_light`），这导致在深色模式下无法自动切换颜色。
- **改动方案**：在 `res/values/themes.xml` 中定义了一套语义化的颜色属性映射。在所有的布局文件（Layout）、颜色状态列表（ColorStateList）以及形状背景（Shape Drawable）中，将 `@color/xxx` 统一替换为了 `?attr/xxx`。
- **涉及的核心属性**：
  - `?android:attr/colorBackground`：页面全局背景色。
  - `?attr/colorSurface`：卡片、对话框等表面背景色。
  - `?attr/colorOnSurface`：表面上的主要文本或图标颜色。
  - `?attr/colorOnSurfaceVariant`：表面上的次要文本或图标颜色。
  - `?attr/colorPrimary`：品牌主色调。
  - `?attr/colorOutline` / `?attr/colorOutlineVariant`：边框及分割线颜色。

### 2. 矢量图动态着色（Icon Tinting）
此前，不同的颜色状态需要维护多份矢量图标（例如 `ic_play_white_24dp`、`ic_reset_black_24dp`），导致资源冗余且难以适配深色模式。
- **改动方案**：
  - 将所有基础矢量图（Vector Drawable）内部的 `android:fillColor` 统一修改为占位纯黑色（`#FF000000`）。
  - 在布局 XML 中，通过 `ImageView` 的 `app:tint` 属性或 `TextView` 的 `android:drawableTint` 属性，结合上述的主题属性动态为图标上色。
- **优势**：同一个图标可以在不同场景、不同主题下复用，极大精简了资源包体积。

### 3. 彻底消除中文字符串硬编码
- **改动方案**：全局扫描了所有布局文件（如 `fragment_tasks.xml`、`fragment_settings.xml` 等），将直接写死在 `android:text` 或 `android:hint` 中的中文提取到了 `res/values/strings.xml` 中。
- **优势**：满足了 Android 官方的开发规范，同时为未来的国际化（多语言适配）扫清了障碍。

---

## 三、 后续深色模式（Dark Mode）适配操作指南

得益于本次基于主题属性（Theme Attributes）的全面重构，当前项目在架构上已完全具备了支持深色模式的条件。**后续如果需要正式适配深色模式，无需再逐一修改任何布局或图标文件，只需遵循以下步骤进行颜色配置即可：**

### 第一步：定位深色模式主题文件
在项目中打开深色模式的专属主题配置文件：
`app/src/main/res/values-night/themes.xml`

### 第二步：定义深色模式下的颜色映射
在 `<style name="Base.Theme.MyPotato" parent="Theme.Material3.DayNight.NoActionBar">` 中，覆写在浅色模式中用到的主题属性。你需要结合设计稿，将这些语义化的颜色赋值为深色模式下的色值。

**示例：**
```xml
<!-- res/values-night/themes.xml -->
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.MyPotato" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- 基础背景色：浅色模式下可能是纯白或亮灰，深色模式下通常为深灰（如 #121212） -->
        <item name="android:colorBackground">@color/dark_page_bg</item>
        
        <!-- 表面颜色（卡片等）：深色模式下通常比背景色略亮（如 #1E1E1E） -->
        <item name="colorSurface">@color/dark_card_bg</item>
        
        <!-- 主要文本颜色：浅色模式下是黑色，深色模式下必须反转为白色或浅灰 -->
        <item name="colorOnSurface">@color/dark_text_primary</item>
        
        <!-- 次要文本/图标颜色：深色模式下的中灰色 -->
        <item name="colorOnSurfaceVariant">@color/dark_text_secondary</item>
        
        <!-- 品牌主色：深色模式下通常会降低饱和度以减少刺眼感 -->
        <item name="colorPrimary">@color/dark_brand_indigo</item>
        <item name="colorPrimaryContainer">@color/dark_brand_fab_bg</item>
        <item name="colorOnPrimary">@color/dark_text_on_primary</item>
        
        <!-- 边框/分割线颜色 -->
        <item name="colorOutline">@color/dark_outline_strong</item>
        <item name="colorOutlineVariant">@color/dark_outline_soft</item>
        
        <!-- 错误状态颜色 -->
        <item name="colorError">@color/dark_state_error_red</item>
    </style>
</resources>
```

*(注意：你需要在 `res/values/colors.xml` 中预先定义好深色专用的 `@color/...` 基础色板，如 `<color name="dark_page_bg">#121212</color>`)*

### 第三步：处理特殊的自定义 View 或极个别硬编码（如果有）
如果在深色模式的测试中发现某些界面的颜色依然刺眼或不协调，大概率是因为：
1. 代码动态计算颜色时使用了写死的色值（而不是从 Context 中读取 Theme 属性）。
2. 使用了带有背景色的图片（PNG/JPG 等），深色模式下可能需要提供 `drawable-night` 目录下的深色版图片。
3. 透明度（Alpha）叠加在深色背景上的效果与浅色不同，需要微调。

### 第四步：验证与测试
1. 在 Android 10（API 29）及以上的设备或模拟器中运行应用。
2. 通过下拉系统状态栏，手动切换系统的“深色主题（Dark Theme）”开关。
3. 应用会自动重绘并应用 `values-night/themes.xml` 中的颜色配置。
4. 全局回归测试，确保文字可读性强，对比度符合 WCAG 无障碍标准。

## 四、 结语
通过本次重构，我们坚持了**职责单一**与**高内聚低耦合**的设计原则，将“颜色长什么样”交给了资源字典（`colors.xml`），将“颜色用在哪里”交给了主题（`themes.xml`），而布局文件（`layout`）只关心语义（如“这里用主色”）。这为项目未来的长远维护和功能迭代打下了坚实的工程基础。
