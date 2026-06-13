import re
f = "app/src/main/res/layout/fragment_tasks.xml"
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()
content = re.sub(r'app:tint="\?attr/colorOnSurfaceVariant"(\s+)app:tint="\?attr/colorOnSurfaceVariant"', r'app:tint="?attr/colorOnSurfaceVariant"', content)
with open(f, 'w', encoding='utf-8') as file:
    file.write(content)
