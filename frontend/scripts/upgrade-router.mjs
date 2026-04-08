import fs from 'fs';
import glob from 'glob';

const files = glob.sync('src/**/*.jsx', { absolute: true });
let count = 0;

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  let changed = false;

  if (content.includes('useHistory')) {
    // Replace import
    content = content.replace(/useHistory/g, 'useNavigate');
    
    // Replace const history = useHistory() with const navigate = useNavigate()
    content = content.replace(/const\s+history\s*=\s*useNavigate\(\)/g, 'const navigate = useNavigate()');
    
    // Replace history.push() with navigate()
    content = content.replace(/history\.push\(([^)]+)\)/g, 'navigate($1)');
    
    // Replace history.replace() with navigate(..., { replace: true })
    content = content.replace(/history\.replace\(([^)]+)\)/g, 'navigate($1, { replace: true })');

    // Replace history.goBack() with navigate(-1)
    content = content.replace(/history\.goBack\(\)/g, 'navigate(-1)');

    changed = true;
  }

  // Handle Switch -> Routes
  if (content.includes('Switch')) {
    // Check if it's from react-router-dom
    if (content.match(/import\s+.*Switch.*from\s+['"]react-router-dom['"]/)) {
      content = content.replace(/\bSwitch\b/g, 'Routes');
      changed = true;
    }
  }

  // Handle Redirect -> Navigate
  if (content.includes('Redirect')) {
    if (content.match(/import\s+.*Redirect.*from\s+['"]react-router-dom['"]/)) {
      content = content.replace(/\bRedirect\b/g, 'Navigate');
      
      // Update <Navigate to="..." /> to include replace prop (v5 Redirect default behavior)
      content = content.replace(/<Navigate([^>]*?)>/g, (match, props) => {
        if (!props.includes('replace')) {
          return `<Navigate${props} replace>`;
        }
        return match;
      });
      changed = true;
    }
  }

  if (changed) {
    fs.writeFileSync(file, content);
    count++;
  }
});

console.log(`Transformed ${count} files for Router v6.`);
