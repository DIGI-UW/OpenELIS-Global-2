#!/usr/bin/env bash
#
# Shim for /download-ci-logs: when user passes free text (no --pr, --branch,
# --run-id), defaults to current branch and failed-only. Otherwise passes
# arguments through to scripts/download-ci-logs.sh.
#
# Usage:
#   .specify/scripts/bash/download-ci-logs-shim.sh [--pr N | --branch NAME | --run-id ID] [--failed] [--list] ...
#   .specify/scripts/bash/download-ci-logs-shim.sh   # → current branch, failed-only

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
DOWNLOADER="$REPO_ROOT/scripts/download-ci-logs.sh"

has_explicit_target() {
    local arg
    for arg in "$@"; do
        if [[ "$arg" == "--pr" || "$arg" == "--branch" || "$arg" == "--run-id" ]]; then
            return 0
        fi
    done
    return 1
}

ARGS=("$@")
if ! has_explicit_target "${ARGS[@]}"; then
    BRANCH="$(git -C "$REPO_ROOT" branch --show-current 2>/dev/null || echo "")"
    if [[ -z "$BRANCH" ]]; then
        echo "Could not determine current branch. Use --branch <name> or --pr <number>." >&2
        exit 1
    fi
    exec "$DOWNLOADER" --branch "$BRANCH" --failed "${ARGS[@]}"
else
    exec "$DOWNLOADER" "${ARGS[@]}"
fi
