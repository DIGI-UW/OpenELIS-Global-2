#!/usr/bin/env bash
set -euo pipefail

echo "=== OpenELIS Workspace Setup ==="

# 1. Initialize ALL git submodules (plugins, dataexport, tools/*)
echo "Initializing git submodules..."
git submodule update --init --recursive
echo "  Submodules initialized."

# 2. Set up git hooks (pre-commit formatting)
# Prefer lefthook if available; fall back to native .githooks
if command -v lefthook &>/dev/null; then
  echo "Setting up pre-commit hooks via lefthook..."
  lefthook install

  # Ensure Git actually uses the lefthook shim in .git/hooks
  current_hooks_path=""
  if git config --get core.hooksPath >/dev/null 2>&1; then
    current_hooks_path="$(git config --get core.hooksPath)"
  fi
  if [ -n "${current_hooks_path}" ] && [ "${current_hooks_path}" != ".git/hooks" ]; then
    echo "  Detected existing core.hooksPath='${current_hooks_path}'. Unsetting so lefthook hooks run from .git/hooks."
    git config --unset core.hooksPath
  fi
elif [ -f .githooks/setup.sh ]; then
  echo "Setting up git hooks (native)..."
  bash .githooks/setup.sh
fi

# 3. Create .env from template if needed
if [ ! -f .env ] && [ -f .env.example ]; then
  cp .env.example .env
  echo "  Created .env from .env.example — review and customize as needed."
fi

# 4. Verify prerequisites
echo ""
echo "Checking prerequisites..."
if command -v java &>/dev/null; then
  java -version 2>&1 | head -1
else
  echo "  WARNING: Java not found (Java 21 required)"
fi

if command -v mvn &>/dev/null; then
  mvn --version 2>&1 | head -1
else
  echo "  WARNING: Maven not found"
fi

if command -v docker &>/dev/null; then
  docker --version 2>&1
else
  echo "  WARNING: Docker not found"
fi

if command -v node &>/dev/null; then
  echo "  Node $(node --version)"
else
  echo "  WARNING: Node.js not found"
fi

echo ""
echo "=== Setup complete ==="
echo ""
echo "Next steps:"
echo "  1. Review/edit .env if created"
echo "  2. mvn clean install -DskipTests -Dmaven.test.skip=true  (build backend)"
echo "  3. cd frontend && npm install  (install frontend deps)"
echo "  4. docker compose up -d  (start database)"
