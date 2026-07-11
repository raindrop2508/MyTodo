# Material Design 3 高度叠加层（Elevation Overlay）技术详解

## 概述

**Elevation Overlay（高度叠加层）** 是 Material Design 3（M3）中用于表现界面元素空间层级的核心视觉机制。它通过动态改变表面颜色而非仅依赖阴影来体现不同高度的视觉差异，解决了在不同背景下阴影辨识度不高的问题。

## 产生背景

在 Material Design 2（M2）中，视觉层级主要通过**阴影**来表现。但存在以下局限：

1. **深色模式下阴影不明显**：深色背景中的阴影对比度低，难以清晰感知高度差异
2. **纯静态阴影无法体现场景差异**：在浅色或深色背景下，相同高度的视觉感受应当不同
3. **缺乏颜色语义**：无法通过颜色快速感知元素的突出程度

Material Design 3 引入**动态表面着色**机制，通过**主色叠加**的方式强化高度感知。

---

## 核心原理

### 1. 绝对高度（Absolute Elevation）计算

高度叠加层的触发基于**绝对高度**而非单个视图的 `elevation` 属性。

```
绝对高度 = 视图自身 elevation + 所有父级容器的 elevation 累加
```

#### 实际案例分析（本项目中的现象）

本项目中出现的 `#F3E8FF` 现象正是由于这一点：

- **Activity 中卡片正常**：`MaterialCardView` 直接在普通 Activity 根视图中，父级 elevation 为 0，绝对高度 = 0dp → 无叠加
- **BottomSheet 中卡片偏紫**：`BottomSheetDialog` 自身带有约 8dp 的基础高度，虽然卡片自身设置了 `app:cardElevation="0dp"`，但绝对高度 = 0 + 8 = 8dp → 触发叠加

### 2. 混合公式

当绝对高度 > 0 时，系统会自动将主题主色（`colorPrimary`）叠加到表面色（`colorSurface`）上：

```
最终颜色 = 主色 × 不透明度 + 表面色 × (1 - 不透明度)
```

不透明度与绝对高度正相关：

| 绝对高度 | 主色叠加 Alpha |
|---------|--------------|
| 1dp     | ~5%          |
| 3dp     | ~7%          |
| 8dp     | ~9%          |
| 16dp    | ~11%         |
| 24dp    | ~12%         |

### 3. 颜色推演

本项目中颜色变换的数学计算：

- **主色**（`colorPrimary`）：`#4F46E5`（深紫色）
- **表面色**（`colorSurface`）：`#FFFFFF`（纯白）
- **叠加 Alpha**：约 8%

```
R: 255 × (1-0.08) + 79 × 0.08 ≈ 243
G: 255 × (1-0.08) + 70 × 0.08 ≈ 232
B: 255 × (1-0.08) + 229 × 0.08 ≈ 255
```

最终得到 **`#F3E8FF`**，即本项目中 BottomSheet 里卡片出现的浅紫色。

---

## 控制机制

### 全局开关

在主题中全局关闭高度叠加（不推荐，会破坏 M3 整体视觉语言）：

```xml
<style name="Base.Theme.MyPotato" parent="Theme.Material3.Light.NoActionBar">
    <!-- ... 其他配置 ... -->
    <item name="elevationOverlayEnabled">false</item>
</style>
```

### 局部 Theme Overlay（推荐方案）

针对特定组件局部关闭，保留全局设计规范：

```xml
<!-- res/values/themes.xml -->
<style name="ThemeOverlay.MyPotato.CardNoTint" parent="">
    <item name="elevationOverlayEnabled">false</item>
</style>
```

在布局中应用：

```xml
<com.google.android.material.card.MaterialCardView
    android:theme="@style/ThemeOverlay.MyPotato.CardNoTint"
    app:cardBackgroundColor="?attr/colorSurface"
    ... />
```

### 直接设置固定颜色

直接通过 `cardBackgroundColor` 设置具体色值（而非主题属性）也可绕过叠加，但会影响深色模式适配：

```xml
<!-- 不推荐：硬编码颜色会破坏主题一致性 -->
app:cardBackgroundColor="@color/card_bg_white"
```

---

## 技术架构

### 1. MaterialShapeDrawable

核心渲染类，负责处理：
- 形状裁剪
- 边框绘制
- **高度叠加色计算与混合**
- 阴影生成

### 2. ElevationOverlayProvider

负责根据绝对高度计算叠加色的接口。默认实现 `MaterialElevationOverlayProvider` 中：

```kotlin
// 核心逻辑伪代码
fun compositeOverlayIfNeeded(
    backgroundColor: Color,
    absoluteElevation: Float
): Color {
    if (!elevationOverlayEnabled) {
        return backgroundColor
    }
    
    val overlayAlpha = calculateOverlayAlpha(absoluteElevation)
    val overlayColor = overlayColor.withAlpha(overlayAlpha)
    
    return blendColors(overlayColor, backgroundColor)
}
```

### 3. 父级高度累加链

`MaterialCardView` 在绘制时会向上遍历父视图树，累加所有祖先的 `elevation`：

```
MaterialCardView (0dp)
    ↓
FrameLayout (BottomSheet 内部容器，2dp)
    ↓
BottomSheetDialog 根容器 (6dp)
    ↓
总体：0 + 2 + 6 = 8dp
```

---

## 常见问题与解决方案

### Q1: BottomSheet 中的卡片背景为什么会变色？
**A**: BottomSheet 容器自身带有 elevation，导致卡片绝对高度 > 0。

**解决**: 为卡片应用 `elevationOverlayEnabled=false` 的 Theme Overlay。

### Q2: 为什么不同设备上叠加程度看起来不一样？
**A**: 叠加系数受以下因素影响：
- 主题的 `elevationOverlayColor`（默认是 `colorPrimary`）
- 当前系统是否开启"强制深色"或其他辅助功能
- Material Components 库版本

### Q3: 如何自定义叠加色而不是用 colorPrimary？
**A**: 在主题中覆盖 `elevationOverlayColor`：

```xml
<item name="elevationOverlayColor">@color/my_custom_overlay</item>
```

---

## 项目中的应用

在 `MyPotato` 项目中，我们采用局部 Theme Overlay 的方案：

1. **文件变更**：
   - `res/values/themes.xml`：新增 `ThemeOverlay.MyPotato.CardNoTint`
   - `res/layout/bottom_sheet_add_task_placeholder.xml`：应用主题覆盖
   - `res/layout/activity_task_detail.xml`：应用主题覆盖（保持一致性）

2. **效果**：
   - BottomSheet 中的长任务步骤卡片现在保持纯白背景
   - 不影响其他组件的 Material Design 3 标准视觉效果
   - 代码符合 Android 资源隔离规范

---

## 参考资料

- [Material Design 3 官方文档 - Elevation](https://m3.material.io/styles/elevation/overview)
- [Material Components Android - Elevation Overlay](https://github.com/material-components/material-components-android/blob/master/docs/theming/ElevationOverlay.md)
- [MaterialShapeDrawable 源码](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/MaterialTheme.kt)
