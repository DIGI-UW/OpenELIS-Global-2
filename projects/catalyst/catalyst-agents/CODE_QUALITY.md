# Code Quality Tools

This project uses modern Python code quality tools to maintain high standards.

## Tools

### Ruff (Linting + Formatting)

**What:** Ultra-fast linter and formatter (replaces Black, Flake8, isort, and
more) **Why:** 10-100x faster than traditional tools, with 800+ built-in rules

```bash
# Run linter (checks code quality)
uv run ruff check .

# Run linter with auto-fix
uv run ruff check --fix .

# Run formatter (formats code)
uv run ruff format .

# Check formatting without changing files
uv run ruff format --check .
```

### mypy (Type Checking)

**What:** Static type checker for Python **Why:** Catches type-related bugs
before runtime

```bash
# Run type checker
uv run mypy src/

# Run on specific file
uv run mypy src/agents/catalyst_executor.py
```

### pre-commit (Automated Hooks)

**What:** Git hook framework that runs checks before commits **Why:**
Automatically enforces code quality standards

```bash
# Install hooks (one-time setup)
uv run pre-commit install

# Run manually on all files
uv run pre-commit run --all-files

# Update hook versions
uv run pre-commit autoupdate
```

## Quick Start

### 1. Install Dependencies

```bash
cd projects/catalyst/catalyst-agents
uv sync --dev
```

### 2. Setup Pre-commit Hooks

```bash
uv run pre-commit install
```

### 3. Run All Checks

```bash
# Lint and format
uv run ruff check --fix .
uv run ruff format .

# Type check
uv run mypy src/

# Run tests
uv run pytest tests/
```

## CI Integration

These tools run automatically in CI:

- **Ruff**: Checks linting and formatting
- **mypy**: Checks type annotations
- **pytest**: Runs test suite

## Configuration

### Ruff Configuration

See `[tool.ruff]` in [pyproject.toml](pyproject.toml)

Key settings:

- Line length: 100 characters
- Python version: 3.11+
- Enabled rules: pycodestyle, pyflakes, isort, pyupgrade, flake8-bugbear, etc.

### mypy Configuration

See `[tool.mypy]` in [pyproject.toml](pyproject.toml)

Key settings:

- Strict optional checking
- Warn on unused configs
- Allow gradual typing (lenient by default, tighten over time)

### Pre-commit Configuration

See [.pre-commit-config.yaml](.pre-commit-config.yaml)

Hooks run automatically on `git commit`:

1. Basic checks (trailing whitespace, YAML syntax, etc.)
2. Ruff linter (with auto-fix)
3. Ruff formatter
4. mypy type checker

## VS Code Integration

Add to `.vscode/settings.json`:

```json
{
  "python.linting.enabled": false,
  "python.formatting.provider": "none",
  "[python]": {
    "editor.defaultFormatter": "charliermarsh.ruff",
    "editor.formatOnSave": true,
    "editor.codeActionsOnSave": {
      "source.fixAll": "explicit",
      "source.organizeImports": "explicit"
    }
  },
  "ruff.lint.args": ["--config=pyproject.toml"],
  "mypy.enabled": true
}
```

## Troubleshooting

### Ruff is too strict

Adjust rules in `pyproject.toml`:

```toml
[tool.ruff.lint]
ignore = [
    "PLR0913",  # Too many arguments
]
```

### mypy complains about missing imports

Add to `pyproject.toml`:

```toml
[[tool.mypy.overrides]]
module = ["your_module.*"]
ignore_missing_imports = true
```

### Pre-commit hook is too slow

Skip specific hooks:

```bash
SKIP=mypy git commit -m "message"
```

## Best Practices

1. **Run checks before committing** (pre-commit does this automatically)
2. **Fix linting issues with `--fix`** before manual fixes
3. **Add type hints gradually** (mypy is lenient by default)
4. **Keep configuration in `pyproject.toml`** (single source of truth)
5. **Update hooks regularly** (`pre-commit autoupdate`)

## Resources

- [Ruff Documentation](https://docs.astral.sh/ruff/)
- [mypy Documentation](https://mypy.readthedocs.io/)
- [pre-commit Documentation](https://pre-commit.com/)
