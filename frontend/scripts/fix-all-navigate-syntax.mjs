import fs from 'fs';

const files = [
  'frontend/src/components/analyzers/AnalyzersList/AnalyzersList.jsx',
  'frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.jsx',
  'frontend/src/components/analyzers/FieldMapping/FieldMapping.jsx'
];

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  
  // Fix all navigate syntax errors
  content = content.replace(/params\.toString\(\s*\{\s*replace:\s*true\s*\}\s*\}/g, 'params.toString() }, { replace: true }');
  content = content.replace(/search:\s*params\.toString\(\s*\{\s*replace:\s*true\s*\}/g, 'search: params.toString() }, { replace: true }');
  
  fs.writeFileSync(file, content);
  console.log(`Fixed syntax errors in ${file}`);
});
