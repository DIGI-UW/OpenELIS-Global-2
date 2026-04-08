import fs from 'fs';
import { execSync } from 'child_process';

// Get list of files with duplicates
const result = execSync('cd frontend/src && find . -name "*.jsx" -exec grep -l "const intl = useIntl()" {} \\;', { encoding: 'utf8' });
const files = result.trim().split('\n').filter(f => f);

let totalFixed = 0;

console.log('Fixing all duplicate const intl = useIntl() declarations...\n');

files.forEach(file => {
  const fullPath = `frontend/src/${file}`;
  const content = fs.readFileSync(fullPath, 'utf8');
  
  // Count occurrences
  const matches = content.match(/const intl = useIntl\(\);/g);
  if (!matches || matches.length <= 1) return;
  
  console.log(`Fixing ${file} (${matches.length} duplicates)`);
  
  // Replace all but the first occurrence
  let seen = false;
  const newContent = content.replace(/const intl = useIntl\(\);/g, (match) => {
    if (!seen) {
      seen = true;
      return match;
    }
    totalFixed++;
    return ''; // Remove duplicate
  });
  
  fs.writeFileSync(fullPath, newContent);
});

console.log(`\nTotal duplicates removed: ${totalFixed}`);
