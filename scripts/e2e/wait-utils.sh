#!/usr/bin/env bash

run_with_timeout_wait() {
  local seconds="$1"
  local description="$2"
  local cmd="$3"

  if command -v timeout >/dev/null 2>&1; then
    timeout "$seconds" bash -c "$cmd"
    return $?
  fi

  local start now
  start="$(date +%s)"
  while true; do
    if bash -c "$cmd"; then
      return 0
    fi
    now="$(date +%s)"
    if (( now - start >= seconds )); then
      echo "Timed out waiting for: $description" >&2
      return 124
    fi
    sleep 2
  done
}
