# 资源重构与深色模式预适配详细计划 (全项目范围)

## 摘要与方案评估

为了**为后续深色模式适配做好铺垫，同时避免现在就陷入全面的深色模式开发**，我们将采用 **Material Design 主题属性（Theme Attributes）** 与 **矢量图动态着色（Icon Tinting）** 的最佳实践进行项目级的架构调整。

本阶段的**核心目标**是：对整个项目所有布局和相关资源文件进行精确的清理和替换，搭建基于 Theme 属性的引用架构。未来需要支持深色模式时，只需在 `values-night/themes.xml` 中配置一套颜色映射即可。**本次修改必须保证当前浅色模式下的 UI 视觉效果完全保持不变。**

本计划已进行了详尽的全局扫描，**明确列出了所有需要修改的文件，绝无任何省略（不使用“等”字）**。

***

## 拟议变更（明确到所有具体文件）

### 第一部分：全局主题属性映射（已在前期完成）

*文件：`e:\code\MyPotato\app\src\main\res\values\themes.xml`* *及* *`values-night\themes.xml`*

* 已在 `Base.Theme.MyPotato` 中增加了 `android:colorBackground`, `colorSurface`, `colorOnSurface`, `colorOnSurfaceVariant`, `colorOutline`, `colorOutlineVariant`, `colorError` 等属性映射，供后续布局引用。

### 第二部分：矢量图标与状态选择器文件去色与引用更新

将所有带有硬编码颜色的通用矢量图标的 `fillColor` 统一改为占位中性色 `#FF000000`，以便在布局中动态着色。
**需要修改的 7 个图标文件**：

1. `e:\code\MyPotato\app\src\main\res\drawable\ic_info.xml`
2. `e:\code\MyPotato\app\src\main\res\drawable\ic_sun.xml`
3. `e:\code\MyPotato\app\src\main\res\drawable\ic_dropdown.xml`
4. `e:\code\MyPotato\app\src\main\res\drawable\ic_clock.xml`
5. `e:\code\MyPotato\app\src\main\res\drawable\ic_arrow_right.xml`
6. `e:\code\MyPotato\app\src\main\res\drawable\ic_calendar.xml`
7. `e:\code\MyPotato\app\src\main\res\drawable\ic_urgent_alert.xml` （原为红色，现改为 `#FF000000`，由布局决定颜色）

**需要修改的 7 个 ColorStateList 文件（将内部的** **`@color/xxx`** **替换为** **`?attr/xxx`）**：

1. `e:\code\MyPotato\app\src\main\res\color\bottom_nav_item_colors.xml`
2. `e:\code\MyPotato\app\src\main\res\color\chip_bg_color_selector.xml`
3. `e:\code\MyPotato\app\src\main\res\color\chip_stroke_color_selector.xml`
4. `e:\code\MyPotato\app\src\main\res\color\chip_text_color_selector.xml`
5. `e:\code\MyPotato\app\src\main\res\color\selector_segmented_text.xml`
6. `e:\code\MyPotato\app\src\main\res\color\switch_thumb_tint.xml`
7. `e:\code\MyPotato\app\src\main\res\color\switch_track_tint.xml`

**需要修改的 6 个 Shape Drawable 文件（将内部的** **`@color/xxx`** **替换为** **`?attr/xxx`）**：

1. `e:\code\MyPotato\app\src\main\res\drawable\sel_checkbox_circle.xml`
2. `e:\code\MyPotato\app\src\main\res\drawable\bg_tag_outline.xml`
3. `e:\code\MyPotato\app\src\main\res\drawable\bg_tag_type.xml`
4. `e:\code\MyPotato\app\src\main\res\drawable\bg_segmented_item_checked.xml`
5. `e:\code\MyPotato\app\src\main\res\drawable\bg_segmented_container.xml`
6. `e:\code\MyPotato\app\src\main\res\drawable\bg_tag_rounded.xml`

*(注：原有的* *`ic_play_white_24dp.xml`* *等 4 个文件已在前期完成重命名与去色)*

### 第三部分：布局文件硬编码全面替换（颜色与图标动态着色）

对以下 **全部 11 个布局文件** 进行逐行扫描与修改。
操作：将其中的 `@color/page_bg_light` 替换为 `?android:attr/colorBackground`，将 `@color/card_bg_white` 替换为 `?attr/colorSurface`，将 `@color/text_primary` 替换为 `?attr/colorOnSurface` 等。同时，为引用了矢量图标的 `ImageView` 和 `MaterialButton` 添加 `app:tint="?attr/..."` 或 `app:iconTint="?attr/..."`。
**完整的布局文件清单**：

1. `e:\code\MyPotato\app\src\main\res\layout\activity_main.xml` *(已在前期完成)*
2. `e:\code\MyPotato\app\src\main\res\layout\activity_pomodoro.xml` *(已在前期完成)*
3. `e:\code\MyPotato\app\src\main\res\layout\activity_task_detail.xml`
4. `e:\code\MyPotato\app\src\main\res\layout\bottom_sheet_add_task_placeholder.xml`
5. `e:\code\MyPotato\app\src\main\res\layout\fragment_settings.xml`
6. `e:\code\MyPotato\app\src\main\res\layout\fragment_statistics.xml`
7. `e:\code\MyPotato\app\src\main\res\layout\fragment_tasks.xml`
8. `e:\code\MyPotato\app\src\main\res\layout\fragment_today.xml`
9. `e:\code\MyPotato\app\src\main\res\layout\item_statistics_timeline_slot.xml`
10. `e:\code\MyPotato\app\src\main\res\layout\item_step.xml`
11. `e:\code\MyPotato\app\src\main\res\layout\item_today_task.xml`

### 第四部分：字符串硬编码全局提取

通过扫描，发现仍有部分布局文件存在 `android:text` 的中文硬编码。将对以下 **4 个具体文件** 进行字符串提取，统一存入 `e:\code\MyPotato\app\src\main\res\values\strings.xml` 中：

1. `e:\code\MyPotato\app\src\main\res\layout\fragment_tasks.xml` （包含“按类别”、“按象限”、“学习”、“工作”等硬编码文字）
2. `e:\code\MyPotato\app\src\main\res\layout\activity_task_detail.xml` （包含“紧急”、“重要”、“单次任务不支持番茄钟计时”等文字）
3. `e:\code\MyPotato\app\src\main\res\layout\fragment_settings.xml` （包含“外观设置”、“语言设置”、“关于”等大量文字）
4. `e:\code\MyPotato\app\src\main\res\layout\fragment_statistics.xml` （包含“今日”、“本周”、“全部类别”、“已完成”等文字）

***

## 验证步骤

1. **静态检查**：确认上述所有清单中的文件均已修改完毕，不存在遗漏的 `@color/text_primary` 等静态引用和中文字符串硬编码。
2. **编译运行**：验证项目能正常无报错编译。
3. **全局 UI 回归**：在当前浅色模式下，启动应用并访问所有页面，确保每一处 UI 外观与重构前绝对一致。

