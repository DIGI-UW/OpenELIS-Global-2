import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });
let count = 0;

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  let changed = false;

  // Replace <Route exact path="..." component={X} /> with <Route path="..." element={<X />} />
  content = content.replace(/<Route([^>]*?)component=\{([A-Za-z0-9_]+)\}([^>]*?)\/?>/g, (match, before, componentName, after) => {
    changed = true;
    let newBefore = before.replace(/\s*exact\s*/g, ' '); // v6 doesn't need exact
    let newAfter = after.replace(/\s*exact\s*/g, ' ');
    return `<Route${newBefore}element={<${componentName} />}${newAfter} />`;
  });

  if (changed) {
    fs.writeFileSync(file, content);
    count++;
  }
});

console.log(`Transformed ${count} files with <Route /> for v6.`);
