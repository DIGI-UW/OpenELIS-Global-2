import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });
let totalFixed = 0;

console.log('Fixing ALL duplicate const intl = useIntl(); declarations...\n');

files.forEach(file => {
  const content = fs.readFileSync(file, 'utf8');
  const lines = content.split('\n');
  let modified = false;
  let newLines = [];
  let hasIntl = false;
  
  lines.forEach((line, index) => {
    // Check for exact match or line containing const intl = useIntl()
    if (line.includes('const intl = useIntl()')) {
      if (!hasIntl) {
        newLines.push(line);
        hasIntl = true;
      } else {
        // Skip this duplicate line
        modified = true;
        console.log(`Removed duplicate in ${file}:${index + 1}`);
        totalFixed++;
      }
    } else {
      newLines.push(line);
    }
  });
  
  if (modified) {
    fs.writeFileSync(file, newLines.join('\n'));
  }
});

console.log(`\nTotal duplicates removed: ${totalFixed}`);
