#!/usr/bin/env bash
set -euo pipefail

echo "=== OpenELIS Workspace Setup ==="

# 1. Initialize all git submodules (dataexport and tools/*)
echo "Initializing git submodules..."
git submodule update --init --recursive
echo "  Submodules initialized."

# 2. Set up git hooks (pre-commit formatting)
if [ -f .githooks/setup.sh ]; then
  echo "Setting up git hooks..."
  bash .githooks/setup.sh
fi

# 3. Create .env from template if needed
if [ ! -f .env ] && [ -f .env.example ]; then
  cp .env.example .env
  echo "  Created .env from .env.example — defaults support localhost development."
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
echo "  scripts/dev-stack up  (check prerequisites, set up local HTTPS, build and start)"
echo "  Open the URL printed by up; localhost needs no .env edits."
echo "  First use may request administrator authentication for local HTTPS trust."
echo ""
echo "Optional diagnostics: scripts/dev-stack doctor"
echo "Separate CI verification: scripts/run-ci-checks.sh (committed changes)"
