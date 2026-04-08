import fs from 'fs';

const file = 'frontend/src/components/storage/StorageDashboard.test.jsx';
let content = fs.readFileSync(file, 'utf8');

// Fix nested act() calls
content = content.replace(
  /act\(\) \{\s*act\(\) \{\s*fireEvent\.click\(([^)]+)\);\s*\};\s*\};/gs,
  'act(() => {\n      fireEvent.click($1);\n    });'
);

fs.writeFileSync(file, content);
console.log('Fixed nested act() calls in StorageDashboard.test.jsx');
