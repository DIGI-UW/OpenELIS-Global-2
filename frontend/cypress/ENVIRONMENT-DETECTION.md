# Cypress E2E Testing: Environment Detection

**Created**: 2026-01-29
**Purpose**: Guide for detecting and configuring test environment (localhost vs subdomain)

---

## Problem Statement

OpenELIS deployments vary by environment:

- **Local development**: `https://localhost/` (self-signed certificate)
- **Subdomain deployments**: `https://analyzers.openelis-global.org/`, `https://storage.openelis-global.org/`, etc. (Let's Encrypt certificates)

**E2E tests and manual testing scripts MUST detect the environment** to use the correct base URL.

---

## Environment Detection Pattern

### Method 1: Check Let's Encrypt Domain (Recommended)

```bash
# Get domain from proxy container
export DOMAIN=$(docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN | cut -d= -f2 2>/dev/null)

# Fallback to .env file
if [ -z "$DOMAIN" ]; then
  export DOMAIN=$(grep LETSENCRYPT_DOMAIN .env | cut -d= -f2)
fi

# Default to localhost if not found
if [ -z "$DOMAIN" ]; then
  export DOMAIN="localhost"
fi

echo "Detected environment: https://$DOMAIN/"
```

### Method 2: Check Nginx Config

```bash
# Check nginx virtual host configuration
docker exec openelisglobal-proxy cat /etc/nginx/conf.d/default.conf | grep server_name
```

### Method 3: Test Actual Response

```bash
# Try subdomain first, fall back to localhost
curl -k -I https://analyzers.openelis-global.org/ 2>&1 | grep "HTTP" && export DOMAIN="analyzers.openelis-global.org" || export DOMAIN="localhost"
```

---

## Cypress Configuration

### cypress.config.js (Environment-Aware)

```javascript
import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    // Detect baseUrl from environment or default to localhost
    baseUrl: process.env.CYPRESS_BASE_URL || "https://localhost",

    setupNodeEvents(on, config) {
      // Auto-detect from Let's Encrypt if CYPRESS_BASE_URL not set
      if (!process.env.CYPRESS_BASE_URL) {
        const { execSync } = require("child_process");
        try {
          const domain = execSync(
            "docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN | cut -d= -f2",
            { encoding: "utf-8" },
          ).trim();

          if (domain && domain !== "localhost") {
            config.baseUrl = `https://${domain}`;
            console.log(`🌐 Detected subdomain: ${config.baseUrl}`);
          }
        } catch (e) {
          console.log("⚠️  Could not detect subdomain, using localhost");
        }
      }

      return config;
    },
  },
});
```

### Running Tests with Custom Domain

```bash
# Option 1: Set environment variable
export CYPRESS_BASE_URL="https://analyzers.openelis-global.org"
npm run cy:run

# Option 2: Inline variable
CYPRESS_BASE_URL="https://analyzers.openelis-global.org" npm run cy:run

# Option 3: Auto-detect (if config above implemented)
npm run cy:run  # Automatically detects from LETSENCRYPT_DOMAIN
```

---

## Test Script Best Practices

### Shell Scripts (Manual Testing)

**Always detect environment at script start**:

```bash
#!/bin/bash
# Detect environment
DOMAIN=$(docker exec openelisglobal-proxy env 2>/dev/null | grep LETSENCRYPT_DOMAIN | cut -d= -f2)
DOMAIN=${DOMAIN:-localhost}

echo "Testing on: https://$DOMAIN/"

# Use $DOMAIN in all curl commands
curl -k https://$DOMAIN/api/OpenELIS-Global/rest/analyzer-list
```

### Cypress Tests

**Use cy.visit() with relative paths** (automatically uses baseUrl):

```javascript
// ✅ GOOD: Relative path (uses baseUrl from config)
cy.visit("/AnalyzerConfiguration");
cy.visit("/AnalyzerResults");

// ❌ BAD: Hardcoded localhost
cy.visit("https://localhost/AnalyzerConfiguration");
```

**For API calls, use Cypress.config('baseUrl')**:

```javascript
// ✅ GOOD: Use config baseUrl
cy.request({
  url: `${Cypress.config("baseUrl")}/api/OpenELIS-Global/rest/analyzer-list`,
});

// OR even better, use relative URL (Cypress handles baseUrl automatically)
cy.request("/api/OpenELIS-Global/rest/analyzer-list");
```

---

## Common Environment Variables

| Variable             | Description                        | Example                               | Checked Location    |
| -------------------- | ---------------------------------- | ------------------------------------- | ------------------- |
| `LETSENCRYPT_DOMAIN` | Primary domain for SSL certificate | `analyzers.openelis-global.org`       | Proxy container env |
| `LETSENCRYPT_EMAIL`  | Admin email for Let's Encrypt      | `admin@openelis-global.org`           | `.env` file         |
| `VIRTUAL_HOST`       | Nginx virtual host                 | `analyzers.openelis-global.org`       | Nginx config        |
| `CYPRESS_BASE_URL`   | Override for Cypress tests         | `https://storage.openelis-global.org` | Shell env           |

---

## Debugging Environment Issues

### Certificate Errors

```bash
# Check Let's Encrypt certificates
ls -la volume/letsencrypt/live/

# Verify certificate matches domain
docker exec openelisglobal-proxy cat /etc/nginx/conf.d/default.conf | grep -A5 ssl_certificate
```

### Wrong Domain Detected

```bash
# Check all domain references
docker exec openelisglobal-proxy env | grep -i domain
cat .env | grep -i domain
docker exec openelisglobal-webapp env | grep -i host

# Override in tests
export CYPRESS_BASE_URL="https://correct-domain.org"
```

### Localhost vs Subdomain Confusion

```bash
# Quick test: Which one responds?
curl -k -I https://localhost/ 2>&1 | head -1
curl -k -I https://analyzers.openelis-global.org/ 2>&1 | head -1

# Use the one that returns HTTP/1.1 200 OK
```

---

## Platform Notes

**Windows**: The Cypress npm scripts use `unset ELECTRON_RUN_AS_NODE` (POSIX shell).
This fails on Windows cmd/PowerShell. Use WSL, Git Bash, or a Unix-like environment
for local Cypress runs. CI uses Linux. If Windows contributor support is needed,
consider `cross-env` or a Node-based wrapper to clear env vars cross-platform.

---

## Feature Testing Checklist

When creating E2E tests for new features:

- [ ] **Detect environment** using Let's Encrypt domain method
- [ ] **Use relative URLs** in `cy.visit()` (not absolute with hardcoded localhost)
- [ ] **Set DOMAIN variable** in manual testing scripts
- [ ] **Document environment requirements** in feature testing guide
- [ ] **Test on both localhost AND subdomain** before merging
- [ ] **Include env detection in test setup** (see cypress.config.js example above)

---

## Integration with Constitution V.5

This pattern aligns with **Constitution Principle V, Section V.5** (Cypress E2E Testing Best Practices):

- ✅ **Test Execution Workflow**: Environment detection ensures tests work in dev, staging, and CI
- ✅ **Configuration Requirements**: baseUrl detection prevents hardcoded URLs
- ✅ **Anti-Patterns**: Avoids hardcoding `localhost` in test files

---

## Example: Feature 011 Analyzer Testing

See [`specs/011-madagascar-analyzer-integration/MANUAL-TESTING-GUIDE-M9-M10.md`](../../specs/011-madagascar-analyzer-integration/MANUAL-TESTING-GUIDE-M9-M10.md) for implementation of this pattern.

**Key snippet**:

```bash
# Auto-detect domain
export DOMAIN=$(docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN | cut -d= -f2)
export DOMAIN=${DOMAIN:-localhost}

# Use in all test commands
curl -k https://$DOMAIN/api/OpenELIS-Global/rest/analyzer-list
```

---

**Last Updated**: 2026-01-29
**Related**: Constitution V.5, Cypress Best Practices
**Applies To**: All E2E tests, manual testing scripts, CI/CD configurations
