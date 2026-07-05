#!/bin/bash
FILES=$(git ls-files --others --exclude-standard ; git diff --name-only)
# sort and unique
FILES=$(echo "$FILES" | sort | uniq)

for file in $FILES; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        git add "$file"
        git commit -m "Add $filename"
    fi
done

git push origin main
