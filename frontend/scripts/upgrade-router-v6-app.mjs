import fs from 'fs';
import path from 'path';

const file = path.join(process.cwd(), 'frontend/src/App.jsx');
let content = fs.readFileSync(file, 'utf8');

// 1. Replace exact component={() => <Comp />}
content = content.replace(/<Route([^>]*?)\bexact\b\s+component=\{\s*\(\)\s*=>\s*([^}]+)\s*\}([^>]*?)\/?>/g, (match, before, comp, after) => {
  return `<Route${before}element={${comp}}${after} />`;
});

// 2. Replace component={() => <Comp />}
content = content.replace(/<Route([^>]*?)component=\{\s*\(\)\s*=>\s*([^}]+)\s*\}([^>]*?)\/?>/g, (match, before, comp, after) => {
  return `<Route${before}element={${comp}}${after} />`;
});

// 3. Replace component={Comp}
content = content.replace(/<Route([^>]*?)component=\{([A-Za-z0-9_]+)\}([^>]*?)\/?>/g, (match, before, comp, after) => {
  return `<Route${before}element={<${comp} />}${after} />`;
});

fs.writeFileSync(file, content);
console.log('App.jsx transformed');
