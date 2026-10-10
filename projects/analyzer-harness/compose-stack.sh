#!/usr/bin/env bash
# Internal CI compose layers for the analyzer Playwright executor.
#
# Internal executors add an isolated identity/port overlay and discover their
# own database container. Interactive setup uses scripts/dev-stack.

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"

HARNESS_BASE_COMPOSE="$HARNESS_DIR/docker-compose.base.yml"
CI_BUILD_COMPOSE="$REPO_ROOT/build.docker-compose.yml"
CI_HARNESS_COMPOSE="$REPO_ROOT/.github/ci/ci.analyzer-harness.yml"

compose_args_ci() {
  local -a args=(
    -f "$CI_BUILD_COMPOSE"
    -f "$HARNESS_BASE_COMPOSE"
    -f "$CI_HARNESS_COMPOSE"
    -f "$REPO_ROOT/.github/ci/ci.memory-limits.yml"
    -f "$REPO_ROOT/.github/ci/ci.memory-limits-harness.yml"
  )

  printf '%s\n' "${args[@]}"
}
