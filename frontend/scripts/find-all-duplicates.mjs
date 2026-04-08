import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });
const duplicates = [];

files.forEach(file => {
  const content = fs.readFileSync(file, 'utf8');
  const lines = content.split('\n');
  const intlDeclarations = [];
  
  lines.forEach((line, index) => {
    if (line.trim() === 'const intl = useIntl();') {
      intlDeclarations.push({ line: index + 1, content: line.trim() });
    }
  });
  
  if (intlDeclarations.length > 1) {
    duplicates.push({ file, declarations: intlDeclarations });
  }
});

if (duplicates.length > 0) {
  console.log('Found duplicates in:');
  duplicates.forEach(({ file, declarations }) => {
    console.log(`\n${file}:`);
    declarations.forEach(decl => {
      console.log(`  Line ${decl.line}: ${decl.content}`);
    });
  });
} else {
  console.log('No duplicate const intl = useIntl(); declarations found');
}
