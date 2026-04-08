import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });

let totalFixed = 0;

files.forEach(file => {
  const content = fs.readFileSync(file, 'utf8');
  const lines = content.split('\n');
  let modified = false;
  let newLines = [];
  let seenIntl = false;
  
  lines.forEach(line => {
    if (line.includes('const intl = useIntl()')) {
      if (!seenIntl) {
        newLines.push(line);
        seenIntl = true;
      } else {
        // Skip this duplicate line
        modified = true;
        console.log(`Removed duplicate in ${file}: ${line.trim()}`);
      }
    } else {
      newLines.push(line);
    }
  });
  
  if (modified) {
    fs.writeFileSync(file, newLines.join('\n'));
    totalFixed++;
  }
});

console.log(`\nTotal files fixed: ${totalFixed}`);
