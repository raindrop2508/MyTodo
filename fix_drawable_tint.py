import re

files = [
  "app/src/main/res/layout/fragment_statistics.xml",
  "app/src/main/res/layout/fragment_today.xml",
  "app/src/main/res/layout/item_today_task.xml"
]

for f in files:
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    
    content = content.replace('app:drawableStartTint', 'android:drawableTint')
    
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)
print("Fix drawableTint done")
