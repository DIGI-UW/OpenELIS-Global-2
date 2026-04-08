import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });

files.forEach(file => {
  const content = fs.readFileSync(file, 'utf8');
  const lines = content.split('\n');
  const intlDeclarations = [];
  
  lines.forEach((line, index) => {
    if (line.includes('const intl = useIntl()')) {
      intlDeclarations.push({ line: index + 1, content: line.trim() });
    }
  });
  
  if (intlDeclarations.length > 1) {
    console.log(`\n${file}:`);
    intlDeclarations.forEach(decl => {
      console.log(`  Line ${decl.line}: ${decl.content}`);
    });
  }
});
