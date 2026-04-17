#!/bin/bash
cd frontend/src

# Find all files with more than one "const intl = useIntl()" declaration
for file in $(find . -name "*.jsx" -exec grep -l "const intl = useIntl()" {} \;); do
  count=$(grep "const intl = useIntl()" "$file" | wc -l)
  if [ $count -gt 1 ]; then
    echo "$file has $count duplicates"
    grep -n "const intl = useIntl()" "$file"
  fi
done
