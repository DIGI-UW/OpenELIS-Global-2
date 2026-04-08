import fs from 'fs';

const file = 'frontend/src/components/storage/StorageDashboard.test.jsx';
let content = fs.readFileSync(file, 'utf8');

// Replace all fireEvent.click calls with act-wrapped versions
content = content.replace(
  /fireEvent\.click\(([^)]+)\);/g,
  'act(() => {\n      fireEvent.click($1);\n    });'
);

fs.writeFileSync(file, content);
console.log('Fixed all fireEvent.click calls in StorageDashboard.test.jsx');
