.PHONY: help install test lint format typecheck check clean pre-commit-install

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

install: ## Install dependencies
	uv sync --dev

test: ## Run tests
	uv run pytest tests/ -v

lint: ## Run linter (Ruff)
	uv run ruff check .

lint-fix: ## Run linter with auto-fix
	uv run ruff check --fix .

format: ## Format code (Ruff)
	uv run ruff format .

format-check: ## Check formatting without changing files
	uv run ruff format --check .

typecheck: ## Run type checker (mypy)
	uv run mypy src/

check: lint-fix format typecheck test ## Run all checks (lint, format, typecheck, test)

pre-commit-install: ## Install pre-commit hooks
	uv run pre-commit install

pre-commit-run: ## Run pre-commit on all files
	uv run pre-commit run --all-files

clean: ## Clean cache files
	rm -rf .pytest_cache
	rm -rf .mypy_cache
	rm -rf .ruff_cache
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete

dev-setup: install pre-commit-install ## Setup development environment
	@echo "✓ Development environment ready!"
	@echo "Run 'make check' to verify everything works"
