# Git Hooks

## Pre-commit Hook

A pre-commit hook that formats and lints **staged files only** before commits to
prevent CI failures (format + Catalyst Python lint).

### Setup (Automatic)

Hooks are automatically installed when you run any Maven command:

```bash
mvn compile   # or any other mvn goal
```

The first Maven command sets `core.hooksPath` to `.githooks`. No manual setup
required.

If you need to set up manually (or in a worktree where Maven won't run):

```bash
./.githooks/setup.sh
```

Or manually: `git config core.hooksPath .githooks`

### What It Does

The hook runs on **staged files only**:

- **Format**: Java, Markdown, Shell, XML, .gitignore, pom.xml (spotless),
  frontend (prettier), Python (ruff format). Formatted files are re-staged.
- **Lint**: For staged Catalyst Python files, runs `ruff check` (same as
  Catalyst CI). Commit is blocked if lint fails.

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

# Should show absolute path to .githooks
# If not, run any Maven command to auto-install:
mvn compile -q
```

**Formatter not found?**

- Ensure tools are installed (npm, maven, uv)
- Hook gracefully skips missing formatters
