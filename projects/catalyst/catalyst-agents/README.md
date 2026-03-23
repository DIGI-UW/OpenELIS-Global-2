# Catalyst Agents (M0.0)

This package hosts the Catalyst A2A agents used in the M0.0 Foundation POC.

- RouterAgent (A2A SDK server)
- CatalystAgent (A2A SDK server)

## Quick Start

```bash
# Install dependencies
uv sync --dev

# Setup development environment (includes pre-commit hooks)
make dev-setup

# Run all code quality checks
make check
```

## Development

### Code Quality

This project uses modern Python tooling:

- **Ruff**: Ultra-fast linting and formatting (replaces Black, Flake8, isort)
- **mypy**: Static type checking
- **pre-commit**: Automated git hooks

See [CODE_QUALITY.md](CODE_QUALITY.md) for detailed documentation.

### Common Commands

```bash
make install           # Install dependencies
make test              # Run tests
make lint              # Check code quality
make lint-fix          # Auto-fix linting issues
make format            # Format code
make typecheck         # Run type checker
make check             # Run all checks (recommended before commits)
```

### Pre-commit Hooks

Pre-commit hooks automatically run linting, formatting, and type checking before
each commit:

```bash
# Install hooks (one-time setup)
make pre-commit-install

# Or manually:
uv run pre-commit install
```
