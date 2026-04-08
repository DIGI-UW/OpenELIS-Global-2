import fs from 'fs';
import glob from 'glob';

// Use glob to find all JSX files in src
const files = glob.sync('src/**/*.jsx', { absolute: true });

let modifiedCount = 0;

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  
  if (!content.includes('injectIntl')) {
    return;
  }

  const originalContent = content;

  // 1. Replace injectIntl import with useIntl
  content = content.replace(/import\s+\{\s*([^}]*?)\s*\}\s+from\s+['"]react-intl['"];?/g, (match, imports) => {
    let parts = imports.split(',').map(s => s.trim()).filter(s => s && s !== 'injectIntl');
    if (!parts.includes('useIntl')) {
      parts.push('useIntl');
    }
    return `import { ${parts.join(', ')} } from 'react-intl';`;
  });

  // 2. Remove injectIntl from export
  content = content.replace(/export\s+default\s+injectIntl\(([^)]+)\);?/g, 'export default $1;');

  // 3. Inject useIntl hook into the component body
  // Try to find the component definition that matches the export
  const exportMatch = originalContent.match(/export\s+default\s+injectIntl\(([^)]+)\)/);
  if (exportMatch) {
    const componentName = exportMatch[1].trim();
    
    // Look for: const ComponentName = ({ intl, ...props }) => {
    const arrowFuncRegex = new RegExp(`const\\s+${componentName}\\s*=\\s*\\(([^)]*)\\)\\s*=>\\s*\\{`);
    
    if (arrowFuncRegex.test(content)) {
      content = content.replace(arrowFuncRegex, (match, args) => {
        // Remove intl from args
        let newArgs = args
          .replace(/\{\s*intl\s*,?/, '{')  // { intl, props } -> { props }
          .replace(/,\s*intl\s*\}/, '}')   // { props, intl } -> { props }
          .replace(/\{\s*intl\s*\}/, '()') // { intl } -> ()
          .replace(/\bintl\b\s*,?/, '')    // intl, props -> props
          .trim();
          
        if (newArgs === '{}' || newArgs.startsWith('{ ') && newArgs.endsWith(' }') && newArgs.length <= 4) {
             newArgs = ''; // handle empty destructuring
        }
        
        return `const ${componentName} = (${newArgs}) => {\n  const intl = useIntl();`;
      });
    } else {
      // Might be a regular function: function ComponentName({ intl }) {
      const regularFuncRegex = new RegExp(`function\\s+${componentName}\\s*\\(([^)]*)\\)\\s*\\{`);
      if (regularFuncRegex.test(content)) {
        content = content.replace(regularFuncRegex, (match, args) => {
          let newArgs = args.replace(/\bintl\b\s*,?\s*/, '').replace(/\{\s*intl\s*\}/, '').replace(/,\s*$/, '');
          return `function ${componentName}(${newArgs}) {\n  const intl = useIntl();`;
        });
      }
    }
  }

  if (content !== originalContent) {
    fs.writeFileSync(file, content);
    modifiedCount++;
  }
});

console.log(`Successfully modified ${modifiedCount} files.`);
