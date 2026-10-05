#!/usr/bin/env bash
# Shared compose layering for analyzer harness local and CI/parity flows.
#
# Contract: DB service `db.openelis.org` uses container_name `openelisglobal-database`
# (see docker-compose.base.yml). Bridge import dir on host:
# `projects/analyzer-harness/volume/analyzer-imports`.

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
