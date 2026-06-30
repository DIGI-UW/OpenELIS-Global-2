#!/usr/bin/env bash
# ec2-setup.sh — Bootstrap an EC2 instance for analyzer harness deployment.
#
# Run as root (via sudo) on a fresh Ubuntu 22/24 LTS instance.
# Called by: scripts/ec2-harness.sh provision (via SSH)
#
# What it does:
#   1. Install Docker Engine + Compose v2 plugin
#   2. Create 'deploy' user with docker group access
#   3. Create /opt/analyzer-harness deploy directory
#   4. Install python3 (needed for seed-analyzers.sh)
#
# Usage: sudo bash ec2-setup.sh [DEPLOY_PUBKEY]
#   DEPLOY_PUBKEY — optional SSH public key to authorize for deploy user

set -e

DEPLOY_USER="deploy"
DEPLOY_DIR="/opt/analyzer-harness"
DEPLOY_PUBKEY="${1:-}"

echo "========================================"
echo "  EC2 Setup — Analyzer Harness"
echo "========================================"

# --- Docker ---
echo "[1/4] Installing Docker..."
if command -v docker &>/dev/null; then
  echo "  Docker already installed: $(docker --version)"
else
  apt-get update -qq
  apt-get install -y -qq docker.io docker-compose-plugin python3 >/dev/null
  systemctl enable --now docker
  echo "  Installed: $(docker --version)"
  echo "  Compose:  $(docker compose version)"
fi

# --- Deploy user ---
echo "[2/4] Creating deploy user..."
if id "$DEPLOY_USER" &>/dev/null; then
  echo "  User '$DEPLOY_USER' already exists"
else
  useradd -m -s /bin/bash "$DEPLOY_USER"
  echo "  Created user '$DEPLOY_USER'"
fi
usermod -aG docker "$DEPLOY_USER"
echo "  Added to docker group"

# --- SSH key ---
echo "[3/4] Configuring SSH access..."
DEPLOY_SSH_DIR="/home/${DEPLOY_USER}/.ssh"
mkdir -p "$DEPLOY_SSH_DIR"
chmod 700 "$DEPLOY_SSH_DIR"
touch "$DEPLOY_SSH_DIR/authorized_keys"
chmod 600 "$DEPLOY_SSH_DIR/authorized_keys"
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" "$DEPLOY_SSH_DIR"

if [ -n "$DEPLOY_PUBKEY" ]; then
  if ! grep -qF "$DEPLOY_PUBKEY" "$DEPLOY_SSH_DIR/authorized_keys" 2>/dev/null; then
    echo "$DEPLOY_PUBKEY" >> "$DEPLOY_SSH_DIR/authorized_keys"
    echo "  Added deploy public key"
  else
    echo "  Deploy public key already authorized"
  fi
fi

# --- Deploy directory ---
echo "[4/4] Creating deploy directory..."
mkdir -p "$DEPLOY_DIR"
chown "${DEPLOY_USER}:${DEPLOY_USER}" "$DEPLOY_DIR"
echo "  $DEPLOY_DIR owned by $DEPLOY_USER"

echo ""
echo "========================================"
echo "  EC2 Setup Complete"
echo "========================================"
echo "  Docker:     $(docker --version 2>/dev/null || echo 'FAILED')"
echo "  Compose:    $(docker compose version 2>/dev/null || echo 'FAILED')"
echo "  User:       $DEPLOY_USER (docker group)"
echo "  Deploy dir: $DEPLOY_DIR"
echo "  SSH:        $(wc -l < "$DEPLOY_SSH_DIR/authorized_keys") key(s) authorized"
echo ""
