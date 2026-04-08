import fs from 'fs';
import glob from 'glob';

const files = [
  'frontend/src/components/analyzers/AnalyzersList/AnalyzersList.jsx',
  'frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.jsx',
  'frontend/src/components/analyzers/FieldMapping/FieldMapping.jsx'
];

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  
  // Fix the syntax error
  content = content.replace(/params\.toString\(\,/g, 'params.toString()');
  
  fs.writeFileSync(file, content);
  console.log(`Fixed ${file}`);
});
