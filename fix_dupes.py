import re

files = [
  "app/src/main/res/layout/activity_task_detail.xml",
  "app/src/main/res/layout/fragment_today.xml",
  "app/src/main/res/layout/fragment_settings.xml",
  "app/src/main/res/layout/item_today_task.xml"
]

for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    # In XML, if an element has two app:tint, we need to remove one.
    # It's easier to remove the one I appended if there's another app:tint nearby.
    # I'll just remove the appended ones from the same line if the next line has app:tint
    # Or just use regex to clean up:
    content = re.sub(r'android:src="(@drawable/ic_[a-z_]+)" app:tint="\?attr/colorOnSurfaceVariant"(\s+)app:tint="[^"]+"', r'android:src="\1"\2app:tint="?attr/colorOnSurfaceVariant"', content)
    
    # Actually, simpler:
    # pp:tint="?attr/colorOnSurfaceVariant" followed by pp:tint="?attr/colorOnSurfaceVariant" across newlines
    content = re.sub(r'app:tint="\?attr/colorOnSurfaceVariant"\s+app:tint="\?attr/colorOnSurfaceVariant"', 'app:tint="?attr/colorOnSurfaceVariant"', content)
    
    content = re.sub(r'app:drawableStartTint="\?attr/colorOnSurfaceVariant"\s+app:drawableStartTint="\?attr/colorOnSurfaceVariant"', 'app:drawableStartTint="?attr/colorOnSurfaceVariant"', content)

    # If it was originally on the next line:
    content = re.sub(r'(android:src="[^"]+") app:tint="\?attr/colorOnSurfaceVariant"(\s+)app:tint="\?attr/colorOnSurfaceVariant"', r'\1\2app:tint="?attr/colorOnSurfaceVariant"', content)

    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)
print("Fix duplicates done")
