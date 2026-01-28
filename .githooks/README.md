# Git Hooks

## Auto-Format Hook

A pre-commit hook that automatically formats **staged files only** before
commits to prevent CI failures.

### Setup (One-Time)

```bash
# Configure git to use this hooks directory
git config core.hooksPath .githooks
```

### What It Does

The hook automatically runs formatters on **staged files only**:

- **Java**: `mvn spotless:apply`
- **Frontend** (JS/TS/CSS): `npm run format` in frontend directory
- **Python** (Catalyst): `ruff format` in catalyst projects

Formatted files are automatically re-staged.

### How It Works

1. You run `git commit`
2. Hook detects file types in staging area
3. Runs appropriate formatters
4. Re-stages formatted files
5. Commit proceeds

### Benefits

- ✅ Never forget to format
- ✅ No CI failures from formatting
- ✅ Fast (formats staged files only, not entire codebase)
- ✅ Non-intrusive (silently formats and re-stages)

### Disable Temporarily

If you need to bypass the hook:

```bash
git commit --no-verify
```

### Troubleshooting

**Hook not running?**

```bash
# Check hooks path
git config core.hooksPath

# Should show: .githooks
# If not, run setup command above
```

**Formatter not found?**

- Ensure tools are installed (npm, maven, uv)
- Hook gracefully skips missing formatters
