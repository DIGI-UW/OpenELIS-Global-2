#!/usr/bin/env bash
# =============================================================================
# OpenELIS Global 2 - Cloud Development Environment Setup
# =============================================================================
# Installs Java 21, Maven, Node.js 20, and verifies Docker availability for
# development in cloud environments (Cursor Cloud, GitHub Codespaces, SSH dev).
#
# Usage:
#   bash scripts/setup-cloud-env.sh
#
# Requires: sudo (for apt install). Run without sudo if tools already present.
# =============================================================================

set -euo pipefail

REQUIRED_JAVA=21
REQUIRED_NODE=20
REQUIRED_MAVEN=3.8

echo "=== OpenELIS Cloud Environment Setup ==="

# Detect OS
if [ -f /etc/os-release ]; then
  . /etc/os-release
  OS=$ID
  VERSION=${VERSION_ID:-}
else
  OS=unknown
fi

echo "Detected OS: $OS $VERSION"

# Load nvm if present (so node/npm are found when installed via nvm)
if [ -s "$HOME/.nvm/nvm.sh" ]; then
  . "$HOME/.nvm/nvm.sh"
elif [ -s /usr/local/share/nvm/nvm.sh ]; then
  . /usr/local/share/nvm/nvm.sh
fi

# -----------------------------------------------------------------------------
# Java 21 (MANDATORY per AGENTS.md)
# -----------------------------------------------------------------------------
install_java() {
  if command -v java &>/dev/null; then
    local ver
    ver=$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)
    if [ "${ver:-0}" -ge "$REQUIRED_JAVA" ]; then
      echo "  Java $ver already installed"
      java -version 2>&1 | head -1
      return 0
    fi
  fi

  echo "  Installing Java $REQUIRED_JAVA..."
  if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
    sudo apt-get update -qq
    sudo apt-get install -y openjdk-21-jdk || sudo apt-get install -y default-jdk
  elif [ "$OS" = "amzn" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
    sudo dnf install -y java-21-openjdk-devel || sudo yum install -y java-21-openjdk-devel
  else
    echo "  WARNING: Unsupported OS for Java install. Install Java $REQUIRED_JAVA manually."
    return 1
  fi
  java -version 2>&1 | head -1
}

# -----------------------------------------------------------------------------
# Maven 3.8+
# -----------------------------------------------------------------------------
install_maven() {
  if command -v mvn &>/dev/null; then
    local ver
    ver=$(mvn -version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+' | head -1)
    echo "  Maven $ver already installed"
    mvn -version 2>&1 | head -1
    return 0
  fi

  echo "  Installing Maven..."
  if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
    sudo apt-get update -qq
    sudo apt-get install -y maven
  elif [ "$OS" = "amzn" ] || [ "$OS" = "rhel" ] || [ "$OS" = "fedora" ]; then
    sudo dnf install -y maven || sudo yum install -y maven
  else
    echo "  WARNING: Unsupported OS for Maven. Install Maven $REQUIRED_MAVEN+ manually."
    return 1
  fi
  mvn -version 2>&1 | head -1
}

# -----------------------------------------------------------------------------
# Node.js 20 (per frontend/package.json and CI)
# -----------------------------------------------------------------------------
install_node() {
  if command -v node &>/dev/null; then
    local ver
    ver=$(node --version 2>&1 | grep -oE '[0-9]+' | head -1)
    if [ "${ver:-0}" -ge 20 ] 2>/dev/null; then
      echo "  Node $(node --version) already installed"
      node --version
      npm --version
      return 0
    fi
  fi

  echo "  Installing Node.js $REQUIRED_NODE..."

  # Prefer nvm if present (common in cloud environments)
  if [ -s "$HOME/.nvm/nvm.sh" ]; then
    . "$HOME/.nvm/nvm.sh"
    nvm install $REQUIRED_NODE
    nvm use $REQUIRED_NODE
    nvm alias default $REQUIRED_NODE
  elif [ -s /usr/local/share/nvm/nvm.sh ]; then
    . /usr/local/share/nvm/nvm.sh
    nvm install $REQUIRED_NODE
    nvm use $REQUIRED_NODE
  else
    # Fallback: NodeSource (Ubuntu/Debian)
    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
      curl -fsSL https://deb.nodesource.com/setup_${REQUIRED_NODE}.x | sudo -E bash -
      sudo apt-get install -y nodejs
    else
      echo "  WARNING: Node $REQUIRED_NODE not found. Install manually (nvm or NodeSource)."
      return 0
    fi
  fi
  node --version
  npm --version
}

# -----------------------------------------------------------------------------
# Verify Docker (optional for local dev, required for compose)
# -----------------------------------------------------------------------------
check_docker() {
  if command -v docker &>/dev/null; then
    echo "  Docker $(docker --version) available"
    if docker compose version &>/dev/null; then
      docker compose version 2>&1 | head -1
    fi
  else
    echo "  WARNING: Docker not installed. Required for docker compose workflows."
    echo "  Install: https://docs.docker.com/engine/install/"
  fi
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
install_java
install_maven
install_node || echo "  (Node install skipped or failed - may need manual install)"
check_docker

echo ""
echo "=== Setup complete ==="
echo ""
echo "Next steps:"
echo "  1. bash scripts/setup-workspace.sh  (init submodules, hooks, .env)"
echo "  2. cd dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true && cd .."
echo "  3. mvn clean install -DskipTests -Dmaven.test.skip=true  (build backend)"
echo "  4. cd frontend && npm install && cd ..  (frontend deps)"
echo "  5. docker compose -f dev.docker-compose.yml up -d  (start dev stack)"
echo ""
