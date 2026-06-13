import os
import re

# task_icons
icons = [
    'app/src/main/res/drawable/ic_info.xml',
    'app/src/main/res/drawable/ic_sun.xml',
    'app/src/main/res/drawable/ic_dropdown.xml',
    'app/src/main/res/drawable/ic_clock.xml',
    'app/src/main/res/drawable/ic_arrow_right.xml',
    'app/src/main/res/drawable/ic_calendar.xml',
    'app/src/main/res/drawable/ic_urgent_alert.xml'
]
for f in icons:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    content = re.sub(r'android:fillColor="@color/[^"]+"', 'android:fillColor="#FF000000"', content)
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)

# task_colors
colors = [
    'app/src/main/res/color/bottom_nav_item_colors.xml',
    'app/src/main/res/color/chip_bg_color_selector.xml',
    'app/src/main/res/color/chip_stroke_color_selector.xml',
    'app/src/main/res/color/chip_text_color_selector.xml',
    'app/src/main/res/color/selector_segmented_text.xml',
    'app/src/main/res/color/switch_thumb_tint.xml',
    'app/src/main/res/color/switch_track_tint.xml'
]
for f in colors:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    content = content.replace('"@color/brand_indigo"', '"?attr/colorPrimary"')
    content = content.replace('"@color/ink_muted"', '"?attr/colorOnSurfaceVariant"')
    content = content.replace('"@color/card_bg_white"', '"?attr/colorSurface"')
    content = content.replace('"@color/transparent"', '"@android:color/transparent"')
    content = content.replace('"@color/text_primary"', '"?attr/colorOnSurface"')
    content = content.replace('"@color/text_secondary"', '"?attr/colorOnSurfaceVariant"')
    content = content.replace('"@color/white"', '"?attr/colorSurface"')
    content = content.replace('"@color/outline_strong"', '"?attr/colorOutline"')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)

# task_shapes
shapes = [
    'app/src/main/res/drawable/bg_tag_outline.xml',
    'app/src/main/res/drawable/bg_tag_type.xml',
    'app/src/main/res/drawable/bg_segmented_item_checked.xml',
    'app/src/main/res/drawable/bg_segmented_container.xml',
    'app/src/main/res/drawable/bg_tag_rounded.xml'
]
for f in shapes:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    content = content.replace('"@color/outline_soft"', '"?attr/colorOutlineVariant"')
    content = content.replace('"@color/white"', '"?attr/colorSurface"')
    content = content.replace('"@color/warm_surface_variant"', '"?attr/colorSurfaceVariant"')
    content = content.replace('"#E0E0E0"', '"?attr/colorOutline"')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)

print("Done python script")
