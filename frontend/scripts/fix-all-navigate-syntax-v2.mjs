import fs from 'fs';

const files = [
  'frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.jsx',
  'frontend/src/components/analyzers/FieldMapping/FieldMapping.jsx'
];

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  
  // Fix the specific pattern: search: params.toString() { replace: true })
  content = content.replace(
    /search:\s*params\.toString\(\s*\{\s*replace:\s*true\s*\}\s*\)/g,
    'search: params.toString()'
  );
  
  // Fix the pattern where the closing brace and parenthesis are misplaced
  content = content.replace(
    /params\.toString\(\s*\{\s*replace:\s*true\s*\}\s*\)/g,
    'params.toString() }, { replace: true }'
  );
  
  fs.writeFileSync(file, content);
  console.log(`Fixed syntax errors in ${file}`);
});
