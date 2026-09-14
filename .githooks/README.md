# Git Hooks

## Pre-commit Hook

A pre-commit hook that formats and lints **staged files only** before commits to
prevent CI failures (format + Catalyst Python lint).

### Setup (One-Time)

Run once per clone (or worktree) so commits use these hooks:

```bash
./.githooks/setup.sh
```

Or manually: `git config core.hooksPath .githooks`

**Cursor IDE:** Worktrees created via Cursor automatically run this setup via
`.cursor/worktrees.json`.

### What It Does

The hook runs on **staged files only**:

- **Java runtime**: Uses the `.sdkmanrc` Java candidate when it is installed,
  then verifies that Java 21 is active before invoking Maven.
- **Format**: Java, Markdown, Shell, XML, .gitignore, pom.xml (spotless),
  frontend (prettier), Python (ruff format). Formatted files are re-staged.
- **Lint**: For staged Catalyst Python files, runs `ruff check` (same as
  Catalyst CI). Commit is blocked if lint fails.

### How It Works

1. You run `git commit`
2. Hook detects file types in staging area
3. Runs appropriate formatters
4. Re-stages formatted files
5. Stops the commit if a required runtime or formatter fails; otherwise the
   commit proceeds

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

**Required runtime or formatter not found?**

- Install Maven for Spotless-managed files and Node.js/npm for frontend files
- Install the Java version named in `.sdkmanrc`, or activate another Java 21 JDK
- A missing required tool or formatter failure stops the commit
